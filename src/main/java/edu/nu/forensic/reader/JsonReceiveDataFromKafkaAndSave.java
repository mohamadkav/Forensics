package edu.nu.forensic.reader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.nu.forensic.db.cassandra.connectionToCassandra;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.EventNameToNum;
import edu.nu.forensic.db.entity.NetFlowObject;
import edu.nu.forensic.db.entity.Subject;
import edu.nu.forensic.reducer.CPRStandAloneReducer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.*;

public class JsonReceiveDataFromKafkaAndSave {
    private static final int NUM_SERVERS=1;

    public static void main(String[] args) {
        List<Thread> threadList=new ArrayList<>(NUM_SERVERS);   // one threadlist
        for(int i=0;i<NUM_SERVERS;i++){
            Thread t=new Thread(new JsonReceiverThread(i)); // one server generate one JsonReceiverThread
            threadList.add(t);  //  threadList cantains each one of the JsonReceiverThread
            threadList.get(i).start();  // start each JsonReceiverThread after added
        }

    }

}

class JsonReceiverThread extends Thread implements Runnable{

    private List<Event> eventList = new ArrayList<>();
    private List<Subject> subjectList = new ArrayList<>();
    private List<NetFlowObject> netList = new ArrayList<>();

    private Map <Integer, UUID> tidToUUID = new HashMap<>();
    private Map <Integer, UUID> pidToUUID = new HashMap<>();
    private Map <String, UUID> fileNameToUUID = new HashMap<>();
    private Map <String, Integer> eventNameToNum = EventNameToNum.FileIoDelete.getEventNameToNum();

    private Set<Integer> visibleWindowPid = new HashSet<>();
    private Set<String> eventNames = new HashSet<>();

    private connectionToCassandra connectionToCassandra;

    private Consumer<String, String> consumer;
    private long consumed=0;
    private int threadId;
    private CPRStandAloneReducer reducer=new CPRStandAloneReducer();

