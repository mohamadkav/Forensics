package edu.nu.forensic.db.cassandra;

import com.datastax.driver.core.*;
import edu.nu.forensic.GlobalConfig;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.NetFlowObject;
import edu.nu.forensic.db.entity.Subject;
import edu.nu.forensic.db.entity.LatestObject;
import edu.nu.forensic.reader.JsonReceiveDataFromKafkaAndSave;

import java.util.*;

public class ConnectionToCassandra {
    private static Cluster cluster;

    private static List<Session> sessions=new ArrayList<>();
    static{
        cluster = Cluster.builder().addContactPoint(GlobalConfig.CASSANDRA_SERVER).withPort(GlobalConfig.CASSANDRA_PORT).build();
        cluster.getConfiguration().getSocketOptions().setReadTimeoutMillis(300000);
        for(int i = 0; i< GlobalConfig.NUM_SERVERS/ GlobalConfig.NUM_SERVERS_PER_CONNECTION; i++){
            sessions.add(cluster.connect());
            System.out.println("Session "+i+" added");
        }
    }
    public Session getSession(int machineNum){
        return sessions.get(machineNum/GlobalConfig.NUM_SERVERS_PER_CONNECTION);
    }
//    private int TTL = 8640000‬;

    private PreparedStatement psta;
    private PreparedStatement pstaObject;
    private PreparedStatement pstaEvent;
    private PreparedStatement pstaNetwork;

    private int ttl = 8640000;

    public ConnectionToCassandra(int machineNum) {
   //     session=cluster.connect();  // generate and initialize a new conversation and use session to record connection
   //     this.machineNumber = machineNum;    // machine Number used to identify this thread
        createSubjectTable(machineNum);
        createEventAndObjectTable(machineNum);

        // 1.create table subject+threadID\event+threadID\object+threadID\network+threadID under keyspace test
        // 2.create prepared statement for each table so as to use model conveniently
    }

    private void createSubjectTable(int machineNumber){
        String sql = "CREATE TABLE IF NOT EXISTS test.subject"+machineNumber+" (" +
                "uuid uuid PRIMARY KEY," +
                "name varchar," +
                "timestamp bigint," +
                "parentuuid uuid," +
                "Usersid ascii," +
                "eventName int," +
                "visibleWindow boolean)" +
                " WITH default_time_to_live = "+ttl +
                " and compression = {'sstable_compression': 'DeflateCompressor'}" +
                " and compaction = {'class':'org.apache.cassandra.db.compaction.DateTieredCompactionStrategy'};";
        getSession(machineNumber).execute(sql);
        String insertDB = "insert into test.subject"+machineNumber+"(uuid ,name,timestamp,parentuuid,Usersid,eventName,visibleWindow) " +
                "values(?,?,?,?,?,?,?)";
        this.psta = getSession(machineNumber).prepare(insertDB);
    }

    private void createEventAndObjectTable(int machineNumber){
        String sqlEvent = "CREATE TABLE IF NOT EXISTS test.event"+machineNumber+" (" +
                "timestamp bigint PRIMARY KEY," +
                "subjectuuid uuid," +
                "objectuuid uuid," +
                "eventName int)" +
                " WITH default_time_to_live = "+ttl +
                " and compression = {'sstable_compression': 'DeflateCompressor'}" +
                " and compaction = {'class':'org.apache.cassandra.db.compaction.DateTieredCompactionStrategy'};";
        getSession(machineNumber).execute(sqlEvent);
        String insertDBEvent = "insert into test.event"+machineNumber+"" +
                "(timestamp,subjectuuid,objectuuid,eventName) " +
                "values(?,?,?,?)";
        this.pstaEvent = getSession(machineNumber).prepare(insertDBEvent);

        //store entities
        String sqlObject = "CREATE TABLE IF NOT EXISTS test.object"+machineNumber+" (" +
                "timestamp bigint PRIMARY KEY," +
                "uuid uuid," +
                "name varchar)" +
                " WITH default_time_to_live = "+ttl +
                " and compression = {'sstable_compression': 'DeflateCompressor'}" +
                " and compaction = {'class':'org.apache.cassandra.db.compaction.DateTieredCompactionStrategy'};";
        getSession(machineNumber).execute(sqlObject);
        String insertDBObject = "insert into test.object"+machineNumber+"(timestamp,uuid,name) " +
                "values(?,?,?)";
        this.pstaObject = getSession(machineNumber).prepare(insertDBObject);

        String sqlNetwork = "CREATE TABLE IF NOT EXISTS test.network"+machineNumber+" (" +
                "timestamp bigint PRIMARY KEY,"+
                "subjectuuid uuid,"+
                "eventName int,"+
                "daddress int,"+
                "dport int," +
                "sport int)" +
                " WITH default_time_to_live = "+ttl +
                " and compression = {'sstable_compression': 'DeflateCompressor'}" +
                " and compaction = {'class':'org.apache.cassandra.db.compaction.DateTieredCompactionStrategy'};";
        getSession(machineNumber).execute(sqlNetwork);
        String insertDBNetwork = "insert into test.network"+machineNumber+"(timestamp,subjectuuid,eventName,daddress,dport,sport) " +
                "values(?,?,?,?,?,?)";
        this.pstaNetwork = getSession(machineNumber).prepare(insertDBNetwork);
    }

