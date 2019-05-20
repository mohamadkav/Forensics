package edu.nu.forensic.reader;

import edu.nu.forensic.db.DBApi.PostGreSqlApi;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.Subject;

import java.util.List;

public class ReadJsonToDB {

    private PostGreSqlApi postGreSqlApi = new PostGreSqlApi("jdbc:postgresql://localhost:5432/testdb", "postgres", "123456");

    public void ReadProcessToDB(List<Subject> subjects){
        postGreSqlApi.storeSubject(subjects);
    }

    public void ReadFileToDB(List<Event> eventList){
        postGreSqlApi.storeEvent(eventList);
    }
}
