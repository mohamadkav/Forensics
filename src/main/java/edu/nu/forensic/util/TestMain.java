package edu.nu.forensic.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.nu.forensic.db.cassandra.connectionToCassandra;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.IoEventAfterCPR;
import edu.nu.forensic.db.entity.NetFlowObject;
import edu.nu.forensic.db.entity.Subject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TestMain {
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
    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        String file = "E:\\download\\2019-05-19-19-25-47.out";
        String machineNum = "2";
        String IPaddress = "10.214.148.122";
        String line = null;
        List<Subject> subjectList = new ArrayList<>();
        Map<Integer, UUID> pidToUUID = new HashMap<>();
        Set<Integer> visibleWindowPid = new HashSet<>();
        List<Event> eventList = new ArrayList<>();  //we will store event through this set
        Map<String, UUID> FileNameToUUID = new HashMap<>();
        List<NetFlowObject> netList = new ArrayList<>();
        Set<String> eventNames = new HashSet<>();
        connectionToCassandra connectionToCassandra = new connectionToCassandra(IPaddress, machineNum);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(file)));
        while((line = bufferedReader.readLine())!=null){
            try {
                JsonObject jsonObject = new JsonParser().parse(line).getAsJsonObject();
                if(line.contains("CallStack")){
                    //To do: handle Keylogger and ScreenGrab
                    continue;
                }
                String eventName = jsonObject.get("EventName").getAsString();
                if(eventName.contains("ProcessStart")||eventName.contains("ProcessDCStart")){
                    int pid = jsonObject.get("arguments").getAsJsonObject().get("ProcessId").getAsInt();
                    int parentPid = jsonObject.get("processID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
//                    UUID uuid = UUID.fromString(machineNum+"-"+pid+"-"+timeStamp+"-"+pid+"-"+machineNum);
                    UUID uuid = UUID.randomUUID();
                    if(pidToUUID.containsKey(pid)) uuid = pidToUUID.get(pid);
                    else pidToUUID.put(pid, uuid);
                    Subject subject = new edu.nu.forensic.db.entity.Subject(uuid, "SUBJECT_PROCESS",
                            pid, !pidToUUID.containsKey(parentPid)?null:pidToUUID.get(parentPid), null, timeStamp,
                            jsonObject.get("arguments").getAsJsonObject().get("CommandLine").getAsString(), null,
                            jsonObject.get("arguments").getAsJsonObject().get("UserSID").getAsString(),
                            visibleWindowPid.contains(pid)?true:false);
                    subjectList.add(subject);
                    if(subjectList.size()>10000) {
                        System.out.println("Saving Subjects...");
                        connectionToCassandra.insertSubjectData(subjectList);
                        subjectList=new ArrayList<>();
                    }
                }
                else if(eventName.contains("ProcessEnd")){
                    //To do: handle this event
                }
                else if(eventName.contains("FileIoRename")||eventName.contains("FileIoDelete")){
                    int tid = jsonObject.get("threadID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                    String filename = jsonObject.get("arguments").getAsJsonObject().get("FileName").getAsString();
                    UUID uuid = UUID.randomUUID();
                    //k = true means the map includes this key-value;
                    Event event;
                    if(FileNameToUUID.containsKey(filename)){
                        uuid = FileNameToUUID.get(filename);
                        FileNameToUUID.remove(filename);
                        event = new Event(uuid, eventName, tid,
                                pidToUUID.containsKey(jsonObject.get("processID").getAsInt())?pidToUUID.get(jsonObject.get("processID").getAsInt()):UUID.randomUUID(),
                                filename, timeStamp, "names", false);
                    }
                    else{
                        event = new Event(uuid, eventName, tid,
                                pidToUUID.containsKey(jsonObject.get("processID").getAsInt())?pidToUUID.get(jsonObject.get("processID").getAsInt()):UUID.randomUUID(),
                                filename, timeStamp, "names", true);
                    }

                    //if the event name is fileIoRename, we should put new file name to object table, but I think it is not necessary to store event table;
                    if(eventName.contains("FileIoRename")){
                        String newFileName = jsonObject.get("arguments").getAsJsonObject().get("NewFileName").getAsString();
                        FileNameToUUID.put(newFileName, uuid);
                        Event newEvent = new Event(uuid, eventName, tid,
                                pidToUUID.containsKey(jsonObject.get("processID").getAsInt())?pidToUUID.get(jsonObject.get("processID").getAsInt()):UUID.randomUUID(),
                                newFileName, timeStamp,"names", true);
                        eventList.add(newEvent);
                    }
                    //if the event name is fileIoDelete, we should store it in event table;
                    else eventList.add(event);

                    if(eventList.size()>10000) {
                        System.out.println("Saving file... ");
                        connectionToCassandra.insertEventData(eventList);
                        eventList=new ArrayList<>();
                    }
                }
                else if(eventName.contains("FileIo")){
                    int tid = jsonObject.get("threadID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                    String filename = jsonObject.get("arguments").getAsJsonObject().get("FileName").getAsString();
                    UUID uuid = UUID.randomUUID();
                    boolean k = false;
                    Event event;
                    if(FileNameToUUID.containsKey(filename)) {
                        uuid = FileNameToUUID.get(filename);
                        event = new Event(uuid, eventName, tid,
                                pidToUUID.containsKey(jsonObject.get("processID").getAsInt())?pidToUUID.get(jsonObject.get("processID").getAsInt()):UUID.randomUUID(),
                                filename, timeStamp, "names", false);
                    }
                    else{
                        event = new Event(uuid, eventName, tid,
                                pidToUUID.containsKey(jsonObject.get("processID").getAsInt())?pidToUUID.get(jsonObject.get("processID").getAsInt()):UUID.randomUUID(),
                                filename, timeStamp, "names", true);
                        FileNameToUUID.put(filename, uuid);
                    }
                    eventList.add(event);
                    if(eventList.size()>10000) {
                        System.out.println("Saving file... ");
                        connectionToCassandra.insertEventData(eventList);
                        eventList=new ArrayList<>();
                    }
                }
                // FileIoRename & FileIoDelete
                else if(eventName.contains("Image")){
                    //To do: handle ImageDCStart and ImageLoad
                }
                else if(eventName.contains("TcpIp")||eventName.contains("UdpIp")){
                    int tid = jsonObject.get("threadID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                    boolean k = false;
                    Integer localAddress = jsonObject.get("arguments").getAsJsonObject().get("saddr").getAsInt();
                    Integer remoteAddress = jsonObject.get("arguments").getAsJsonObject().get("daddr").getAsInt();
                    Integer localPort = jsonObject.get("arguments").getAsJsonObject().get("sport").getAsInt();
                    Integer remotePort = jsonObject.get("arguments").getAsJsonObject().get("dport").getAsInt();
                    NetFlowObject netFlowObject = new NetFlowObject(
                            transferIntIPToStringIP(localAddress), localPort, transferIntIPToStringIP(remoteAddress), remotePort,
                            pidToUUID.containsKey(jsonObject.get("processID").getAsInt())?pidToUUID.get(jsonObject.get("processID").getAsInt()):UUID.randomUUID(),
                            timeStamp, eventName, tid);
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
                else if(!eventName.contains("Thread")&&!eventNames.contains(eventName)) {
                    eventNames.add(eventName);
                }
            }catch (Exception e){
                System.err.println(line);
                e.printStackTrace();
            }
        }
        connectionToCassandra.insertEventData(eventList);
        connectionToCassandra.insertSubjectData(subjectList);
        connectionToCassandra.insertNetworkEvent(netList);
        for(String eventname:eventNames){
            System.out.println(eventname);
        }
        System.out.println(System.currentTimeMillis()-start);
        System.exit(0);
    }
}
