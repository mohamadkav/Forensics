package edu.nu.forensic.reader;


import com.bbn.tc.schema.avro.cdm19.SubjectType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.Subject;
import edu.nu.forensic.db.repository.EventRepository;
import edu.nu.forensic.db.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Component
public class JsonReader {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private EventRepository eventRepository;

    private Map <Integer, UUID> tidToUUID=new HashMap<>();
    private Map <Integer, UUID> pidToUUID=new HashMap<>();

    private List<Event> toBeSavedEvents=new ArrayList<>();
    public void readTrace(File source) throws IOException {
        Scanner input = new Scanner(source);
        long startTime = System.currentTimeMillis();
        long counter=0;
        while (input.hasNext()){

                long stopTime = System.currentTimeMillis();
                long elapsedTime = stopTime - startTime;
                counter++;
                if (elapsedTime > 1000) {
                    System.out.println("Ingested: " + counter);
                    startTime = System.currentTimeMillis();
                }
                String raw = input.nextLine();
            try {
                JsonObject jsonObject = new JsonParser().parse(raw).getAsJsonObject();
                if(!jsonObject.has("EventName"))
                    continue;
                String eventName = jsonObject.get("EventName").getAsString();
                switch (eventName) {
                    case "ThreadStart":
                    case "ThreadDCStart": {
                        int tid = jsonObject.get("arguments").getAsJsonObject().get("TThreadId").getAsInt();
                        int parentPid = jsonObject.get("processID").getAsInt();
                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                        UUID uuid = UUID.randomUUID();
                        Subject parent = null;
                        if (pidToUUID.containsKey(parentPid))
                            parent = subjectRepository.findById(pidToUUID.get(parentPid)).get();
                        tidToUUID.put(tid, uuid);
                        Subject subject = new edu.nu.forensic.db.entity.Subject(uuid, "SUBJECT_THREAD",
                                tid, parent==null?null:parent.getUuid(), null, timeStamp,
                                null, null);
                        subjectRepository.save(subject);
                        break;
                    }
                    case "ProcessStart":
                    case "ProcessDCStart": {
                        int pid = jsonObject.get("arguments").getAsJsonObject().get("ProcessId").getAsInt();
                        int parentPid = jsonObject.get("processID").getAsInt();
                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                        UUID uuid = UUID.randomUUID();
                        Subject parent = null;
                        if (pidToUUID.containsKey(parentPid))
                            parent = subjectRepository.findById(pidToUUID.get(parentPid)).get();
                        pidToUUID.put(pid, uuid);
                        Subject subject = new edu.nu.forensic.db.entity.Subject(uuid, "SUBJECT_PROCESS",
                                pid, parent==null?null:parent.getUuid(), null, timeStamp,
                                jsonObject.get("arguments").getAsJsonObject().get("CommandLine").getAsString(), null);
                        subjectRepository.save(subject);
                        break;
                    }
                    case "FileIoRead":
                    case "FileIoWrite": {
                        int tid = jsonObject.get("threadID").getAsInt();
                        long timeStamp = jsonObject.get("TimeStamp").getAsLong();
                        UUID uuid = UUID.randomUUID();
                        Event event = new Event(uuid, eventName.equals("FileIoRead") ? "EVENT_READ" : "EVENT_WRITE", tid, tidToUUID.get(tid),
                                jsonObject.get("arguments").getAsJsonObject().get("FileName").getAsString(), timeStamp, eventName.equals("FileIoRead") ? "FileIoRead" : "FileIoWrite");
                        toBeSavedEvents.add(event);
                        if(toBeSavedEvents.size()>1000) {
                            System.out.println("Saving...");
                            eventRepository.saveAll(toBeSavedEvents);
                            toBeSavedEvents=new ArrayList<>();
                        }
                        break;
                    }
                }
            }catch (Exception e){
                System.err.println(raw);
                e.printStackTrace();
            }
        }
    }
}
