package edu.nu.forensic.reader;

import com.bbn.tc.schema.SchemaNotInitializedException;
import com.bbn.tc.schema.avro.cdm20.*;
import com.bbn.tc.schema.serialization.AvroGenericDeserializer;
import edu.nu.forensic.db.DBApi.Neo4jApi;
import edu.nu.forensic.db.DBApi.PostGreSqlApi;
import edu.nu.forensic.util.RecordConverter;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.UUID;


@Component
public class AvroReader {

    @Autowired
    private RecordConverter recordConverter;

    public void readTrace(File source) throws IOException, SchemaNotInitializedException {
        AvroGenericDeserializer avroGenericDeserializer=new AvroGenericDeserializer("schema/TCCDMDatum.avsc","schema/TCCDMDatum.avsc",
                true,source);
        final Scanner scanner = new Scanner(new FileReader("C:\\Data\\ta1-marple-e4-A.index"));
        int i=0;
        List<String> registryUUIDs = new LinkedList<>();
        List<edu.nu.forensic.db.entity.Subject> subjectList= new ArrayList<>();
        List<edu.nu.forensic.db.entity.Event> eventList= new ArrayList<>();
        Map<UUID,UUID> ThreadIDToProcessID = new HashMap<>();
        Map<UUID,UUID> UnitToDependency = new HashMap<>();

        //init db
//        Neo4jApi neo4jApi = new Neo4jApi("C:\\Data\\neo4j-community-3.5.3\\data\\databases\\graph.db");
        PostGreSqlApi postGreSqlApi = new PostGreSqlApi();

        while(scanner.hasNextInt()){
            final int length  = scanner.nextInt();
//            GenericContainer data= (GenericContainer)avroGenericDeserializer.deserializeNextRecordFromFile();
            GenericContainer data= (GenericContainer)avroGenericDeserializer.deserializeNextRecordFromFile(length);
            if(data==null)
                break;
            TCCDMDatum CDMdatum=(TCCDMDatum) data;
            try {
                if(i%10000==0) System.out.println(i);
                i++;
//                if (CDMdatum.getDatum() instanceof Subject)
//                    recordConverter.saveAndConvertBBNSubjectToSubject((Subject) CDMdatum.getDatum());
//                else if (CDMdatum.getDatum() instanceof Principal)
//                    recordConverter.saveAndConvertBBNPrincipalToPrincipal((Principal) CDMdatum.getDatum());
//                else if (CDMdatum.getDatum() instanceof FileObject)
//                    recordConverter.saveAndConvertBBNFileObjectToFileObject((FileObject) CDMdatum.getDatum());
//                else if (CDMdatum.getDatum() instanceof RegistryKeyObject)
//                    registryUUIDs.add(((RegistryKeyObject) CDMdatum.getDatum()).getUuid().toString());
//                else if (CDMdatum.getDatum() instanceof NetFlowObject)
//                    recordConverter.saveAndConvertBBNNetFlowObjectToNetFlowObject((NetFlowObject) CDMdatum.getDatum());
//                else if (CDMdatum.getDatum() instanceof Event) {
//                    if (((Event) CDMdatum.getDatum()).getType().toString().contains("READ") || ((Event) CDMdatum.getDatum()).getType().toString().contains("WRITE"))
//                    {
//                        Boolean k = true;
//                        for(String it:registryUUIDs) {
//                            if(CDMdatum.getDatum().toString().contains(it)){
//                                k = false;
//                                break;
//                            }
//                            if(k) recordConverter.saveAndConvertBBNEventToEvent((Event) CDMdatum.getDatum());
//                        }
//                    }
//
//                }
//                else if (CDMdatum.getDatum() instanceof UnitDependency)
//                    recordConverter.saveAndConvertBBNUnitDependencyToUnitDependency((UnitDependency) CDMdatum.getDatum());
//                else
//                    System.err.println(CDMdatum.toString());
                if(CDMdatum.getDatum() instanceof Subject) {
                    if (((Subject) CDMdatum.getDatum()).getType().toString().contains("THREAD")) {
                        UUID threadID = UUID.nameUUIDFromBytes(((Subject) CDMdatum.getDatum()).getUuid().bytes());
                        UUID processID =  UUID.nameUUIDFromBytes(((Subject) CDMdatum.getDatum()).getParentSubject().bytes());
                        if(processID!=null) ThreadIDToProcessID.put(threadID,processID);
                    } else {
//                        System.out.println(CDMdatum.getDatum().toString());

                        UUID processID = UUID.nameUUIDFromBytes(((Subject) CDMdatum.getDatum()).getUuid().bytes());
                        UUID parentID = ((Subject) CDMdatum.getDatum()).getParentSubject() != null ? UUID.nameUUIDFromBytes(((Subject) CDMdatum.getDatum()).getParentSubject().bytes()) : null;

                        //thread
                        while(ThreadIDToProcessID.containsKey(processID)) processID = ThreadIDToProcessID.get(processID);
                        while(parentID!=null&&ThreadIDToProcessID.containsKey(parentID)) parentID = ThreadIDToProcessID.get(parentID);

                        //unitdependent
                        while(UnitToDependency.containsKey(processID)) {
                            processID = UnitToDependency.get(processID);
                        }
                        while((parentID!=null)&&UnitToDependency.containsKey(parentID)) {
                            parentID = UnitToDependency.get(parentID);
                        }

                        edu.nu.forensic.db.entity.Subject subject = new edu.nu.forensic.db.entity.Subject(
                                processID
                                , ((Subject) CDMdatum.getDatum()).getCid()
                                , parentID
                                , ((Subject) CDMdatum.getDatum()).getStartTimestampNanos()
                                ,((Subject) CDMdatum.getDatum()).getCmdLine() != null ? ((Subject) CDMdatum.getDatum()).getCmdLine().toString().replaceAll("\"","").replaceAll("'","") : null);
                        subjectList.add(subject);
                        if (subjectList.size() > 100) {
                            postGreSqlApi.storeSubject(subjectList);
                            subjectList.clear();
                            System.out.println(11111);
                        }
                    }
                }
                else if(CDMdatum.getDatum() instanceof Event){
                    if(((Event) CDMdatum.getDatum()).toString().contains("FileIoWrite")||((Event) CDMdatum.getDatum()).toString().contains("FileIoRead")) {
                        if(!((Event) CDMdatum.getDatum()).toString().contains("UNKNOWN")) {
                            UUID processID = UUID.nameUUIDFromBytes(((Event) CDMdatum.getDatum()).getSubject().bytes());

                            //thread
                            while(ThreadIDToProcessID.containsKey(processID)) processID = ThreadIDToProcessID.get(processID);
                            //dependent
                            while(UnitToDependency.containsKey(processID)) {
                                processID = UnitToDependency.get(processID);
                            }

                            edu.nu.forensic.db.entity.Event event = new edu.nu.forensic.db.entity.Event(
                                    UUID.nameUUIDFromBytes(((Event) CDMdatum.getDatum()).getUuid().bytes())
                                    , ((Event) CDMdatum.getDatum()).getNames().toString()
                                    , ((Event) CDMdatum.getDatum()).getThreadId()
                                    , processID
                                    , ((Event) CDMdatum.getDatum()).getTimestampNanos()
                                    , ((Event) CDMdatum.getDatum()).getPredicateObjectPath() != null ? ((Event) CDMdatum.getDatum()).getPredicateObjectPath().toString() : ((Event) CDMdatum.getDatum()).getPredicateObject2Path().toString());
                            eventList.add(event);
                        }
                    }
                    if(eventList.size()>3000){
                        postGreSqlApi.storeEvent(eventList);
                        eventList.clear();
                        System.out.println(22222);
                    }
                }
                else if(CDMdatum.getDatum() instanceof UnitDependency){
                    UUID unit = UUID.nameUUIDFromBytes(((UnitDependency)CDMdatum.getDatum()).getUnit().bytes());
                    UUID dependentUnit = UUID.nameUUIDFromBytes(((UnitDependency)CDMdatum.getDatum()).getDependentUnit().bytes());
                    UnitToDependency.put(dependentUnit,unit);
                }
            }catch (Exception e){
                System.err.println("Darn! We have an unknown bug over: ");
//                System.err.println(CDMdatum);
                e.printStackTrace();
            }
        }
        postGreSqlApi.storeSubject(subjectList);
        postGreSqlApi.storeEvent(eventList);
        Date day=new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(df.format(day));
        avroGenericDeserializer.close();
    }

