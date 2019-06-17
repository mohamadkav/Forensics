package edu.nu.forensic.db.DBApi;

import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.Subject;

import java.sql.*;
import java.util.*;

public class PostGreSqlApi {

    private static Connection c = null;
    private static List<String> StoreSubjectLists = new LinkedList<>();
    private static Statement stmt = null;
    private static int CountInTable = 0;
    private static String NumInTable = null;
    private static PreparedStatement pstSubj = null;
    private static PreparedStatement pstEvent = null;
    private static Boolean createSubj = false;
    private static PreparedStatement pstTempFile = null;

    //if you want to use Node Merge with other algo, please use this constructor
    public PostGreSqlApi(String machineNum) {
        Connection c = null;
        this.NumInTable = machineNum;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/testdb", "postgres", "123456");
            c.setAutoCommit(false);
            stmt = c.createStatement();
            String sql = "insert into TempFile_" +NumInTable + " (FILENAME) values (?)";
            pstTempFile = c.prepareStatement(sql);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        System.out.println("Open database successfully");
        this.c = c;
        try{
            this.c.commit();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(0);
        }
    }

    // this constructor will create subject and file table, if you use this program with other algo, please use another one.
    public PostGreSqlApi(String url, String user, String passwd, String machineNum){
        Connection c = null;
        this.NumInTable = machineNum;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection(url, user, passwd);
            c.setAutoCommit(false);
            stmt = c.createStatement();
            if(!createSubj){
                CreateTable();
                createTempFileName();
                createSubj = true;
            }
            String sqlSubj = "insert into Subject_" +NumInTable + " (UUID, PID, NAME, PARENTUUID, TIMESTAMP) values (?,?,?,?,?)";
            pstSubj = c.prepareStatement(sqlSubj);
            String sqlEvent = "insert into File_" +NumInTable+ " (UUID, FILENAME, EVENTTYPE, TIMESTAMP, SUBJECTUUID) values (?,?,?,?,?)";
            pstEvent = c.prepareStatement(sqlEvent);
            String sql = "insert into TempFile_" +NumInTable + " (FILENAME) values (?)";
            pstTempFile = c.prepareStatement(sql);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        System.out.println("Open database successfully");
        this.c = c;
        try{
            this.c.commit();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static synchronized void storeSubject(List<Subject> subjectlists) {
        try {
            for (Subject subject : subjectlists) {
                String parentSubjectUUID = null;
                if(subject.getParentSubjectUUID()!=null) parentSubjectUUID = subject.getParentSubjectUUID().toString();
                try{
                    if(!StoreSubjectLists.contains(subject.getUuid())) {
                        StoreSubjectLists.add(subject.getUuid());
                        pstSubj.setString(1, subject.getUuid());
                        pstSubj.setString(2, String.valueOf(subject.getCid()));
                        pstSubj.setString(3, subject.getCmdLine());
                        pstSubj.setString(4, parentSubjectUUID);
                        pstSubj.setString(5, String.valueOf(subject.getStartTimestampNanos()));
//                        String sql = "INSERT INTO \"Subject_\" (UUID,PID,NAME,PARENTUUID,TIMESTAMP) VALUES ('" +
//                                subject.getUuid() + "' , '" +
//                                subject.getCid() + "' , '" +
//                                subject.getCmdLine() + "' , '" +
//                                parentSubjectUUID+ "' , '" +
//                                subject.getStartTimestampNanos() + " ');";
//                        stmt.execute(sql);
                        pstSubj.executeUpdate();
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

    public static synchronized void storeTempFileName(Set<String> fileList){

        try {
            for (String it : fileList) {
                System.out.println(it);
                pstTempFile.setString(1, it);
                pstTempFile.executeUpdate();
            }
            c.commit();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static synchronized Map<String, Integer> getTempFileName(){
        Map<String, Integer> result = new HashMap<>();
        try{
            String sqlfile = "SELECT FILENAME FROM TempFile_"+NumInTable;
            ResultSet resultSet = stmt.executeQuery(sqlfile);
            while(resultSet.next()){
                result.put(resultSet.getString(1),20);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("tempsize"+result.size());
        return result;
    }

    public static synchronized void createTempFileName(){
        try {
            String sqlfile = "CREATE TABLE IF NOT EXISTS TempFile_"+NumInTable+ " (FILENAME  VARCHAR NOT NULL)";
            stmt.execute(sqlfile);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static synchronized void storeEvent(List<Event> eventlists) {
        try {
            for (Event event : eventlists) {
                try{
                    pstEvent.setString(1, event.getId().toString());
                    pstEvent.setString(2, event.getPredicateObjectPath().toString());
                    pstEvent.setString(3, event.getType());
                    pstEvent.setString(4, String.valueOf(event.getTimestampNanos()));
                    pstEvent.setString(5, event.getSubjectUUID().toString());
//                    String sql = "INSERT INTO \"File_" +NumInTable+ "\" (UUID,FILENAME,EVENTTYPE,TIMESTAMP,SUBJECTUUID) VALUES ('" +
//                                event.getId() + "' , '+" +
//                                event.getPredicateObjectPath() + "' , '" +
//                                event.getType() + "' , '" +
//                                event.getTimestampNanos() +"' , '" +
//                                event.getSubjectUUID()+ " ');";
//                    stmt.execute(sql);
                    pstEvent.executeUpdate();
                }catch (Exception e){
                    e.printStackTrace();
                    e.getMessage();
                    System.exit(0);
                }
            }
            c.commit();
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

    @Deprecated
    private static void CreateFileTable(){
        try {
            String sqlfile = "CREATE TABLE File_"+NumInTable+
                    " (UUID   VARCHAR     NOT NULL," +
                    " FILENAME       VARCHAR    NOT NULL, " +
                    " EVENTTYPE      VARCHAR   NOT NULL, "+
                    " TIMESTAMP       VARCHAR NOT NULL, " +
                    " SUBJECTUUID  VARCHAR NOT NULL)";
            stmt.execute(sqlfile);
            c.commit();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    @Deprecated
    private static void CreateSubjectTable(){
        try {
            String sqlsubject = "CREATE TABLE Subject_"+NumInTable+
                    " (UUID  VARCHAR PRIMARY KEY    NOT NULL," +
                    " PID        VARCHAR    NOT NULL, " +
                    " NAME       VARCHAR    NOT NULL, " +
                    " PARENTUUID   VARCHAR , " +
                    " TIMESTAMP     VARCHAR  NOT NULL)";
            stmt.execute(sqlsubject);
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void CreateTable(){
        try {
            String sqlfile = "CREATE TABLE File_"+NumInTable+
                    " (UUID   VARCHAR     NOT NULL," +
                    " FILENAME       VARCHAR    NOT NULL, " +
                    " EVENTTYPE      VARCHAR   NOT NULL, "+
                    " TIMESTAMP       VARCHAR NOT NULL, " +
                    " SUBJECTUUID  VARCHAR NOT NULL)";
            stmt.execute(sqlfile);
            String sqlsubject = "CREATE TABLE Subject_"+NumInTable+
                    " (UUID  VARCHAR PRIMARY KEY    NOT NULL," +
                    " PID        VARCHAR    NOT NULL, " +
                    " NAME       VARCHAR    NOT NULL, " +
                    " PARENTUUID   VARCHAR , " +
                    " TIMESTAMP     VARCHAR  NOT NULL)";
            stmt.execute(sqlsubject);
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
            int threshold = 1;
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

    public synchronized Map<String, Map<String,Integer>> getProcessToFileFrequences(String machineNum){
        try{
            int threshold = 1;
            Map<String, Map<String, Integer>> result = new HashMap<>();
            //get last file name
            Map<String, Integer> lastFileName = getTempFileName();
            result.put("temp", lastFileName);

            List<String> NotReadOnlyFiles = new LinkedList<>();
            Map<String, Integer> temp = new TreeMap<>();
            String sqlFindFiles = "SELECT SUBJECTUUID, FILENAME, EVENTTYPE, COUNT(*) as count FROM File_"+machineNum
                    +" GROUP BY SUBJECTUUID, FILENAME, EVENTTYPE";
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
                    if(lastFileName.keySet().contains(filename)){
                        lastFileName.remove(filename);
                        result.put("temp", lastFileName);
                    }
                }
                else if(!NotReadOnlyFiles.contains(filename)&&count>threshold){
                    if(!temp.containsKey(filename)) temp.put(filename,count);
                }
                if(!fileResult.next()) lastSubjectUUID = SubjectUUID;
            }
            if(temp.size()!=0&&temp.size()!=1) result.put(lastSubjectUUID, temp);
            return result;
        }catch (Exception e){
            e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

}
