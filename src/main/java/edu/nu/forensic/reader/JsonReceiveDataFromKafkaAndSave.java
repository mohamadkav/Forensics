package edu.nu.forensic.reader;

import edu.nu.forensic.GlobalConfig;
import edu.nu.forensic.db.cassandra.ConnectionToCassandra;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.NetFlowObject;
import edu.nu.forensic.db.entity.Subject;
import edu.nu.forensic.etw.*;
import edu.nu.forensic.reducer.CPRStandAloneReducer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.xerial.snappy.Snappy;

import java.time.Duration;
import java.util.*;

public class JsonReceiveDataFromKafkaAndSave {

    public static void main(String[] args) {
        List<Thread> threadList=new ArrayList<>(GlobalConfig.NUM_SERVERS);   // one threadlist
        for(int i=0;i<GlobalConfig.NUM_SERVERS;i++){
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
//    private Map <String, Integer> eventNameToNum = EventNameToNum.FileIoDelete.getEventNameToNum();

    private Set<Integer> visibleWindowPid = new HashSet<>();
    private Set<String> eventNames = new HashSet<>();

    private ConnectionToCassandra connectionToCassandra;

    private Consumer<Long, byte[]> consumer;
    private long consumed=0;
    private int threadId;
    private CPRStandAloneReducer reducer=new CPRStandAloneReducer();

    public JsonReceiverThread(int threadId){    // creating JsonReceiverThread, each with an identifier as threadId (the same with its offset in threadList)
        this.threadId=threadId;
    }

    public void run(){
        connectionToCassandra=new ConnectionToCassandra(threadId);    // create new connection, providing threadId
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, GlobalConfig.KAFKA_SERVER);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "MARPLE");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 6000000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 100000);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 30000);

        consumer = new KafkaConsumer<>(props);  // use properties and define consumer
        consumer.subscribe(Collections.singletonList("abc"+threadId));   // allow consumer to subscribe only to ["MARPLE"+threadId] topic
        System.err.println("Consumer subscribed to topic MARPLE"+threadId);
        while (true) {
            ConsumerRecords<Long, byte[]> consumerRecords = consumer.poll(Duration.ofSeconds(5));  // receive records from kafka cluster, otherwise wait
            // 1000 is the time in milliseconds consumer will wait if no record is found at broker.
            consumerRecords.forEach(record -> { // traverse consumer records
                consumed++; // use consumed to flag record number
                if(consumed%10000==0)   // each 10000 records reported
                    System.err.println("Thread "+threadId+" consumed "+consumed);
                //String line = record.value(); // use String line to hold record
                try {
                    byte[] uncompressedMessage = Snappy.uncompress(record.value());
                    Etwdata.EventRecords eventRecords = Etwdata.EventRecords.parseFrom(uncompressedMessage);
                    int dataIndex = 0;
                    List<EtwEventType> etwEventTypeList = EtwMessageProcessor.preCheck(eventRecords);
                    List<EventRecord> finalRecords=EtwMessageProcessor.constructEventRecord(eventRecords,etwEventTypeList);
                    for(EventRecord eventRecord:finalRecords) {
                        try {
                            switch (eventRecord.getEtwEventType()) {
                                case Unknown:
                                    System.err.println("Unknown event?");
                                    break;
                                case Callstack: {
                                    Event event = new Event(UUID.randomUUID(), EtwEventType.Callstack.ordinal(), eventRecord.getThreadId(), tidToUUID.get(eventRecord.getThreadId()),
                                            null, eventRecord.getTimestamp(), eventRecord.getEtwEventType().name(), false);
                                    eventList.add(event);   // eventList hold all seperate event
                                    if (eventList.size() > 10000) {    // every 10000 of event should be inserted and eventList should be cleared.
                                        connectionToCassandra.insertEventData(eventList,threadId);
                                        eventList = new ArrayList<>();
                                    }
                                    break;
                                }
                                case ProcessStart:
                                case ProcessDCStart: {
                                    UUID uuid = UUID.randomUUID();
                                    int pid = (int) (long) (eventRecord.getArgument().get("ProcessId"));
                                    int parentPid = eventRecord.getProcessId();
                                    pidToUUID.put(pid, uuid);
                                    String commandLine="";
                                    if(eventRecord.getArgument().get("CommandLine") instanceof String){
                                        commandLine=(String) eventRecord.getArgument().get("CommandLine");
                                    }
                                    Subject subject = new edu.nu.forensic.db.entity.Subject(uuid, eventRecord.getEtwEventType().ordinal(),
                                            pid, !pidToUUID.containsKey(parentPid) ? null : pidToUUID.get(parentPid), eventRecord.getTimestamp(),
                                            commandLine,
                                            (String) eventRecord.getArgument().get("UserSID"),
                                            visibleWindowPid.contains(pid) ? true : false);

                                    subjectList.add(subject);
                                    if (subjectList.size() > 10000) {
                                        connectionToCassandra.insertSubjectData(subjectList,threadId);
                                        subjectList = new ArrayList<>();
                                    }
                                    break;
                                }
                                case ProcessEnd:
                                case ThreadEnd:
                                case ProcessDCEnd:
                                case ThreadDCEnd: {
                                    Event event = new Event(UUID.randomUUID(), eventRecord.getEtwEventType().ordinal(), eventRecord.getThreadId(), tidToUUID.get(eventRecord.getThreadId()),
                                            null, eventRecord.getTimestamp(), eventRecord.getEtwEventType().name(), false);
                                    eventList.add(event);
                                    if (eventList.size() > 10000) {
                                        connectionToCassandra.insertEventData(eventList,threadId);
                                        eventList = new ArrayList<>();
                                    }
                                    tidToUUID.remove(eventRecord.getThreadId());
                                    reducer.notifyFileDelete(eventRecord.getThreadId() + ""); //Bad function naming though...
                                    break;
                                }
                                case FileIoRename:
                                case FileIoDelete: {
                                    int tid = eventRecord.getThreadId();
                                    long timeStamp = eventRecord.getTimestamp();
                                    String filename = (String) eventRecord.getArgument().get("FileName");
                                    UUID uuid = UUID.randomUUID();
                                    Event event;

                                    if (fileNameToUUID.containsKey(filename)) {   // if file exits, temp-store its uuid and remove relative mapping.
                                        uuid = fileNameToUUID.get(filename);
                                        fileNameToUUID.remove(filename);

                                        event = new Event(uuid, eventRecord.getEtwEventType().ordinal(), tid,
                                                tidToUUID.get(tid), filename, timeStamp, eventRecord.getEtwEventType().name(), false);

                                    } else {
                                        event = new Event(uuid, eventRecord.getEtwEventType().ordinal(), tid,
                                                tidToUUID.get(tid), filename, timeStamp, eventRecord.getEtwEventType().name(), true);
                                    }

                                    //if the event name is fileIoRename, we should put new file name to object table, but I think it is not necessary to store event table;
                                    if (eventRecord.getEtwEventType().equals(EtwEventType.FileIoRename)) {
                                        String newFileName = (String) eventRecord.getArgument().get("NewFileName");
                                        reducer.notifyFileRename(filename, newFileName);

                                        fileNameToUUID.put(newFileName, uuid);

                                        Event newEvent = new Event(uuid, eventRecord.getEtwEventType().ordinal(), tid,
                                                tidToUUID.get(tid), newFileName, timeStamp, eventRecord.getEtwEventType().name(), true);

                                        eventList.add(newEvent);
                                    }
                                    //if the event name is fileIoDelete, we should store it in event table;
                                    else eventList.add(event);

                                    if (eventRecord.getEtwEventType().equals(EtwEventType.FileIoDelete)) {
                                        reducer.notifyFileDelete(filename);
                                        fileNameToUUID.remove(filename);
                                    }

                                    if (eventList.size() > 10000) {
                                        connectionToCassandra.insertEventData(eventList,threadId);
                                        eventList = new ArrayList<>();
                                    }
                                    break;
                                }
                                case FileIoCleanup:
                                case FileIoClose:
                                case FileIoCreate:
                                case FileIoFileCreate:
                                case FileIoQueryInfo:
                                case FileIoRead:
                                case FileIoWrite:
                                case FileIoSetInfo:
                                case FileIoRenamePath: {
                                    String filename = (String) eventRecord.getArgument().get("FileName");
                                    UUID uuid = UUID.randomUUID();

                                    Event event;
                                    if (fileNameToUUID.containsKey(filename)) {
                                        uuid = fileNameToUUID.get(filename);

                                        event = new Event(uuid, eventRecord.getEtwEventType().ordinal(), eventRecord.getThreadId(),
                                                tidToUUID.get(eventRecord.getThreadId()), filename, eventRecord.getTimestamp(), eventRecord.getEtwEventType().name(), false);
                                    } else {

                                        event = new Event(uuid, eventRecord.getEtwEventType().ordinal(), eventRecord.getThreadId(),
                                                tidToUUID.get(eventRecord.getThreadId()), filename, eventRecord.getTimestamp(), eventRecord.getEtwEventType().name(), true);

                                        fileNameToUUID.put(filename, uuid);
                                    }
                                    if (!reducer.canBeRemoved(event))
                                        eventList.add(event);
                                    if (eventList.size() > 10000) {
                                        connectionToCassandra.insertEventData(eventList,threadId);
                                        eventList = new ArrayList<>();
                                    }
                                    break;
                                }
                                case ImageDCStart:
                                case ImageDCEnd:
                                case ImageLoad: {

                                    String filename = (String) eventRecord.getArgument().get("FileName");

                                    Event event = new Event(UUID.randomUUID(), eventRecord.getEtwEventType().ordinal(), eventRecord.getThreadId(), tidToUUID.get(eventRecord.getThreadId()),
                                            filename, eventRecord.getTimestamp(), eventRecord.getEtwEventType().name(), false);
                                    eventList.add(event);
                                    if (eventList.size() > 10000) {
                                        connectionToCassandra.insertEventData(eventList,threadId);
                                        eventList = new ArrayList<>();
                                    }
                                    break;
                                }
                                case TcpIpAcceptIPV4:
                                case TcpIpConnectIPV4:
                                case TcpIpDisconnectIPV4:
                                case TcpIpReconnectIPV4:
                                case TcpIpRecvIPV4:
                                case TcpIpRetransmitIPV4:
                                case TcpIpSendIPV4:
                                case TcpIpTCPCopyIPV4:
                                case UdpIpRecvIPV4:
                                case UdpIpSendIPV4: {
                                    Integer localAddress = (int) (long) (eventRecord.getArgument().get("saddr"));
                                    Integer remoteAddress = (int) (long) (eventRecord.getArgument().get("daddr"));
                                    Integer localPort = (int) (long) (eventRecord.getArgument().get("sport"));
                                    Integer remotePort = (int) (long) (eventRecord.getArgument().get("dport"));

                                    NetFlowObject netFlowObject = new NetFlowObject(localAddress, localPort, remoteAddress, remotePort,
                                            tidToUUID.get(eventRecord.getThreadId()), eventRecord.getTimestamp(), eventRecord.getEtwEventType().ordinal(), eventRecord.getThreadId());

                                    netList.add(netFlowObject);
                                    if (netList.size() > 10000) {
                                        connectionToCassandra.insertNetworkEvent(netList,threadId);
                                        netList = new ArrayList<>();
                                    }
                                    break;
                                }
                                case VisibleWindowInfo: {
                                    if (!visibleWindowPid.contains(eventRecord.getProcessId()))
                                        visibleWindowPid.add(eventRecord.getProcessId());
                                    break;
                                }
                                case ThreadDCStart:
                                case ThreadStart: {
                                    int tid = (int) (long) (eventRecord.getArgument().get("TThreadId"));
                                    int parentPid = eventRecord.getProcessId();
                                    UUID uuid = UUID.randomUUID();
                                    tidToUUID.put(tid, uuid);
                                    Subject subject = new edu.nu.forensic.db.entity.Subject(uuid, eventRecord.getEtwEventType().ordinal(), tid,
                                            !pidToUUID.containsKey(parentPid) ? null : pidToUUID.get(parentPid), eventRecord.getTimestamp(), null, null, false);
                                    subjectList.add(subject);
                                    if (subjectList.size() > 10000) {
                                        connectionToCassandra.insertSubjectData(subjectList,threadId);
                                        subjectList = new ArrayList<>();
                                    }
                                    break;
                                }
                                default: {
                                    //Nothing?
                                }

                            }
                        }catch (Exception e){
                            e.printStackTrace();
                            System.out.println(eventRecord.getTimestamp());
                        }
                    }
                }catch (Exception e){
                    //System.err.println(line);
                    e.printStackTrace();
                }
            }
            );
       //     consumer.commitAsync();
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