    //CDM20
    public void readTraceWithoutIndex(File source) throws IOException, SchemaNotInitializedException {
        AvroGenericDeserializer avroGenericDeserializer=new AvroGenericDeserializer("schema/TCCDMDatum.avsc","schema/TCCDMDatum.avsc",
                true,source);
        int i=0;
        List<String> registryUUIDs = new LinkedList<>();
        List<edu.nu.forensic.db.entity.Subject> subjectList= new ArrayList<>();
        List<edu.nu.forensic.db.entity.Event> eventList= new ArrayList<>();
        Map<UUID,UUID> ThreadIDToProcessID = new HashMap<>();
        Map<UUID,UUID> UnitToDependency = new HashMap<>();

        //init db
//        Neo4jApi neo4jApi = new Neo4jApi("C:\\Data\\neo4j-community-3.5.3\\data\\databases\\graph.db");
        PostGreSqlApi postGreSqlApi = new PostGreSqlApi();
        DatumReader<TCCDMDatum> reader = new SpecificDatumReader<>(TCCDMDatum.class);

        DataFileReader<TCCDMDatum> dataFileReader = new DataFileReader<>(source, reader);
        TCCDMDatum CDMdatum = null;
        while (dataFileReader.hasNext()) {
            CDMdatum = dataFileReader.next();

            if(CDMdatum==null)
                break;
            try {
                if(i%10000==0) System.out.println(i);
                i++;
//                if (CDMdatum.getDatum() instanceof Subject)
//                    recordConverter.saveAndConvertBBNSubjectToSubject((Subject) CDMdatum.getDatum());
//                else if (CDMdatum.getDatum() instanceof Principal)
//                    recordConverter.saveAndConvertBBNPrincipalToPrincipal((Principal) CDMdatum.getDatum());
//                else if (CDMdatum.getDatum() instanceof FileObject)
//                    recordConverter.saveAndConvertBBNFileObjectToFileObject((FileObject) CDMdatum.getDatum());
//                else if (CDMdatum.getDatum() instanceof RegistryKeyObject)
//                    registryUUIDs.add(((RegistryKeyObject) CDMdatum.getDatum()).getUuid().toString());
//                else if (CDMdatum.getDatum() instanceof NetFlowObject)
//                    recordConverter.saveAndConvertBBNNetFlowObjectToNetFlowObject((NetFlowObject) CDMdatum.getDatum());
//                else if (CDMdatum.getDatum() instanceof Event) {
//                    if (((Event) CDMdatum.getDatum()).getType().toString().contains("READ") || ((Event) CDMdatum.getDatum()).getType().toString().contains("WRITE"))
//                    {
//                        Boolean k = true;
//                        for(String it:registryUUIDs) {
//                            if(CDMdatum.getDatum().toString().contains(it)){
//                                k = false;
//                                break;
//                            }
//                            if(k) recordConverter.saveAndConvertBBNEventToEvent((Event) CDMdatum.getDatum());
//                        }
//                    }
//
//                }
//                else if (CDMdatum.getDatum() instanceof UnitDependency)
//                    recordConverter.saveAndConvertBBNUnitDependencyToUnitDependency((UnitDependency) CDMdatum.getDatum());
//                else
//                    System.err.println(CDMdatum.toString());
                if(CDMdatum.getDatum() instanceof Subject) {
                    if (((Subject) CDMdatum.getDatum()).getType().toString().contains("THREAD")) {
                        UUID threadID = UUID.nameUUIDFromBytes(((Subject) CDMdatum.getDatum()).getUuid().bytes());
                        UUID processID =  UUID.nameUUIDFromBytes(((Subject) CDMdatum.getDatum()).getParentSubject().bytes());
                        if(processID!=null) ThreadIDToProcessID.put(threadID,processID);
                    } else {
//                        System.out.println(CDMdatum.getDatum().toString());

                        UUID processID = UUID.nameUUIDFromBytes(((Subject) CDMdatum.getDatum()).getUuid().bytes());
                        UUID parentID = ((Subject) CDMdatum.getDatum()).getParentSubject() != null ?
                                UUID.nameUUIDFromBytes(((Subject) CDMdatum.getDatum()).getParentSubject().bytes()) : null;

                        //thread
                        while(ThreadIDToProcessID.containsKey(processID)) processID = ThreadIDToProcessID.get(processID);
                        while(parentID!=null&&ThreadIDToProcessID.containsKey(parentID)) parentID = ThreadIDToProcessID.get(parentID);

                        //unitdependent
                        while(UnitToDependency.containsKey(processID)) {
                            processID = UnitToDependency.get(processID);
                        }
                        while((parentID!=null)&&UnitToDependency.containsKey(parentID)) {
                            parentID = UnitToDependency.get(parentID);
                        }

                        edu.nu.forensic.db.entity.Subject subject = new edu.nu.forensic.db.entity.Subject(
                                processID
                                , ((Subject) CDMdatum.getDatum()).getCid()
                                , parentID
                                , ((Subject) CDMdatum.getDatum()).getStartTimestampNanos()
                                ,((Subject) CDMdatum.getDatum()).getCmdLine() != null ?
                                 ((Subject) CDMdatum.getDatum()).getCmdLine().toString().replaceAll("\"","").
                                         replaceAll("'","") : null);
                        subjectList.add(subject);
                        if (subjectList.size() > 100) {
                            postGreSqlApi.storeSubject(subjectList);
                            subjectList.clear();
                            System.out.println(11111);
                        }
                    }
                }
                else if(CDMdatum.getDatum() instanceof Event) {
                    if (((Event) CDMdatum.getDatum()).getType().toString().contains("EVENT_WRITE")
                            || ((Event) CDMdatum.getDatum()).getType().toString().contains("EVENT_READ")) {
                        if (!((Event) CDMdatum.getDatum()).toString().contains("UNKNOWN")) {
                            UUID processID = UUID.nameUUIDFromBytes(((Event) CDMdatum.getDatum()).getSubject().bytes());

                            //thread
                            while (ThreadIDToProcessID.containsKey(processID))
                                processID = ThreadIDToProcessID.get(processID);
                                //dependent
                            while (UnitToDependency.containsKey(processID)) {
                                processID = UnitToDependency.get(processID);
                            }

                            edu.nu.forensic.db.entity.Event event = new edu.nu.forensic.db.entity.Event(
                                    UUID.nameUUIDFromBytes(((Event) CDMdatum.getDatum()).getUuid().bytes())
                                    , ((Event) CDMdatum.getDatum()).getType().toString()
                                    , ((Event) CDMdatum.getDatum()).getThreadId()
                                    , processID
                                    , ((Event) CDMdatum.getDatum()).getTimestampNanos()
                                    , ((Event) CDMdatum.getDatum()).getPredicateObjectPath() != null
                                      ? ((Event) CDMdatum.getDatum()).getPredicateObjectPath().toString() :
                                      ((Event) CDMdatum.getDatum()).getPredicateObject2Path().toString());
                            eventList.add(event);
                        }
                        if (eventList.size() > 3000) {
                            postGreSqlApi.storeEvent(eventList);
                            eventList.clear();
                            System.out.println(22222);
                        }
                    } else if (CDMdatum.getDatum() instanceof UnitDependency) {
                        UUID unit = UUID.nameUUIDFromBytes(((UnitDependency) CDMdatum.getDatum()).getUnit().bytes());
                        UUID dependentUnit = UUID.nameUUIDFromBytes(((UnitDependency) CDMdatum.getDatum()).getDependentUnit().bytes());
                        UnitToDependency.put(dependentUnit, unit);
                    }
                }
            }catch (Exception e){
                System.err.println("Darn! We have an unknown bug over: ");
//                System.err.println(CDMdatum);
                e.printStackTrace();
            }
        }
        postGreSqlApi.storeSubject(subjectList);
        postGreSqlApi.storeEvent(eventList);
        postGreSqlApi.closeConnection();
        Date day=new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(df.format(day));
        avroGenericDeserializer.close();
    }
}