    public void insertSubjectData(List<Subject> subjectList, int machineNumber) {
        try{
            BatchStatement batchStatement = new BatchStatement();
            int i = 0;
            for(Subject subject:subjectList) {
                BoundStatement boundSta = new BoundStatement(psta);
                // you can see BatchStatement as a stl, boundSta is the class that can only be stored into batchStatement;
                // boundSta.bind means add data according to psta's format;
                boundSta.bind(subject.getUuid(), subject.getCmdLine(), subject.getStartTimestampNanos(),
                        subject.getParentSubject()==null?null:subject.getParentSubject(),
                        subject.getUsersid(), subject.getTypeNum(), subject.getVisibleWindowInfo());
                batchStatement.add(boundSta);
                ++i;
                if(i%50==0) {
                    getSession(machineNumber).execute(batchStatement);
                    batchStatement = new BatchStatement();
                }
            }
            getSession(machineNumber).execute(batchStatement);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          //  System.exit(1);
        }
    }

    public void insertEventData(List<Event> eventList, int machineNumber) {
        try {
        //store event
            BatchStatement batchStatementEvent = new BatchStatement();
            BatchStatement batchStatementObject = new BatchStatement();

            int i = 0;
            for(Event event: eventList){
                BoundStatement boundStaEvent = new BoundStatement(pstaEvent);
                boundStaEvent.bind(event.getTimestampNanos(), event.getSubjectUUID(), event.getId(), event.getTypeNum());
                batchStatementEvent.add(boundStaEvent);

                ++i;
                if(event.getNeedWritingToObjectTable()){
                    BoundStatement boundStaObject = new BoundStatement(pstaObject);
                    boundStaObject.bind(event.getTimestampNanos(), event.getId(), event.getPredicateObjectPath());
                    batchStatementObject.add(boundStaObject);
                }
                if(i%50==0) {
                    getSession(machineNumber).execute(batchStatementEvent);
                    getSession(machineNumber).execute(batchStatementObject);
                    batchStatementObject = new BatchStatement();
                    batchStatementEvent = new BatchStatement();
                }
            }
            getSession(machineNumber).execute(batchStatementEvent);
            getSession(machineNumber).execute(batchStatementObject);

        } catch (Exception e) {
            e.printStackTrace();
         //   System.exit(1);
        }
    }

    public void insertNetworkEvent(List<NetFlowObject> networkList, int machineNumber){
        try {
            //store event
            BatchStatement batchStatementEvent = new BatchStatement();

            int i = 0;
            for(NetFlowObject netFlowObject: networkList){
                BoundStatement boundStaEvent = new BoundStatement(pstaNetwork);
                boundStaEvent.bind(netFlowObject.getStartTimestampNanos(), netFlowObject.getSubjectUUID(), netFlowObject.getTypeNum(),
                        netFlowObject.getRemoteAddress(), netFlowObject.getRemotePort(), netFlowObject.getLocalPort());
                batchStatementEvent.add(boundStaEvent);

                ++i;
                if(i%50==0) {
                    getSession(machineNumber).execute(batchStatementEvent);
                    batchStatementEvent = new BatchStatement();
                }
            }
            getSession(machineNumber).execute(batchStatementEvent);
        } catch (Exception e) {
            e.printStackTrace();
         //   System.exit(1);
        }
    }

    public String eventObjectUUIDToFilename(Event event, int machineNumber){
        String filename = "";
        try{
            List<LatestObject> latestObjectList = new ArrayList<>();
            UUID uuidOfFile = event.getSubjectUUID();
            long timestamp = 0;
            String fileSearch = "select * from test.object"+ machineNumber +" where uuid=" + uuidOfFile + " allow filtering;";
            ResultSet resultSet = getSession(machineNumber).execute(fileSearch);
            Iterator<Row> rsIterator = resultSet.iterator();
            while(rsIterator.hasNext()){
                Row row = rsIterator.next();
                LatestObject latestObject = new LatestObject(row.getUUID("uuid"), row.getString("name"), row.getLong("timestamp"));
                if (timestamp < latestObject.getTimestampNanos()){
                    timestamp = latestObject.getTimestampNanos();
                    filename = latestObject.getName();
                }
                System.err.println(latestObjectList.size());
                System.err.println("object TT   available:  " + latestObject.getTimestampNanos());
                System.err.println("object NA   available:  " + latestObject.getName());
                System.err.println("object ID   available:  " + latestObject.getId());
                latestObjectList.add(latestObject);
            }
            System.err.println("list        size:   " + latestObjectList.size());
            if(latestObjectList.size() == 0){
                System.err.println("No valid filename");
            }
        }catch(Exception e){
            e.printStackTrace();
            System.exit(1);
        }
        return filename;
    }

