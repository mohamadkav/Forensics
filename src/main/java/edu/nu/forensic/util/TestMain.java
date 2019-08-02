package edu.nu.forensic.util;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.nu.forensic.db.cassandra.connectionToCassandra;
import edu.nu.forensic.db.entity.*;

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

        //START common:

        long start = System.currentTimeMillis();
        String file = "2019-05-19-19-25-47.out";
        String machineNum = "2";
        String IPaddress = "localhost";    // 10.214.148.128
        String line = null;
        List<Subject> subjectList = new ArrayList<>();
        Map<Integer, UUID> pidToUUID = new HashMap<>();
        Set<Integer> visibleWindowPid = new HashSet<>();
        List<Event> eventList = new ArrayList<>();  //we will store event through this set
        Map<String, UUID> fileNameToUUID = new HashMap<>();
        List<NetFlowObject> netList = new ArrayList<>();
        Set<String> eventNames = new HashSet<>();
        Map<Integer, UUID> tidToUUID = new HashMap<>();
        Map<String, Integer> eventNameToNum = EventNameToNum.FileIoDelete.getEventNameToNum();
        connectionToCassandra connectionToCassandra = new connectionToCassandra(IPaddress, machineNum);

        // END common.

        // START random event for testing eventToParentprocess:

        try {
            List<UUID> subjectUUIDList = new ArrayList<>();
            String number = "12"; // ThreadEnd:12; 19 has no parent.
            String eventSearch = "select * from test.event"+ machineNum + " where eventname="+ number + " allow filtering;";
            ResultSet resultSet = connectionToCassandra.getSession().execute(eventSearch);

            Iterator<Row> rsIterator = resultSet.iterator();
            while(rsIterator.hasNext()){
                Row row = rsIterator.next();
                UUID newUUID = row.getUUID("subjectuuid");
                subjectUUIDList.add(newUUID);
            }
            System.err.println("list        size:   " + subjectUUIDList.size());

            if(subjectUUIDList.size() <= 0)
                throw new Exception("no available events");

            Random random = new Random();
            int t = random.nextInt(subjectUUIDList.size());
            System.err.println("list        offset: " + t);
            UUID uuid = subjectUUIDList.get(t);
            System.err.println("subjectuuid  chosen: " + uuid);

            Event event = new Event();
            event.setSubjectUUID(uuid);
            String parentUUIDToName = connectionToCassandra.eventToParentprocessname(event);
            System.err.println("parent process name: "+parentUUIDToName);
        } catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);

        // END random event for testing eventToParentprocess.

        // START random event for testing eventObjectUUIDToFilename:

        /*List<UUID> objectUUIDList = new ArrayList<>();

        String number = "16"; // fileIoRead:1 fileIoWrite:2 fileIoRename:16 fileIoDelete:17
        String eventSearch = "select * from test.event"+ machineNum + " where eventname="+ number + " allow filtering;";
        ResultSet resultSet = connectionToCassandra.getSession().execute(eventSearch);

        Iterator<Row> rsIterator = resultSet.iterator();
        while(rsIterator.hasNext()){
            Row row = rsIterator.next();
            UUID newUUID = row.getUUID("objectuuid");
            objectUUIDList.add(newUUID);
        }
        System.err.println("list        size:   " + objectUUIDList.size());

        Random random = new Random();
        int t = random.nextInt(objectUUIDList.size());
        System.err.println("list        offset: " + t);
        UUID uuid = objectUUIDList.get(t);
        System.err.println("objectuuid  chosen: " + uuid);

        Event event = new Event();
        event.setSubjectUUID(uuid);
        String filename = connectionToCassandra.eventObjectUUIDToFilename(event);
        System.err.println(filename);
        System.exit(0);*/

        // END random event for testing eventObjectUUIDToFilename.

        // START filename for testing filenameToUUIDFuzzy:

        /*String filename = "C:\\Users\\mayn\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\Code Cache\\js\\index-dir\\the-real-index";
        Map<String, UUID> filenameToUUID = connectionToCassandra.filenameToUUIDFuzzy(filename);
        if (filenameToUUID.isEmpty()) {
            System.err.println("no any relevant result");
        } else if (filenameToUUID.containsKey(filename)) {
            // accurate search result exits
            System.err.println("filename: " + filename);
            System.err.println("latestuuid: " + filenameToUUID.get(filename));
            System.err.println(filenameToUUID.size()-1 + " other relevant filenames");
        } else {
            // only relevant filenames
            System.err.println(filenameToUUID.size() + " relevant filenames");
        }
        System.exit(0);*/

        // END filename for testing filenameToUUID.

        // START filename for testing filenameToUUIDFuzzy:

        /*String filename = "\'C:\\Users\\mayn\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\Code Cache\\js\\index-dir\\the-real-index\'";
        UUID uuid = connectionToCassandra.filenameToUUIDExact(filename);
        System.err.println("filename: " + filename);
        System.err.println("latestuuid: " + uuid);

        System.exit(0);*/

        // END filename for testing filenameToUUID.

        // START insert data directly [UPDATED_19.8.1_MOIRA]:

        /*BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(file)));

        while((line = bufferedReader.readLine())!=null) {
            try {

                JsonObject jsonObject = new JsonParser().parse(line).getAsJsonObject();
                System.err.println(line);
                if (line.contains("CallStack")) {
                    String eventName = jsonObject.get("CallStack").getAsString();
                    int tid = jsonObject.get("threadID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                    // use map uuid
                    while (eventName.contains(",")) { // parse eventname, which may be composed of several strings and seperated by ','
                        int k = eventName.indexOf(",");
                        String tempEventName = eventName.substring(0, k);
                        Event event = new Event(UUID.randomUUID(), eventNameToNum.get(tempEventName), tid, tidToUUID.get(tid),
                                null, timeStamp, eventName, false);
                        eventList.add(event);
                        ++timeStamp;
                        eventName = eventName.substring(k + 1);
                    }
                    Event event = new Event(UUID.randomUUID(), eventNameToNum.get(eventName), tid, tidToUUID.get(tid),
                            null, timeStamp, eventName, false);
                    eventList.add(event);   // eventList hold all seperate event
                    if (eventList.size() > 10000) {    // every 10000 of event should be inserted and eventList should be cleared.
                        connectionToCassandra.insertEventData(eventList);
                        eventList = new ArrayList<>();
                    }
                    continue;
                }

                String eventName = jsonObject.get("EventName").getAsString();

                if (eventName.contains("ProcessStart") || eventName.contains("ProcessDCStart")) {
                    int pid = jsonObject.get("arguments").getAsJsonObject().get("ProcessId").getAsInt();
                    int parentPid = jsonObject.get("processID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                    UUID uuid = UUID.randomUUID();

//                        if(pidToUUID.containsKey(pid)) uuid = pidToUUID.get(pid);
//                        else pidToUUID.put(pid, uuid);

                    // optimize: omit conditional judgment
                    pidToUUID.put(pid, uuid);

                    Subject subject = new edu.nu.forensic.db.entity.Subject(uuid, eventNameToNum.get(eventName),
                            pid, !pidToUUID.containsKey(parentPid) ? null : pidToUUID.get(parentPid), timeStamp,
                            jsonObject.get("arguments").getAsJsonObject().get("CommandLine").getAsString(),
                            jsonObject.get("arguments").getAsJsonObject().get("UserSID").getAsString(),
                            visibleWindowPid.contains(pid) ? true : false);

                    subjectList.add(subject);
                    if (subjectList.size() > 10000) {
                        connectionToCassandra.insertSubjectData(subjectList);
                        subjectList = new ArrayList<>();
                    }
                } else if (eventName.contains("ProcessEnd") || eventName.contains("ThreadEnd") || eventName.contains("ProcessDCEnd") || eventName.contains("ThreadDCEnd")) {
                    // feasible for thread, but inadequate for process. MARK. all threads belonging to process should be terminated. pls. delete process from pidToUUID
                    int tid = jsonObject.get("threadID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                    Event event = new Event(UUID.randomUUID(), eventNameToNum.get(eventName), tid, tidToUUID.get(tid),
                            null, timeStamp, eventName, false);
                    eventList.add(event);
                    if (eventList.size() > 10000) {
                        connectionToCassandra.insertEventData(eventList);
                        eventList = new ArrayList<>();
                    }
                    tidToUUID.remove(tid);
                    //                    reducer.notifyFileDelete(tid+""); //Bad function naming though...
                } else if (eventName.contains("FileIoRename") || eventName.contains("FileIoDelete")) {
                    int tid = jsonObject.get("threadID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                    String filename = jsonObject.get("arguments").getAsJsonObject().get("FileName").getAsString();
                    UUID uuid = UUID.randomUUID();
                    Event event;

                    if (fileNameToUUID.containsKey(filename)) {   // if file exits, temp-store its uuid and remove relative mapping.
                        uuid = fileNameToUUID.get(filename);
                        fileNameToUUID.remove(filename);

                        event = new Event(uuid, eventNameToNum.get(eventName), tid,
                                tidToUUID.get(tid), filename, timeStamp, eventName, false);

                    } else {
                        event = new Event(uuid, eventNameToNum.get(eventName), tid,
                                tidToUUID.get(tid), filename, timeStamp, eventName, true);
                    }

                    //if the event name is fileIoRename, we should put new file name to object table, but I think it is not necessary to store event table;
                    if (eventName.contains("FileIoRename")) {
                        String newFileName = jsonObject.get("arguments").getAsJsonObject().get("NewFileName").getAsString();
                        //                        reducer.notifyFileRename(filename,newFileName);

                        fileNameToUUID.put(newFileName, uuid);

                        Event newEvent = new Event(uuid, eventNameToNum.get(eventName), tid,
                                tidToUUID.get(tid), newFileName, timeStamp, eventName, true);

                        eventList.add(newEvent);
                    }
                    //if the event name is fileIoDelete, we should store it in event table;
                    else eventList.add(event);

                    if (eventName.contains("FileIoDelete")) {
                        //                        reducer.notifyFileDelete(filename);
                        fileNameToUUID.remove(filename);
                    }

                    if (eventList.size() > 10000) {
                        connectionToCassandra.insertEventData(eventList);
                        eventList = new ArrayList<>();
                    }
                } else if (eventName.contains("FileIo")) {
                    int tid = jsonObject.get("threadID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                    String filename = jsonObject.get("arguments").getAsJsonObject().get("FileName").getAsString();
                    UUID uuid = UUID.randomUUID();

                    Event event;
                    if (fileNameToUUID.containsKey(filename)) {
                        uuid = fileNameToUUID.get(filename);

                        event = new Event(uuid, eventNameToNum.get(eventName), tid,
                                tidToUUID.get(tid), filename, timeStamp, eventName, false);
                    } else {

                        event = new Event(uuid, eventNameToNum.get(eventName), tid,
                                tidToUUID.get(tid), filename, timeStamp, eventName, true);

                        fileNameToUUID.put(filename, uuid);
                    }
                    //                    if(!reducer.canBeRemoved(event))
                    eventList.add(event);
                    if (eventList.size() > 10000) {
                        System.out.println("Saving file... ");
                        connectionToCassandra.insertEventData(eventList);
                        eventList = new ArrayList<>();
                    }
                }
                //             FileIoRead & FileIoWrite
                else if (eventName.contains("Image")) {
                    //To do: handle ImageDCStart and ImageLoad

                    int tid = jsonObject.get("threadID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                    String filename = jsonObject.get("arguments").getAsJsonObject().get("FileName").getAsString();

                    Event event = new Event(UUID.randomUUID(), eventNameToNum.get(eventName), tid, tidToUUID.get(tid),
                            filename, timeStamp, eventName, false);
                    eventList.add(event);
                    if (eventList.size() > 10000) {
                        System.out.println("Saving file... ");
                        connectionToCassandra.insertEventData(eventList);
                        eventList = new ArrayList<>();
                    }
                } else if (eventName.contains("TcpIp") || eventName.contains("UdpIp")) {
                    int tid = jsonObject.get("threadID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                    Integer localAddress = jsonObject.get("arguments").getAsJsonObject().get("saddr").getAsInt();
                    Integer remoteAddress = jsonObject.get("arguments").getAsJsonObject().get("daddr").getAsInt();
                    Integer localPort = jsonObject.get("arguments").getAsJsonObject().get("sport").getAsInt();
                    Integer remotePort = jsonObject.get("arguments").getAsJsonObject().get("dport").getAsInt();

                    NetFlowObject netFlowObject = new NetFlowObject(localAddress, localPort, remoteAddress, remotePort,
                            tidToUUID.get(tid), timeStamp, eventNameToNum.get(eventName), tid);

                    netList.add(netFlowObject);
                    if (netList.size() > 10000) {
                        System.out.println("Saving network... ");
                        connectionToCassandra.insertNetworkEvent(netList);
                        netList = new ArrayList<>();
                    }
                } else if (eventName.contains("VisibleWindowInfo")) {
                    int pid = jsonObject.get("processID").getAsInt();
                    if (!visibleWindowPid.contains(pid)) visibleWindowPid.add(pid);
                } else if (eventName.equals("ThreadStart") || eventName.equals("ThreadDCStart")) {
                    int tid = jsonObject.get("arguments").getAsJsonObject().get("TThreadId").getAsInt();
                    int parentPid = jsonObject.get("processID").getAsInt();
                    long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                    UUID uuid = UUID.randomUUID();
                    tidToUUID.put(tid, uuid);
                    Subject subject = new edu.nu.forensic.db.entity.Subject(uuid, eventNameToNum.get(eventName), tid,
                            !pidToUUID.containsKey(parentPid) ? null : pidToUUID.get(parentPid), timeStamp, null, null, false);
                    subjectList.add(subject);
                    if (subjectList.size() > 10000) {
                        connectionToCassandra.insertSubjectData(subjectList);
                        subjectList = new ArrayList<>();
                    }
                } else if (!eventNames.contains(eventName)) {
                    eventNames.add(eventName);
                    System.err.println("No handler found for: " + line);
                } else {
                    System.err.println("No handler found for: " + line);
                }

            } catch (Exception e) {
                System.err.println(line);
                e.printStackTrace();
            }
        }
        connectionToCassandra.insertEventData(eventList);
        connectionToCassandra.insertSubjectData(subjectList);
        connectionToCassandra.insertNetworkEvent(netList);
        for (String eventname : eventNames) {
            System.out.println(eventname);
        }
        System.out.println(System.currentTimeMillis() - start);
        System.exit(0);*/

        // END insert data directly.
    }

}
