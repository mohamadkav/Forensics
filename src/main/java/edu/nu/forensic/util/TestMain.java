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

    public static void main(String[] args) throws IOException {
        String file = "E:\\download\\2019-05-19-19-25-47.out";
        String machineNum = "0";
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
                    UUID uuid = UUID.fromString(machineNum+pid+timeStamp);
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
                else if(eventName.contains("FileIo")){
                    int tid = jsonObject.get("threadID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                    UUID uuid = UUID.randomUUID();
                    IoEventAfterCPR ioEventAfterCPR = new IoEventAfterCPR(uuid, eventName, tid,
                            pidToUUID.containsKey(jsonObject.get("processID").getAsInt())?pidToUUID.get(jsonObject.get("processID").getAsInt()):UUID.randomUUID(),
                            jsonObject.get("arugements").getAsJsonObject().get("FileName").getAsString(), timeStamp, timeStamp, "names");
                    eventList.add(ioEventAfterCPR);
                    if(eventList.size()>1000) {
                        connectionToCassandra.insertEventData(eventList);
                        System.err.println("Saving file... ");
                        eventList=new HashSet<>();
                    }
                }
                else if(eventName.contains("TcpIp")||eventName.contains("UdpIp")){
                    int tid = jsonObject.get("threadID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                    UUID uuid = UUID.randomUUID();
                    Integer localAddress = jsonObject.get("arguments").getAsJsonObject().get("saddr").getAsInt();
                    Integer remoteAddress = jsonObject.get("arguments").getAsJsonObject().get("daddr").getAsInt();
                    Integer localPort = jsonObject.get("arguments").getAsJsonObject().get("sport").getAsInt();
                    Integer remotePort = jsonObject.get("arguments").getAsJsonObject().get("dport").getAsInt();
                    NetFlowObject netFlowObject = new NetFlowObject(uuid, String.valueOf(localAddress), localPort, String.valueOf(remoteAddress), remotePort,
                            pidToUUID.containsKey(jsonObject.get("processID").getAsInt())?pidToUUID.get(jsonObject.get("processID").getAsInt()):UUID.randomUUID(),
                            timeStamp, eventName, tid);
                    netList.add(netFlowObject);
                    if(netList.size()>1000) {
                        connectionToCassandra.insertNetworkEvent(netList);
                        System.err.println("Saving file... ");
                        netList=new HashSet<>();
                    }
                }
                else if(eventName.contains("VisibleWindowInfo")) {
                    int pid = jsonObject.get("arguments").getAsJsonObject().get("ProcessId").getAsInt();
                    if(!visibleWindowPid.contains(pid)) visibleWindowPid.add(pid);
                }
                else if(!eventName.contains("Thread")&&!eventNames.contains(eventName)) {
                    System.out.println(eventName);
                    eventNames.add(eventName);
                }
            }catch (Exception e){
                System.err.println(line);
                e.printStackTrace();
            }
        }
    }
}
