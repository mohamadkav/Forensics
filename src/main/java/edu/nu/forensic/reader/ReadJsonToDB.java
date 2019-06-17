package edu.nu.forensic.reader;

import com.google.gson.Gson;
import edu.nu.forensic.JsonFormat.ETWEvent;
import edu.nu.forensic.db.DBApi.PostGreSqlApi;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.Subject;
import org.parboiled.common.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class ReadJsonToDB {


// UUID format: cid

    public void readEvent(File source, String machineNum){
        PostGreSqlApi postGreSqlApi = new PostGreSqlApi("jdbc:postgresql://localhost:5432/testdb", "postgres", "123456", machineNum);
        try{
            int i = 0;
            String line = null;
            List<Event> eventList = new ArrayList<>();
            List<Subject> subjectList = new ArrayList<>();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(source));
            Gson gson = new Gson();
            while((line = bufferedReader.readLine())!=null){
                i++;
                if(i<=68131452) continue;
                try {
                    if (line.contains("ProcessStart") || line.contains("ProcessDCStart")) {
                        ETWEvent etwEvent = gson.fromJson(line, ETWEvent.class);
                        Subject subject = new Subject();
                        subject.setCmdLine(etwEvent.arguments.ImageFileName);
                        subject.setCid(etwEvent.processID);
                        subject.setUuid(String.valueOf(etwEvent.processID));
                        subject.setStartTimestampNanos(etwEvent.TimeStamp);
                        subject.setParentSubjectUUID(String.valueOf(etwEvent.arguments.ParentId));
                        subjectList.add(subject);
                    } else if (line.contains("FileIoRead") || line.contains("FileIoWrite")) {
                        ETWEvent etwEvent = gson.fromJson(line, ETWEvent.class);
                        Event event = new Event();
                        event.setPredicateObjectPath(etwEvent.arguments.FileName);
                        event.setId(etwEvent.arguments.FileKey);
                        event.setType(etwEvent.EventName);
                        event.setSubjectUUID(String.valueOf(etwEvent.processID));
                        event.setTimestampNanos(etwEvent.TimeStamp);
                        eventList.add(event);
                    }
                    if (subjectList.size() >= 1000) {
                        postGreSqlApi.storeSubject(subjectList);
                        subjectList = new ArrayList<>();
                        System.out.println(1111111);
                    }
                    if (eventList.size() >= 10000) {
                        postGreSqlApi.storeEvent(eventList);
                        eventList = new ArrayList<>();
                        System.out.println(2222222);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                    System.out.println(line);
                }
            }
            postGreSqlApi.storeSubject(subjectList);
            postGreSqlApi.storeEvent(eventList);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