    public Map<String, UUID> filenameToUUIDFuzzy(String filename, int machineNumber){
        List<String> possibleFilename = new ArrayList<>();
        Map<String, Long> filenameToTimestamp = new HashMap<>();
        Map<String, UUID> filenameToUUID = new HashMap<>();
        try {
            String fileSearch = "select * from test.object"+ machineNumber + ";";
            ResultSet resultSet = getSession(machineNumber).execute(fileSearch);
            Iterator<Row> rsIterator = resultSet.iterator();
            while(rsIterator.hasNext()){
                Row row = rsIterator.next();
                LatestObject latestObject = new LatestObject(row.getUUID("uuid"), row.getString("name"), row.getLong("timestamp"));
                if(latestObject.containsFilename(filename))     // object.name contains filename
                {
                    if(!possibleFilename.contains(latestObject.getName())){
                        possibleFilename.add(latestObject.getName());
                        filenameToTimestamp.put(latestObject.getName(), latestObject.getTimestampNanos());
                        filenameToUUID.put(latestObject.getName(), latestObject.getId());
                    } else if(latestObject.getTimestampNanos() > filenameToTimestamp.get(latestObject.getName())){
                        filenameToTimestamp.replace(latestObject.getName(), latestObject.getTimestampNanos());
                        filenameToUUID.replace(latestObject.getName(), latestObject.getId());
                    }
                }
            }
            System.err.println("possibleFilename    list    size:   " + possibleFilename.size());
            if(possibleFilename.size() == 0){
                System.err.println("No valid filename");
            }
        } catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
        return filenameToUUID;
    }

    public UUID filenameToUUIDExact(String filename, int machineNumber){
        UUID uuid = null;
        try {
            String fileSearch = "select * from test.object"+ machineNumber + " where name=" + filename + " allow filtering;";
            ResultSet resultSet = getSession(machineNumber).execute(fileSearch);
            Iterator<Row> rsIterator = resultSet.iterator();
            Long timestamp = new Long(0);
            while(rsIterator.hasNext()){
                Row row = rsIterator.next();
                if(row.getLong("timestamp") > timestamp){
                    timestamp = row.getLong("timestamp");
                    uuid = row.getUUID("uuid");
                }
            }
            if(uuid == null)
                throw new Exception("not recorded, how weird");
        } catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
        return uuid;
    }

    public String eventToParentprocessname(Event event, int machineNumber){
        String parentprocessUUIDName = null;
        try {
            UUID uuid = event.getSubjectUUID();
            Map<UUID, String> parentUUIDAndName = findSubject(uuid,machineNumber);  // on exact match
            System.err.println(parentUUIDAndName);
            while (parentUUIDAndName.entrySet().stream().findFirst().get().getValue() == null){
                // parent is a thread, find the parent process of this thread
                Iterator<UUID> iterator = parentUUIDAndName.keySet().iterator();
                uuid = iterator.next();
                parentUUIDAndName = findSubject(uuid,machineNumber);
                System.err.println(parentUUIDAndName);
                parentprocessUUIDName = parentUUIDAndName.entrySet().stream().findFirst().get().getValue();
            }
            // parent is a process
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return parentprocessUUIDName;
    }

    public Map<UUID, String> findSubject(UUID uuid, int machineNumber){
        Map<UUID, String> parentUUIDAndProcessname = new HashMap<>();
        try{
            String subjectSearch = "select * from test.subject"+machineNumber + " where uuid="+uuid+" allow filtering;";
            ResultSet resultSet = getSession(machineNumber).execute(subjectSearch);
            Iterator<Row> rsIterator = resultSet.iterator();
            while(rsIterator.hasNext()){
                Row row = rsIterator.next();
                parentUUIDAndProcessname.put(row.getUUID("parentuuid"), row.getString("name"));
            }
            switch (parentUUIDAndProcessname.size()){   // maybe rule out necessity?
                case 1:
                    System.err.println("one exact subject found");
                    break;
                case 0:
                    throw new Exception("no parent subject recorded");
                default:
                    throw new Exception("several parent subjects recorded");
            }
        } catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
        return parentUUIDAndProcessname;
    }

    public List<UUID> fileObjectsAccessedBySubject(UUID subjectUUID, int machineNumber){
        List<UUID> returnList=new ArrayList<>();
        try{
            String fileSearch = "select objectuuid from test.event"+machineNumber+" where subjectuuid=" +subjectUUID.toString()+" allow filtering;";
            ResultSet resultSet = getSession(machineNumber).execute(fileSearch);
            Iterator<Row> rsIterator = resultSet.iterator();
            while (rsIterator.hasNext()){
                Row row=rsIterator.next();
                returnList.add(row.getUUID("objectuuid"));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return returnList;
    }
    public void close() {
        cluster.close();
        System.out.println("closed！");
    }
}