    public JsonReceiverThread(int threadId){    // creating JsonReceiverThread, each with an identifier as threadId (the same with its offset in threadList)

        this.threadId=threadId;
        String IPaddress = "127.0.0.1";
        connectionToCassandra=new connectionToCassandra(IPaddress, threadId+"");    // create new connection, providing threadId
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "MARPLE");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumer = new KafkaConsumer<>(props);  // use properties and define consumer
        consumer.subscribe(Collections.singletonList("MARPLE"+threadId));   // allow consumer to subscribe only to ["MARPLE"+threadId] topic
        System.err.println("Consumer subscribed to topic MARPLE"+threadId);
    }

    public void run(){
        while (true) {
            ConsumerRecords<String, String> consumerRecords = consumer.poll(5000);  // receive records from kafka cluster, otherwise wait
            // 1000 is the time in milliseconds consumer will wait if no record is found at broker.
            consumerRecords.forEach(record -> { // traverse consumer records
                consumed++; // use consumed to flag record number
                if(consumed%10000==0)   // each 10000 records reported
                    System.err.println("Thread "+threadId+" consumed "+consumed);
                String line = record.value(); // use String line to hold record
                try {
                    JsonObject jsonObject = new JsonParser().parse(line).getAsJsonObject(); // and parse line into JsonObject (each record in the form of a JsonObject)

                    if(line.contains("CallStack")){
                        String eventName = jsonObject.get("CallStack").getAsString();
                        int tid = jsonObject.get("threadID").getAsInt();
                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                        // use map uuid
                        while(eventName.contains(",")){ // parse eventname, which may be composed of several strings and seperated by ','
                            int k = eventName.indexOf(",");
                            String tempEventName = eventName.substring(0, k);
                            Event event = new Event(UUID.randomUUID(), eventNameToNum.get(tempEventName), tid, tidToUUID.get(tid),
                                    null, timeStamp, eventName, false);
                            eventList.add(event);
                            ++timeStamp;
                            eventName = eventName.substring(k+1);
                        }
                        Event event = new Event(UUID.randomUUID(), eventNameToNum.get(eventName), tid, tidToUUID.get(tid),
                                null, timeStamp, eventName, false);
                        eventList.add(event);   // eventList hold all seperate event
                        if(eventList.size()>10000) {    // every 10000 of event should be inserted and eventList should be cleared.
                            connectionToCassandra.insertEventData(eventList);
                            eventList=new ArrayList<>();
                        }
                        return;
                    }

                    String eventName = jsonObject.get("EventName").getAsString();

                    if(eventName.contains("ProcessStart")||eventName.contains("ProcessDCStart")){
                        int pid = jsonObject.get("arguments").getAsJsonObject().get("ProcessId").getAsInt();
                        int parentPid = jsonObject.get("processID").getAsInt();
                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                        UUID uuid = UUID.randomUUID();

                        /*if(pidToUUID.containsKey(pid)) uuid = pidToUUID.get(pid);
                        else pidToUUID.put(pid, uuid);*/

                        // optimize: omit conditional judgment
                        pidToUUID.put(pid, uuid);

                        Subject subject = new edu.nu.forensic.db.entity.Subject(uuid, eventNameToNum.get(eventName),
                                pid, !pidToUUID.containsKey(parentPid)?null:pidToUUID.get(parentPid),  timeStamp,
                                jsonObject.get("arguments").getAsJsonObject().get("CommandLine").getAsString(),
                                jsonObject.get("arguments").getAsJsonObject().get("UserSID").getAsString(),
                                visibleWindowPid.contains(pid)?true:false);

                        subjectList.add(subject);
                        if(subjectList.size()>10000) {
                            connectionToCassandra.insertSubjectData(subjectList);
                            subjectList=new ArrayList<>();
                        }
                    }
                    else if(eventName.contains("ProcessEnd")||eventName.contains("ThreadEnd")){
                        // feasible for thread, but inadequate for process. MARK. all threads belonging to process should be terminated. pls. delete process from pidToUUID
                        int tid = jsonObject.get("threadID").getAsInt();
                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                        Event event=new Event(UUID.randomUUID(),eventName,tid,tidToUUID.get(tid),
                                null,timeStamp,eventName,false);
                        eventList.add(event);
                        if(eventList.size()>10000) {
                            connectionToCassandra.insertEventData(eventList);
                            eventList=new ArrayList<>();
                        }
                        tidToUUID.remove(tid);
                        reducer.notifyFileDelete(tid+""); //Bad function naming though...

                    }
                    else if(eventName.contains("FileIoRename")||eventName.contains("FileIoDelete")){
                        int tid = jsonObject.get("threadID").getAsInt();
                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                        String filename = jsonObject.get("arguments").getAsJsonObject().get("FileName").getAsString();
                        UUID uuid = UUID.randomUUID();
                        Event event;

                        if(fileNameToUUID.containsKey(filename)){   // if file exits, temp-store its uuid and remove relative mapping.
                            uuid = fileNameToUUID.get(filename);
                            fileNameToUUID.remove(filename);

                            event = new Event(uuid, eventNameToNum.get(eventName), tid,
                                    tidToUUID.get(tid), filename, timeStamp, eventName, false);

                        }
                        else{
                            event = new Event(uuid, eventNameToNum.get(eventName), tid,
                                    tidToUUID.get(tid), filename, timeStamp, eventName, true);
                        }

                        //if the event name is fileIoRename, we should put new file name to object table, but I think it is not necessary to store event table;
                        if(eventName.contains("FileIoRename")){
                            String newFileName = jsonObject.get("arguments").getAsJsonObject().get("NewFileName").getAsString();
                            reducer.notifyFileRename(filename,newFileName);

                            fileNameToUUID.put(newFileName, uuid);

                            Event newEvent = new Event(uuid, eventNameToNum.get(eventName), tid,
                                    tidToUUID.get(tid), newFileName, timeStamp, eventName, true);

                            eventList.add(newEvent);
                        }
                        //if the event name is fileIoDelete, we should store it in event table;
                        else eventList.add(event);

                        if(eventName.contains("FileIoDelete")) {
                            reducer.notifyFileDelete(filename);
                            fileNameToUUID.remove(filename);
                        }

                        if(eventList.size()>10000) {
                            connectionToCassandra.insertEventData(eventList);
                            eventList=new ArrayList<>();
                        }
                    }
                    else if(eventName.contains("FileIo")){
                        int tid = jsonObject.get("threadID").getAsInt();
                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                        String filename = jsonObject.get("arguments").getAsJsonObject().get("FileName").getAsString();
                        UUID uuid = UUID.randomUUID();

                        Event event;
                        if(fileNameToUUID.containsKey(filename)) {
                            uuid = fileNameToUUID.get(filename);

                            event = new Event(uuid, eventNameToNum.get(eventName), tid,
                                    tidToUUID.get(tid), filename, timeStamp, eventName, false);
                        }
                        else{

                            event = new Event(uuid, eventNameToNum.get(eventName), tid,
                                    tidToUUID.get(tid), filename, timeStamp, eventName, true);

                            fileNameToUUID.put(filename, uuid);
                        }
                        if(!reducer.canBeRemoved(event))
                            eventList.add(event);
                        if(eventList.size()>10000) {
                            System.out.println("Saving file... ");
                            connectionToCassandra.insertEventData(eventList);
                            eventList=new ArrayList<>();
                        }
                    }
                    // FileIoRead & FileIoWrite
                    else if(eventName.contains("Image")){
                        //To do: handle ImageDCStart and ImageLoad

                        int tid = jsonObject.get("threadID").getAsInt();
                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                        String filename = jsonObject.get("arguments").getAsJsonObject().get("FileName").getAsString();

                        Event event=new Event(UUID.randomUUID(), eventNameToNum.get(eventName), tid, tidToUUID.get(tid),
                                filename, timeStamp, eventName, false);
                        eventList.add(event);
                        if(eventList.size()>10000) {
                            System.out.println("Saving file... ");
                            connectionToCassandra.insertEventData(eventList);
                            eventList=new ArrayList<>();
                        }
                    }
                    else if(eventName.contains("TcpIp")||eventName.contains("UdpIp")){
                        int tid = jsonObject.get("threadID").getAsInt();
                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                        Integer localAddress = jsonObject.get("arguments").getAsJsonObject().get("saddr").getAsInt();
                        Integer remoteAddress = jsonObject.get("arguments").getAsJsonObject().get("daddr").getAsInt();
                        Integer localPort = jsonObject.get("arguments").getAsJsonObject().get("sport").getAsInt();
                        Integer remotePort = jsonObject.get("arguments").getAsJsonObject().get("dport").getAsInt();

                        NetFlowObject netFlowObject = new NetFlowObject(localAddress, localPort, remoteAddress, remotePort,
                                tidToUUID.get(tid), timeStamp, eventNameToNum.get(eventName), tid);

                        netList.add(netFlowObject);
                        if(netList.size()>10000) {
                            System.out.println("Saving network... ");
                            connectionToCassandra.insertNetworkEvent(netList);
                            netList=new ArrayList<>();
                        }
                    }
                    else if(eventName.contains("VisibleWindowInfo")) {
                        int pid = jsonObject.get("processID").getAsInt();
                        if(!visibleWindowPid.contains(pid)) visibleWindowPid.add(pid);
                    }
                    else if (eventName.equals("ThreadStart")|| eventName.equals("ThreadDCStart")){
                        int tid = jsonObject.get("arguments").getAsJsonObject().get("TThreadId").getAsInt();
                        int parentPid = jsonObject.get("processID").getAsInt();
                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                        UUID uuid = UUID.randomUUID();
                        tidToUUID.put(tid, uuid);
                        Subject subject = new edu.nu.forensic.db.entity.Subject(uuid, eventNameToNum.get(eventName), tid,
                                !pidToUUID.containsKey(parentPid)?null:pidToUUID.get(parentPid),  timeStamp,"thread", null, false);
                        subjectList.add(subject);
                        if(subjectList.size()>10000) {
                            connectionToCassandra.insertSubjectData(subjectList);
                            subjectList=new ArrayList<>();
                        }
                    }
                    else if(!eventNames.contains(eventName)) {
                        eventNames.add(eventName);
                        System.err.println("No handler found for: "+line);
                    }
                    else {
                        System.err.println("No handler found for: "+line);
                    }
                }catch (Exception e){
                    System.err.println(line);
                    e.printStackTrace();
                }
            }
            );
            consumer.commitAsync();
        }
    }
    private static String transferIntIPToStringIP(Integer IP){
        String IP1 = String.valueOf(IP&0xff);
        IP = IP>>8;
        String IP2 = String.valueOf(IP&0xff);
        IP = IP>>8;
        String IP3 = String.valueOf(IP&0xff);
        IP = IP>>8;
        String IP4 = String.valueOf(IP&0xff);
        String result = IP1+"."+IP2+"."+IP3+"."+IP4;
        return result;
    }
}
