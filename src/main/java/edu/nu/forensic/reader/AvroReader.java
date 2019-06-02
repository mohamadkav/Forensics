package edu.nu.forensic.reader;

import com.bbn.tc.schema.SchemaNotInitializedException;
import com.bbn.tc.schema.avro.cdm19.*;
import com.bbn.tc.schema.serialization.AvroGenericDeserializer;
import edu.nu.forensic.util.RecordConverter;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;


@Component
public class AvroReader {

    @Autowired
    private RecordConverter recordConverter;

    private int eventCounter;


    public void readTrace(File source) throws IOException, SchemaNotInitializedException {
        AvroGenericDeserializer avroGenericDeserializer=new AvroGenericDeserializer("schema/TCCDMDatum.avsc","schema/TCCDMDatum.avsc",
                true,source);
        eventCounter = 0;
        while(true){
            GenericContainer data= (GenericContainer)avroGenericDeserializer.deserializeNextRecordFromFile();
            if(data==null)
                break;
            TCCDMDatum CDMdatum=(TCCDMDatum) data;
            try {
                if (CDMdatum.getDatum() instanceof Subject)
                    recordConverter.saveAndConvertBBNSubjectToSubject((Subject) CDMdatum.getDatum());
            //    else if (CDMdatum.getDatum() instanceof Principal)
            //        recordConverter.saveAndConvertBBNPrincipalToPrincipal((Principal) CDMdatum.getDatum());
            //    else if (CDMdatum.getDatum() instanceof FileObject)
            //        recordConverter.saveAndConvertBBNFileObjectToFileObject((FileObject) CDMdatum.getDatum());
            //    else if (CDMdatum.getDatum() instanceof RegistryKeyObject)
            //        recordConverter.saveAndConvertBBNRegistryKeyObjectToRegistryKeyObject((RegistryKeyObject) CDMdatum.getDatum());
            //    else if (CDMdatum.getDatum() instanceof NetFlowObject)
            //        recordConverter.saveAndConvertBBNNetFlowObjectToNetFlowObject((NetFlowObject) CDMdatum.getDatum());
                else if (CDMdatum.getDatum() instanceof Event) {
                    Event e=(Event) CDMdatum.getDatum();
                    eventCounter += 1;
                    if (eventCounter % 1000000 == 0)
                        System.out.println(eventCounter);
                    if(e.getNames()!=null && (e.getNames().get(0).toString().contains("FileIoRead") || e.getNames().get(0).toString().contains("FileIoWrite")))
                        if(!e.getPredicateObjectPath().toString().equals("UNKNOWN_FILE")) {
                            recordConverter.saveAndConvertBBNEventToEvent(e);
                        }
                }
            //    else if (CDMdatum.getDatum() instanceof UnitDependency)
            //        recordConverter.saveAndConvertBBNUnitDependencyToUnitDependency((UnitDependency) CDMdatum.getDatum());
            //    else
            //        System.err.println(CDMdatum.toString());
            }catch (Exception e){
                System.err.println("Darn! We have an unknown bug over: ");
                System.err.println(CDMdatum);
                e.printStackTrace();
            }
        }
        System.out.println(eventCounter);
        avroGenericDeserializer.close();
    }
}