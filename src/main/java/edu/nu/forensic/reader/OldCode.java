package edu.nu.forensic.reader;

public class OldCode {

//                    String line="";
//                    JsonObject jsonObject = new JsonParser().parse(line).getAsJsonObject(); // and parse line into JsonObject (each record in the form of a JsonObject)
//
//                    if(line.contains("CallStack")){
//                        String eventName = jsonObject.get("CallStack").getAsString();
//                        int tid = jsonObject.get("threadID").getAsInt();
//                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
//                        // use map uuid
//                        while(eventName.contains(",")){ // parse eventname, which may be composed of several strings and seperated by ','
//                            int k = eventName.indexOf(",");
//                            String tempEventName = eventName.substring(0, k);
//                            Event event = new Event(UUID.randomUUID(), eventNameToNum.get(tempEventName), tid, tidToUUID.get(tid),
//                                    null, timeStamp, eventName, false);
//                            eventList.add(event);
//                            ++timeStamp;
//                            eventName = eventName.substring(k+1);
//                        }
//                        Event event = new Event(UUID.randomUUID(), eventNameToNum.get(eventName), tid, tidToUUID.get(tid),
//                                null, timeStamp, eventName, false);
//                        eventList.add(event);   // eventList hold all seperate event
//                        if(eventList.size()>10000) {    // every 10000 of event should be inserted and eventList should be cleared.
//                            connectionToCassandra.insertEventData(eventList);
//                            eventList=new ArrayList<>();
//                        }
//                        return;
//                    }

//                    String eventName = jsonObject.get("EventName").getAsString();
//
//                    if(eventName.contains("ProcessStart")||eventName.contains("ProcessDCStart")){
//                        int pid = jsonObject.get("arguments").getAsJsonObject().get("ProcessId").getAsInt();
//                        int parentPid = jsonObject.get("processID").getAsInt();
//                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
//                        UUID uuid = UUID.randomUUID();
//
//                        /*if(pidToUUID.containsKey(pid)) uuid = pidToUUID.get(pid);
//                        else pidToUUID.put(pid, uuid);*/
//
//                        // optimize: omit conditional judgment
//                        pidToUUID.put(pid, uuid);
//
//                        Subject subject = new edu.nu.forensic.db.entity.Subject(uuid, eventNameToNum.get(eventName),
//                                pid, !pidToUUID.containsKey(parentPid)?null:pidToUUID.get(parentPid),  timeStamp,
//                                jsonObject.get("arguments").getAsJsonObject().get("CommandLine").getAsString(),
//                                jsonObject.get("arguments").getAsJsonObject().get("UserSID").getAsString(),
//                                visibleWindowPid.contains(pid)?true:false);
//
//                        subjectList.add(subject);
//                        if(subjectList.size()>10000) {
//                            connectionToCassandra.insertSubjectData(subjectList);
//                            subjectList=new ArrayList<>();
//                        }
//                    }
//                    else if(eventName.contains("ProcessEnd")||eventName.contains("ThreadEnd")){
//                        // feasible for thread, but inadequate for process. MARK. all threads belonging to process should be terminated. pls. delete process from pidToUUID
//                        int tid = jsonObject.get("threadID").getAsInt();
//                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
//                        Event event=new Event(UUID.randomUUID(),eventName,tid,tidToUUID.get(tid),
//                                null,timeStamp,eventName,false);
//                        eventList.add(event);
//                        if(eventList.size()>10000) {
//                            connectionToCassandra.insertEventData(eventList);
//                            eventList=new ArrayList<>();
//                        }
//                        tidToUUID.remove(tid);
//                        reducer.notifyFileDelete(tid+""); //Bad function naming though...
//
//                    }
//                    else if(eventName.contains("FileIoRename")||eventName.contains("FileIoDelete")){
//                        int tid = jsonObject.get("threadID").getAsInt();
//                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
//                        String filename = jsonObject.get("arguments").getAsJsonObject().get("FileName").getAsString();
//                        UUID uuid = UUID.randomUUID();
//                        Event event;
//
//                        if(fileNameToUUID.containsKey(filename)){   // if file exits, temp-store its uuid and remove relative mapping.
//                            uuid = fileNameToUUID.get(filename);
//                            fileNameToUUID.remove(filename);
//
//                            event = new Event(uuid, eventNameToNum.get(eventName), tid,
//                                    tidToUUID.get(tid), filename, timeStamp, eventName, false);
//
//                        }
//                        else{
//                            event = new Event(uuid, eventNameToNum.get(eventName), tid,
//                                    tidToUUID.get(tid), filename, timeStamp, eventName, true);
//                        }
//
//                        //if the event name is fileIoRename, we should put new file name to object table, but I think it is not necessary to store event table;
//                        if(eventName.contains("FileIoRename")){
//                            String newFileName = jsonObject.get("arguments").getAsJsonObject().get("NewFileName").getAsString();
//                            reducer.notifyFileRename(filename,newFileName);
//
//                            fileNameToUUID.put(newFileName, uuid);
//
//                            Event newEvent = new Event(uuid, eventNameToNum.get(eventName), tid,
//                                    tidToUUID.get(tid), newFileName, timeStamp, eventName, true);
//
//                            eventList.add(newEvent);
//                        }
//                        //if the event name is fileIoDelete, we should store it in event table;
//                        else eventList.add(event);
//
//                        if(eventName.contains("FileIoDelete")) {
//                            reducer.notifyFileDelete(filename);
//                            fileNameToUUID.remove(filename);
//                        }
//
//                        if(eventList.size()>10000) {
//                            connectionToCassandra.insertEventData(eventList);
//                            eventList=new ArrayList<>();
//                        }
//                    }
//                    else if(eventName.contains("FileIo")){
//                        int tid = jsonObject.get("threadID").getAsInt();
//                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
//                        String filename = jsonObject.get("arguments").getAsJsonObject().get("FileName").getAsString();
//                        UUID uuid = UUID.randomUUID();
//
//                        Event event;
//                        if(fileNameToUUID.containsKey(filename)) {
//                            uuid = fileNameToUUID.get(filename);
//
//                            event = new Event(uuid, eventNameToNum.get(eventName), tid,
//                                    tidToUUID.get(tid), filename, timeStamp, eventName, false);
//                        }
//                        else{
//
//                            event = new Event(uuid, eventNameToNum.get(eventName), tid,
//                                    tidToUUID.get(tid), filename, timeStamp, eventName, true);
//
//                            fileNameToUUID.put(filename, uuid);
//                        }
//                        if(!reducer.canBeRemoved(event))
//                            eventList.add(event);
//                        if(eventList.size()>10000) {
//                            System.out.println("Saving file... ");
//                            connectionToCassandra.insertEventData(eventList);
//                            eventList=new ArrayList<>();
//                        }
//                    }
    // FileIoRead & FileIoWrite
//                    else if(eventName.contains("Image")){
//                        //To do: handle ImageDCStart and ImageLoad
//
//                        int tid = jsonObject.get("threadID").getAsInt();
//                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
//                        String filename = jsonObject.get("arguments").getAsJsonObject().get("FileName").getAsString();
//
//                        Event event=new Event(UUID.randomUUID(), eventNameToNum.get(eventName), tid, tidToUUID.get(tid),
//                                filename, timeStamp, eventName, false);
//                        eventList.add(event);
//                        if(eventList.size()>10000) {
//                            System.out.println("Saving file... ");
//                            connectionToCassandra.insertEventData(eventList);
//                            eventList=new ArrayList<>();
//                        }
//                    }
//                    else if(eventName.contains("TcpIp")||eventName.contains("UdpIp")){
//                        int tid = jsonObject.get("threadID").getAsInt();
//                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
//                        Integer localAddress = jsonObject.get("arguments").getAsJsonObject().get("saddr").getAsInt();
//                        Integer remoteAddress = jsonObject.get("arguments").getAsJsonObject().get("daddr").getAsInt();
//                        Integer localPort = jsonObject.get("arguments").getAsJsonObject().get("sport").getAsInt();
//                        Integer remotePort = jsonObject.get("arguments").getAsJsonObject().get("dport").getAsInt();
//
//                        NetFlowObject netFlowObject = new NetFlowObject(localAddress, localPort, remoteAddress, remotePort,
//                                tidToUUID.get(tid), timeStamp, eventNameToNum.get(eventName), tid);
//
//                        netList.add(netFlowObject);
//                        if(netList.size()>10000) {
//                            System.out.println("Saving network... ");
//                            connectionToCassandra.insertNetworkEvent(netList);
//                            netList=new ArrayList<>();
//                        }
//                    }
//                    else if(eventName.contains("VisibleWindowInfo")) {
//                        int pid = jsonObject.get("processID").getAsInt();
//                        if(!visibleWindowPid.contains(pid)) visibleWindowPid.add(pid);
//                    }
//                    else if (eventName.equals("ThreadStart")|| eventName.equals("ThreadDCStart")){
//                        int tid = jsonObject.get("arguments").getAsJsonObject().get("TThreadId").getAsInt();
//                        int parentPid = jsonObject.get("processID").getAsInt();
//                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
//                        UUID uuid = UUID.randomUUID();
//                        tidToUUID.put(tid, uuid);
//                        Subject subject = new edu.nu.forensic.db.entity.Subject(uuid, eventNameToNum.get(eventName), tid,
//                                !pidToUUID.containsKey(parentPid)?null:pidToUUID.get(parentPid),  timeStamp,"thread", null, false);
//                        subjectList.add(subject);
//                        if(subjectList.size()>10000) {
//                            connectionToCassandra.insertSubjectData(subjectList);
//                            subjectList=new ArrayList<>();
//                        }
//                    }
//                    else if(!eventNames.contains(eventName)) {
//                        eventNames.add(eventName);
//                        System.err.println("No handler found for: "+line);
//                    }
//                    else {
//                        System.err.println("No handler found for: "+line);
//                    }
}
