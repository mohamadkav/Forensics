package edu.nu.forensic.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.nu.forensic.db.cassandra.connectionToCassandra;
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
        String file = "E:\\download\\2019-05-19-19-25-47.out";
        String machineNum = "1";
        String IPaddress = "10.214.148.122";
        String line = null;
        Set<Subject> subjectList = new HashSet<>();
        Map<Integer, UUID> pidToUUID = new HashMap<>();
        Set<Integer> visibleWindowPid = new HashSet<>();
        Set<IoEventAfterCPR> eventList = new HashSet<>();
        Set<NetFlowObject> netList = new HashSet<>();
        Set<String> eventNames = new HashSet<>();
        connectionToCassandra connectionToCassandra = new connectionToCassandra(IPaddress, machineNum);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(file)));
        int j = 0;
        while((line = bufferedReader.readLine())!=null){
            j++;
            if(j==10000) break;
            try {
                JsonObject jsonObject = new JsonParser().parse(line).getAsJsonObject();
                String eventName = jsonObject.get("EventName").getAsString();
                if(eventName.contains("ProcessStart")||eventName.contains("ProcessDCStart")){
                    int pid = jsonObject.get("arguments").getAsJsonObject().get("ProcessId").getAsInt();
                    int parentPid = jsonObject.get("processID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
//                    UUID uuid = UUID.fromString(machineNum+"-"+pid+"-"+timeStamp+"-"+pid+"-"+machineNum);
                    UUID uuid = UUID.randomUUID();
                    pidToUUID.put(pid, uuid);
                    Subject subject = new edu.nu.forensic.db.entity.Subject(uuid, "SUBJECT_PROCESS",
                            pid, !pidToUUID.containsKey(parentPid)?null:pidToUUID.get(parentPid), null, timeStamp,
                            jsonObject.get("arguments").getAsJsonObject().get("CommandLine").getAsString(), null,
                            jsonObject.get("arguments").getAsJsonObject().get("UserSID").getAsString(),
                            visibleWindowPid.contains(pid)?"visibleWindow":"NoWindow");
                    subjectList.add(subject);
                    if(subjectList.size()>1000) {
                        System.out.println("Saving Subjects...");
                        connectionToCassandra.insertSubjectData(subjectList);
                        subjectList=new HashSet<>();
                    }
                }
                else if(eventName.contains("ProcessEnd")){

                }
                else if(eventName.contains("FileIo")){
                    int tid = jsonObject.get("threadID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                    UUID uuid = UUID.randomUUID();
                    IoEventAfterCPR ioEventAfterCPR = new IoEventAfterCPR(uuid, eventName, tid,
                            pidToUUID.containsKey(jsonObject.get("processID").getAsInt())?pidToUUID.get(jsonObject.get("processID").getAsInt()):UUID.randomUUID(),
                            jsonObject.get("arguments").getAsJsonObject().get("FileName").getAsString(), timeStamp, timeStamp, "names");
                    eventList.add(ioEventAfterCPR);
                    if(eventList.size()>1000) {
                        System.out.println("Saving file... ");
                        connectionToCassandra.insertEventData(eventList);
                        eventList=new HashSet<>();
                    }
                }
                else if(eventName.contains("Image")){

                }
                else if(eventName.contains("TcpIp")||eventName.contains("UdpIp")){
                    int tid = jsonObject.get("threadID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                    UUID uuid = UUID.randomUUID();
                    Integer localAddress = jsonObject.get("arguments").getAsJsonObject().get("saddr").getAsInt();
                    Integer remoteAddress = jsonObject.get("arguments").getAsJsonObject().get("daddr").getAsInt();
                    Integer localPort = jsonObject.get("arguments").getAsJsonObject().get("sport").getAsInt();
                    Integer remotePort = jsonObject.get("arguments").getAsJsonObject().get("dport").getAsInt();
                    NetFlowObject netFlowObject = new NetFlowObject(
                            uuid, transferIntIPToStringIP(localAddress), localPort, transferIntIPToStringIP(remoteAddress), remotePort,
                            pidToUUID.containsKey(jsonObject.get("processID").getAsInt())?pidToUUID.get(jsonObject.get("processID").getAsInt()):UUID.randomUUID(),
                            timeStamp, eventName, tid);
                    netList.add(netFlowObject);
                    if(netList.size()>1000) {
                        System.out.println("Saving network... ");
                        connectionToCassandra.insertNetworkEvent(netList);
                        netList=new HashSet<>();
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
        System.exit(0);
    }
}
