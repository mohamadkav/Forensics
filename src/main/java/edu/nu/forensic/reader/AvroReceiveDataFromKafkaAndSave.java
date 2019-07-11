package edu.nu.forensic.reader;

import com.bbn.tc.schema.avro.cdm19.TCCDMDatum;
import com.bbn.tc.schema.serialization.AvroConfig;
import com.bbn.tc.schema.serialization.AvroGenericDeserializer;
import com.bbn.tc.schema.serialization.kafka.KafkaAvroGenericDeserializer;
import com.bbn.tc.schema.serialization.kafka.KafkaAvroGenericSerializer;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.Subject;
import org.apache.avro.Schema;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.*;

public class AvroReceiveDataFromKafkaAndSave {
    private static final int NUM_SERVERS=1;

    public static void main(String[] args) {
        List<ReceiverThread> threadList=new ArrayList<>(NUM_SERVERS);
        for(int i=0;i<NUM_SERVERS;i++){
            threadList.add(new ReceiverThread(i));
            threadList.get(i).start();
        }

    }

}
class ReceiverThread extends Thread implements Runnable{
    private Connection c;
    private Statement stmt = null;
    private List<Event> eventList=new ArrayList<>();
    private List<Subject> subjectList=new ArrayList<>();

    private Consumer<String, TCCDMDatum> consumer;
    public ReceiverThread(int threadId){
        initDB("jdbc:postgresql://localhost:5432/forensic", "postgres", "1234");
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "MARPLE");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,KafkaAvroGenericDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(AvroConfig.SCHEMA_READER_FILE, "schema/TCCDMDatum.avsc");
        props.put(AvroConfig.SCHEMA_WRITER_FILE, "schema/TCCDMDatum.avsc");
        props.put(AvroConfig.SCHEMA_SERDE_IS_SPECIFIC, false);

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("MARPLE"+threadId));
    }

    private void initDB(String url, String user, String passwd){
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

    private void storeSubject(List<Subject> subjectList) {
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
    private void storeEvent(List<Event> eventList) {
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
    public void start(){
        while (true) {
            ConsumerRecords<String, TCCDMDatum> consumerRecords = consumer.poll(5000);
            // 1000 is the time in milliseconds consumer will wait if no record is found at broker.
            consumerRecords.forEach(record -> {
                TCCDMDatum CDMdatum = record.value();
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
                            return;
                        else if (bbnEvent.getPredicateObjectPath().toString().compareTo(("UNKNOWN_FILE")) == 0)
                            return;
                    }
                    else if(!(bbnEvent.getType().toString().equals("EVENT_READ")||bbnEvent.getType().toString().equals("EVENT_WRITE")))
                        return;

                    Event event=new Event(UUID.nameUUIDFromBytes(bbnEvent.getUuid().bytes()),bbnEvent.getType()==null?null:bbnEvent.getType().name(),
                            bbnEvent.getThreadId(),bbnEvent.getSubject()==null?null:UUID.nameUUIDFromBytes(bbnEvent.getSubject().bytes()),bbnEvent.getPredicateObjectPath()!=null?bbnEvent.getPredicateObjectPath().toString():null,
                            bbnEvent.getTimestampNanos(),eventNames!=null?eventNames.toString():null);
                    eventList.add(event);
                    if(eventList.size()>=10000) {
                        storeEvent(eventList);
                        eventList=new ArrayList<>();
                    }
                }
            });
            consumer.commitAsync();

        }
    }
}
