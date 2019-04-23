package edu.nu.forensic.db.DBApi;

import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.Subject;

import java.sql.*;
import java.util.*;

public class PostGreSqlApi {

    private Connection c = null;
    private List<UUID> StoreSubjectLists = new LinkedList<>();

    public PostGreSqlApi() {
        Connection c = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/testdb", "postgres", "123456");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully");
        this.c = c;
    }

    public synchronized void storeSubject(List<Subject> subjectlists) {
        try {
            Statement stmt = c.createStatement();
            for (Subject subject : subjectlists) {
                String parentSubjectUUID = null;
                if(subject.getParentSubjectUUID()!=null) parentSubjectUUID = subject.getParentSubjectUUID().toString();
                try{
                    if(!StoreSubjectLists.contains(subject.getUuid())) {
                        createsheet(subject.getUuid(), stmt);
                        StoreSubjectLists.add(subject.getUuid());
                        String sql = "INSERT INTO \"subject_" + subject.getUuid().toString() + "\" (UUID,PID,NAME,PARENTUUID,TIMESTAMP) VALUES ('" +
                                subject.getUuid().toString() + "' , '" +
                                subject.getCid() + "' , '" +
                                subject.getCmdLine() + "' , '" +
                                parentSubjectUUID+ "' , '" +
                                subject.getStartTimestampNanos() + " ');";
                        stmt.execute(sql);
                    }
                    else{
                        mergeSubject(subject, stmt, parentSubjectUUID);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    e.getMessage();
                    System.exit(0);
                }
            }
            stmt.close();
            c.commit();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public synchronized void storeEvent(List<Event> eventlists) {
        try {
            Statement statement = c.createStatement();
            for (Event event : eventlists) {
                try{
                    if(!StoreSubjectLists.contains(event.getSubjectUUID())) {
                        createsheet(event.getSubjectUUID(), statement);
                        StoreSubjectLists.add(event.getSubjectUUID());
                    }
                    String sql = "INSERT INTO \"file_" + event.getSubjectUUID().toString() + "\" (UUID,FILENAME,EVENTTYPE,TIMESTAMP) VALUES ('" +
                                event.getId().toString() + "' , '+" +
                                event.getPredicateObjectPath() + "' , '" +
                                event.getType() + "' , '" +
                                event.getTimestampNanos() + " ');";
                    statement.execute(sql);
                }catch (Exception e){
                    e.getMessage();
                }
            }
            statement.close();
            c.commit();
        } catch (Exception e) {
            e.printStackTrace();
            e.getMessage();
        }
    }

    public void mergeSubject(Subject subject, Statement statement, String parentSubjectUUID){
        try{
            ResultSet resultSet = statement.executeQuery("SELECT "+subject.getUuid().toString()+"FROM \"subject_"+subject.getUuid().toString()+"\"");
            if(resultSet.next()){
                String temp = resultSet.getString("TIMESTAMP");
                System.out.println(temp);
                String sql = "INSERT INTO \"subject_" + subject.getUuid().toString() + "\" (UUID,PID,NAME,PARENTUUID, TIMESTAMP) VALUES ('" +
                        subject.getUuid().toString() + "' , '" +
                        subject.getCid() + "' , '" +
                        subject.getCmdLine() + "' , '" +
                        parentSubjectUUID + "' , '" +
                        temp + " ');";
                statement.execute(sql);
            }
        }catch (Exception e){
            e.getMessage();
        }

    }

    public void createsheet(UUID uuid, Statement stmt){
        try {
            String sqlsubject = "CREATE TABLE \"subject_"+uuid.toString() +
                    "\" (UUID  VARCHAR PRIMARY KEY    NOT NULL," +
                    " PID        VARCHAR    NOT NULL, " +
                    " NAME       VARCHAR    NOT NULL, " +
                    " PARENTUUID   VARCHAR , " +
                    " TIMESTAMP     VARCHAR  NOT NULL)";
            stmt.executeUpdate(sqlsubject);
            String sqlfile = "CREATE TABLE \"file_"+uuid.toString() +
                    "\" (UUID   VARCHAR     NOT NULL," +
                    " FILENAME       VARCHAR    NOT NULL, " +
                    " EVENTTYPE      VARCHAR   NOT NULL, "+
                    " TIMESTAMP       VARCHAR PRIMARY KEY  NOT NULL)";
            stmt.executeUpdate(sqlfile);
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void closeConnection(){
        try{
            c.close();
        }catch (Exception e){
            System.err.println("error when shutting down");
        }
    }

    public List<String> FindTables(Statement statement){
        try {
            String sqlfindfiles = "SELECT tablename FROM  pg_tables  WHERE tablename LIKE 'file%'";
            ResultSet resultSet = statement.executeQuery(sqlfindfiles);
            List<String> filetables = new LinkedList<>();
            while(resultSet.next()){
                String FileSheetName = resultSet.getString("tablename");
                filetables.add(FileSheetName);
            }
            return filetables;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public synchronized Map<String, Map<String,Integer>> getProcessToFileFrequences(){
        try{
            Statement statement = c.createStatement();
            List<String> filetables = FindTables(statement);
            Map<String, Map<String, Integer>> result = new HashMap<>();
            List<String> NotReadOnlyFiles = new LinkedList<>();
            for(String FileSheetName: filetables){
                String SubjectSheetName = FileSheetName.replace("file","subject");
                String SubjectName = FildSubjectName(SubjectSheetName, statement);
                Map<String, Integer> temp = new TreeMap<>();
                String sqlFindFiles = "SELECT FILENAME, EVENTTYPE, COUNT(*) as count FROM \""+FileSheetName +"\" GROUP BY FILENAME, EVENTTYPE";
                ResultSet fileResult = statement.executeQuery(sqlFindFiles);
                while(fileResult.next()){
                    String filename = fileResult.getString("FILENAME");
                    String eventtype = fileResult.getString("EVENTTYPE");
                    int count = fileResult.getInt("count");
                    if(eventtype.contains("Write")) {
                        if(!NotReadOnlyFiles.contains(filename)) {
                            NotReadOnlyFiles.add(filename);
                            temp.remove(filename);
                        }
                    }
                    else if(!NotReadOnlyFiles.contains(filename)){
                        if(!temp.containsKey(filename)) temp.put(filename,count);
                    }
                }
                if(temp.size()!=0&&temp.size()!=1) result.put(SubjectName, temp);
                else result.remove(SubjectName);
            }
            statement.close();
            return result;
        }catch (Exception e){
            e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    public String FildSubjectName(String SubjectSheetName, Statement statement){
        try{
            String sqlfindSubject = "SELECT * FROM \""+SubjectSheetName +"\"";
            ResultSet resultSet = statement.executeQuery(sqlfindSubject);
            String result = null;
            if(resultSet.next()){
                result = resultSet.getString("NAME");
            }
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

}
