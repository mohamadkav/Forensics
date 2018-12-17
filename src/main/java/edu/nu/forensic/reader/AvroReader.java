package edu.nu.forensic.reader;

import com.bbn.tc.schema.SchemaNotInitializedException;
import com.bbn.tc.schema.avro.cdm19.*;
import com.bbn.tc.schema.serialization.AvroGenericDeserializer;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;


@Component
public class AvroReader {

    public void readTrace(File source) throws IOException, SchemaNotInitializedException {
        AvroGenericDeserializer avroGenericDeserializer=new AvroGenericDeserializer("schema/TCCDMDatum.avsc","schema/TCCDMDatum.avsc",
                false,source);
        while(true){
            IndexedRecord data= (IndexedRecord)avroGenericDeserializer.deserializeNextRecordFromFile();
            if(data==null)
                break;
            GenericData.Fixed uuid=(GenericData.Fixed)data.get(3);
            TCCDMDatum datum=new TCCDMDatum(data.get(0),(CharSequence)data.get(1), RecordType.valueOf(data.get(2).toString()),new UUID(uuid.bytes()),(Integer)data.get(4),InstrumentationSource.valueOf(data.get(5).toString()));
            if(datum.getDatum() instanceof Subject)
                System.out.println(((Subject) datum.getDatum()).getType());
            else
                System.out.println(datum.getDatum().getClass());
        }
        avroGenericDeserializer.close();
    }
}
