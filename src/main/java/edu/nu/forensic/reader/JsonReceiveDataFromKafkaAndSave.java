package edu.nu.forensic.reader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.Subject;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.*;

public class JsonReceiveDataFromKafkaAndSave {
    private static final int NUM_SERVERS=1;

    public static void main(String[] args) {
        List<Thread> threadList=new ArrayList<>(NUM_SERVERS);
        for(int i=0;i<NUM_SERVERS;i++){
            Thread t=new Thread(new JsonReceiverThread(i));
            threadList.add(t);
            threadList.get(i).start();
        }

    }

}

class JsonReceiverThread extends Thread implements Runnable{
    private Connection c;
    private Statement stmt = null;
    private List<Event> eventList=new ArrayList<>();
    private List<Subject> subjectList=new ArrayList<>();
    private Map <Integer, UUID> tidToUUID=new HashMap<>();
    private Map <Integer, UUID> pidToUUID=new HashMap<>();
    private Consumer<String, String> consumer;
    private long consumed=0;
    private int threadId;
    public JsonReceiverThread(int threadId){
        this.threadId=threadId;
        initDB("jdbc:postgresql://localhost:5432/forensic", "postgres", "1234");
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "MARPLE");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
     //   props.put(AvroConfig.SCHEMA_READER_FILE, "schema/TCCDMDatum.avsc");
     //   props.put(AvroConfig.SCHEMA_WRITER_FILE, "schema/TCCDMDatum.avsc");
     //   props.put(AvroConfig.SCHEMA_SERDE_IS_SPECIFIC, false);
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("MARPLE"+threadId));
        System.err.println("Consumer subscribed to topic MARPLE"+threadId);
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
    public void run(){
        while (true) {
            ConsumerRecords<String, String> consumerRecords = consumer.poll(5000);
            // 1000 is the time in milliseconds consumer will wait if no record is found at broker.
            consumerRecords.forEach(record -> {
                consumed++;
                if(consumed%10000==0)
                    System.err.println("Thread "+threadId+" consumed "+consumed);
                String json=record.value();
                try {
                    JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
                    String eventName = jsonObject.get("EventName").getAsString();
                    switch (eventName) {
                        case "ThreadStart":
                        case "ThreadDCStart": {
                            int tid = jsonObject.get("arguments").getAsJsonObject().get("TThreadId").getAsInt();
                            int parentPid = jsonObject.get("processID").getAsInt();
                            long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                            UUID uuid = UUID.randomUUID();
//                        Subject parent = null;
//                        if (pidToUUID.containsKey(parentPid))
//                            parent = subjectRepository.findById(pidToUUID.get(parentPid)).get();
                            tidToUUID.put(tid, uuid);
                            Subject subject = new edu.nu.forensic.db.entity.Subject(uuid, "SUBJECT_THREAD",
                                    tid, !pidToUUID.containsKey(parentPid)?null:pidToUUID.get(parentPid), null, timeStamp,
                                    null, null);
                            subjectList.add(subject);
                            if(subjectList.size()>1000) {
                                storeSubject(subjectList);
                                subjectList=new ArrayList<>();
                            }
                            break;
                        }
                        case "ProcessStart":
                        case "ProcessDCStart": {
                            int pid = jsonObject.get("arguments").getAsJsonObject().get("ProcessId").getAsInt();
                            int parentPid = jsonObject.get("processID").getAsInt();
                            long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                            UUID uuid = UUID.randomUUID();
//                        Subject parent = null;
//                        if (pidToUUID.containsKey(parentPid))
//                            parent = subjectRepository.findById(pidToUUID.get(parentPid)).get();
                            pidToUUID.put(pid, uuid);
                            Subject subject = new edu.nu.forensic.db.entity.Subject(uuid, "SUBJECT_PROCESS",
                                    pid, !pidToUUID.containsKey(parentPid)?null:pidToUUID.get(parentPid), null, timeStamp,
                                    jsonObject.get("arguments").getAsJsonObject().get("CommandLine").getAsString(), null);
                            subjectList.add(subject);
                            if(subjectList.size()>1000) {
                                System.out.println("Saving Subjects...");
                                System.err.println("Saving... "+threadId);
                                storeSubject(subjectList);;
                                subjectList=new ArrayList<>();
                            }
                            break;
                        }
                        case "FileIoRead":
                        case "FileIoWrite": {
                            int tid = jsonObject.get("threadID").getAsInt();
                            long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                            UUID uuid = UUID.randomUUID();
                            Event event = new Event(uuid, eventName.equals("FileIoRead") ? "EVENT_READ" : "EVENT_WRITE", tid, tidToUUID.get(tid),
                                    jsonObject.get("arguments").getAsJsonObject().get("FileName").getAsString(), timeStamp, eventName.equals("FileIoRead") ? "FileIoRead" : "FileIoWrite");
                            eventList.add(event);
                            if(eventList.size()>1000) {
                                storeEvent(eventList);
                                System.err.println("Saving... "+threadId);
                                eventList=new ArrayList<>();
                            }
                            break;
                        }
                    }
                }catch (Exception e){
                    System.err.println(json);
                    e.printStackTrace();
                }
            });
            consumer.commitAsync();

        }
    }
}
