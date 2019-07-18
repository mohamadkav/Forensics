package edu.nu.forensic.reader;

import com.bbn.tc.schema.avro.cdm19.TCCDMDatum;
import com.bbn.tc.schema.serialization.AvroGenericDeserializer;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.Principal;
import edu.nu.forensic.db.entity.Subject;
import org.apache.avro.generic.GenericContainer;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class AvroStandAloneReader {
    private static Connection c;
    private static Statement stmt = null;

    public static void main(String[] args) throws Exception{
        initDB("jdbc:postgresql://localhost:5432/forensic", "postgres", "postgres");
        int fileCounter = 0;
        int fileNum = 10;
        while(fileCounter < fileNum){
            System.err.println(fileCounter);
            //String raw=input.nextLine();
            String raw = "D:\\E4_data\\" + "ta1-marple-e4-A.bin";
            if(fileCounter !=0)
                raw+="."+ fileCounter;
            fileCounter += 1;
            List<Event> eventList=new ArrayList<>();
            List<Subject> subjectList=new ArrayList<>();

            AvroGenericDeserializer avroGenericDeserializer=new AvroGenericDeserializer("schema/TCCDMDatum.avsc","schema/TCCDMDatum.avsc",
                    true,new File(raw));
            while(true) {
                GenericContainer data = (GenericContainer) avroGenericDeserializer.deserializeNextRecordFromFile();
                if (data == null) {
                    avroGenericDeserializer.close();
                    storeEvent(eventList);
                    storeSubject(subjectList);
                    break;
                }
                TCCDMDatum CDMdatum = (TCCDMDatum) data;
                if (CDMdatum.getDatum() instanceof com.bbn.tc.schema.avro.cdm19.Subject){
                    com.bbn.tc.schema.avro.cdm19.Subject bbnSubject=(com.bbn.tc.schema.avro.cdm19.Subject) CDMdatum.getDatum();
                    Subject subject=new Subject(UUID.nameUUIDFromBytes(bbnSubject.getUuid().bytes()),bbnSubject.getType().name(),
                            bbnSubject.getCid(),bbnSubject.getParentSubject()==null?null:UUID.nameUUIDFromBytes(bbnSubject.getParentSubject().bytes()),null,bbnSubject.getStartTimestampNanos(),
                            bbnSubject.getCmdLine()==null?null:bbnSubject.getCmdLine().toString(),bbnSubject.getPrivilegeLevel()==null?null:bbnSubject.getPrivilegeLevel().name());
                    subjectList.add(subject);
                    if(subjectList.size()>=1000) {
                        storeSubject(subjectList);
                        subjectList=new ArrayList<>();
                    }
                }
                if(CDMdatum.getDatum() instanceof com.bbn.tc.schema.avro.cdm19.Event){
                    com.bbn.tc.schema.avro.cdm19.Event bbnEvent=(com.bbn.tc.schema.avro.cdm19.Event) CDMdatum.getDatum();

                    StringBuilder eventNames= new StringBuilder();
                    if(bbnEvent.getNames()!=null) {
                        for (CharSequence cs : bbnEvent.getNames())
                            eventNames.append(cs.toString()).append(",");
                    }
                    if(!eventNames.toString().isEmpty())
                        eventNames.deleteCharAt(eventNames.length() - 1);
                    else
                        eventNames=null;
                    if(bbnEvent.getType().toString().equals("EVENT_OTHER")){
                        if(eventNames==null||!(eventNames.toString().equals("FileIoRead")||eventNames.toString().equals("FileIoWrite")))
                            continue;
                        else if (bbnEvent.getPredicateObjectPath().toString().compareTo(("UNKNOWN_FILE")) == 0)
                            continue;
                    }
                    else if(!(bbnEvent.getType().toString().equals("EVENT_READ")||bbnEvent.getType().toString().equals("EVENT_WRITE")))
                        continue;

                    Event event=new Event(UUID.nameUUIDFromBytes(bbnEvent.getUuid().bytes()),bbnEvent.getType()==null?null:bbnEvent.getType().name(),
                            bbnEvent.getThreadId(),bbnEvent.getSubject()==null?null:UUID.nameUUIDFromBytes(bbnEvent.getSubject().bytes()),bbnEvent.getPredicateObjectPath()!=null?bbnEvent.getPredicateObjectPath().toString():null,
                            bbnEvent.getTimestampNanos(),eventNames!=null?eventNames.toString():null,false);
                    eventList.add(event);
                    if(eventList.size()>=10000) {
                        storeEvent(eventList);
                        eventList=new ArrayList<>();
                    }
                }
            }
        }
    }


    private static void initDB(String url, String user, String passwd){
        Connection connection = null;
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(url, user, passwd);
            stmt = connection.createStatement();
            connection.setAutoCommit(false);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully");
        c = connection;
    }

    private static void storeSubject(List<Subject> subjectList) {
        try {
            for (Subject subject : subjectList) {
                String sql=null;
                try{
                    String parentSubjectUUID = null;
                    if(subject.getParentSubject()!=null)
                        parentSubjectUUID = subject.getParentSubject().toString();
                    if(subject.getCmdLine()==null)
                        subject.setCmdLine("undefined");
                    if(parentSubjectUUID!=null)
                     sql= "INSERT INTO \"subject\" (UUID,CID,cmd_line,parent_subjectuuid,start_timestamp_nanos,type) VALUES ('" +
                            subject.getUuid() + "' , " +
                            subject.getCid() + " , '" +
                            subject.getCmdLine() + "' , '" +
                            parentSubjectUUID+ "' , " +
                            subject.getStartTimestampNanos() + "," +
                            "'"+subject.getType()+"');";
                    else
                        sql= "INSERT INTO \"subject\" (UUID,CID,cmd_line,start_timestamp_nanos,type) VALUES ('" +
                                subject.getUuid() + "' , " +
                                subject.getCid() + " , '" +
                                subject.getCmdLine() + "' ," +
                                subject.getStartTimestampNanos() + "," +
                                "'"+subject.getType()+"');";
                    stmt.execute(sql);
                }catch (Exception e){
                    System.out.printf(sql);
                    e.printStackTrace();
                }
            }
            c.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void storeEvent(List<Event> eventList) {
        try {
            for (Event event : eventList) {
                try{
                    String sql = "INSERT INTO \"event\" (id,thread_id,names,predicate_object_path,timestamp_nanos,type,subjectuuid) VALUES ('" +
                            event.getId() + "' , " +
                            event.getThreadId() + " , '" +
                            event.getNames() + "' , '" +
                            event.getPredicateObjectPath()+ "' , " +
                            event.getTimestampNanos() + "," +
                            "'"+event.getType()+"','"+event.getSubjectUUID()+"');";
                    stmt.execute(sql);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            c.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
