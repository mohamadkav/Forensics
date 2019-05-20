package edu.nu.forensic.db.DBApi;

import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.Subject;

import java.sql.*;
import java.util.*;

public class PostGreSqlApi {

    private static Connection c = null;
    private static List<UUID> StoreSubjectLists = new LinkedList<>();
    private static Statement stmt = null;
    private static int CountInTable = 0;
    private static int NumInTable = 0;

    public PostGreSqlApi() {
        Connection c = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/testdb", "postgres", "123456");
            stmt = c.createStatement();
            CreateSubjectTable();
            CreateFileTable(NumInTable);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully");
        this.c = c;
    }

    public PostGreSqlApi(String url, String user, String passwd){
        Connection c = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection(url, user, passwd);
            stmt = c.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        System.out.println("Open database successfully");
        this.c = c;
    }

    public static synchronized void storeSubject(List<Subject> subjectlists) {
        try {
            for (Subject subject : subjectlists) {
                String parentSubjectUUID = null;
                if(subject.getParentSubjectUUID()!=null) parentSubjectUUID = subject.getParentSubjectUUID().toString();
                try{
                    if(!StoreSubjectLists.contains(subject.getUuid())) {
                        StoreSubjectLists.add(subject.getUuid());
                        String sql = "INSERT INTO \"SubjectInfo\" (UUID,PID,NAME,PARENTUUID,TIMESTAMP) VALUES ('" +
                                subject.getUuid().toString() + "' , '" +
                                subject.getCid() + "' , '" +
                                subject.getCmdLine() + "' , '" +
                                parentSubjectUUID+ "' , '" +
                                subject.getStartTimestampNanos() + " ');";
                        stmt.execute(sql);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    e.getMessage();
                    System.exit(0);
                }
            }
            c.commit();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public static synchronized void storeEvent(List<Event> eventlists) {
        try {
            for (Event event : eventlists) {
                try{
                    if(CountInTable>10000000){
                        NumInTable++;
                        CreateFileTable(NumInTable);
                    }
                    String sql = "INSERT INTO \"file_" +NumInTable+ "\" (UUID,FILENAME,EVENTTYPE,TIMESTAMP,SUBJECTUUID) VALUES ('" +
                                event.getId().toString() + "' , '+" +
                                event.getPredicateObjectPath() + "' , '" +
                                event.getType() + "' , '" +
                                event.getTimestampNanos() +"' , '" +
                                event.getSubjectUUID()+ " ');";
                    stmt.execute(sql);
                    CountInTable++;
                }catch (Exception e){
                    e.getMessage();
                }
            }
//            c.commit();
        } catch (Exception e) {
            e.printStackTrace();
            e.getMessage();
        }
    }

//    public void mergeSubject(Subject subject, String parentSubjectUUID){
//        try{
//            ResultSet resultSet = stmt.executeQuery("SELECT "+subject.getUuid().toString()+"FROM \"subject_"+subject.getUuid().toString()+"\"");
//            if(resultSet.next()){
//                String temp = resultSet.getString("TIMESTAMP");
//                System.out.println(temp);
//                String sql = "INSERT INTO \"subject\" (UUID,PID,NAME,PARENTUUID, TIMESTAMP) VALUES ('" +
//                        subject.getUuid().toString() + "' , '" +
//                        subject.getCid() + "' , '" +
//                        subject.getCmdLine() + "' , '" +
//                        parentSubjectUUID + "' , '" +
//                        temp + " ');";
//                stmt.execute(sql);
//            }
//        }catch (Exception e){
//            e.getMessage();
//        }
//    }

    private static void CreateFileTable(int num){
        try {
            String sqlfile = "CREATE TABLE \"file_"+num+
                    "\" (UUID   VARCHAR     NOT NULL," +
                    " FILENAME       VARCHAR    NOT NULL, " +
                    " EVENTTYPE      VARCHAR   NOT NULL, "+
                    " TIMESTAMP       VARCHAR PRIMARY KEY  NOT NULL, " +
                    " SUBJECTUUID  VARCHAR NOT NULL)";
            stmt.executeUpdate(sqlfile);
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void CreateSubjectTable(){
        try {
            String sqlsubject = "CREATE TABLE \"SubjectInfo\" " +
                    "(UUID  VARCHAR PRIMARY KEY    NOT NULL," +
                    " PID        VARCHAR    NOT NULL, " +
                    " NAME       VARCHAR    NOT NULL, " +
                    " PARENTUUID   VARCHAR , " +
                    " TIMESTAMP     VARCHAR  NOT NULL)";
            stmt.executeUpdate(sqlsubject);
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void closeConnection(){
        try{
            stmt.close();
            c.close();
        }catch (Exception e){
            System.err.println("error when shutting down");
        }
    }

    public List<String> FindTables(){
        try {
            String sqlfindfiles = "SELECT tablename FROM  pg_tables  WHERE tablename LIKE 'file%'";
            ResultSet resultSet = stmt.executeQuery(sqlfindfiles);
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
            int threshold = 0;
            List<String> filetables = FindTables();
            Map<String, Map<String, Integer>> result = new HashMap<>();
            List<String> NotReadOnlyFiles = new LinkedList<>();
            for(String FileSheetName: filetables){
                Map<String, Integer> temp = new TreeMap<>();
                String sqlFindFiles = "SELECT SUBJECTUUID, FILENAME, EVENTTYPE, COUNT(*) as count FROM \""+FileSheetName
                        +"\" GROUP BY SUBJECTUUID, FILENAME, EVENTTYPE";
                ResultSet fileResult = stmt.executeQuery(sqlFindFiles);
                String lastSubjectUUID = null;
                while(fileResult.next()){
                    String filename = fileResult.getString("FILENAME");
                    String eventtype = fileResult.getString("EVENTTYPE");
                    String SubjectUUID = fileResult.getString("SUBJECTUUID");
                    int count = fileResult.getInt("count");
                    if(SubjectUUID!=lastSubjectUUID){
                        if(lastSubjectUUID!=null){
                            if(temp.size()!=0&&temp.size()!=1) result.put(lastSubjectUUID, temp);
                        }
                        lastSubjectUUID = SubjectUUID;
                    }
                    if(eventtype.contains("Write")) {
                        if(!NotReadOnlyFiles.contains(filename)) {
                            NotReadOnlyFiles.add(filename);
                            temp.remove(filename);
                        }
                    }
                    else if(!NotReadOnlyFiles.contains(filename)&&count>threshold){
                        if(!temp.containsKey(filename)) temp.put(filename,count);
                    }
                    if(!fileResult.next()) lastSubjectUUID = SubjectUUID;
                }
                if(temp.size()!=0&&temp.size()!=1) result.put(lastSubjectUUID, temp);
            }
            return result;
        }catch (Exception e){
            e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    public String FildSubjectName(String SubjectSheetName){
        try{
            String sqlfindSubject = "SELECT * FROM \""+SubjectSheetName +"\"";
            ResultSet resultSet = stmt.executeQuery(sqlfindSubject);
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
