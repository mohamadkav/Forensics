package edu.nu.forensic.db.cassandra;

import com.datastax.driver.core.*;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.NetFlowObject;
import edu.nu.forensic.db.entity.Subject;
import edu.nu.forensic.db.entity.LatestObject;

import java.util.*;

public class connectionToCassandra {
    private Cluster cluster;

    private Session session;

//    private int TTL = 8640000‬;

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    private String machineNumber;

    private PreparedStatement psta;
    private PreparedStatement pstaObject;
    private PreparedStatement pstaEvent;
    private PreparedStatement pstaNetwork;

    private int ttl = 8640000;

    public connectionToCassandra(String nodeIP, String machineNum) {
        cluster = Cluster.builder().addContactPoint(nodeIP).build();    // we add this IP:contact into cassandra cluster
        setSession(cluster.connect());  // generate and initialize a new conversation and use session to record connection
        this.machineNumber = machineNum;    // machine Number used to identify this thread
        createSubjectTable();
        createEventAndObjectTable();
        // 1.create table subject+threadID\event+threadID\object+threadID\network+threadID under keyspace test
        // 2.create prepared statement for each table so as to use model conveniently
    }

    private void createSubjectTable(){
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
        getSession().execute(sql);
        String insertDB = "insert into test.subject"+machineNumber+"(uuid ,name,timestamp,parentuuid,Usersid,eventName,visibleWindow) " +
                "values(?,?,?,?,?,?,?)";
        this.psta = getSession().prepare(insertDB);
    }

    private void createEventAndObjectTable(){
        String sqlEvent = "CREATE TABLE IF NOT EXISTS test.event"+machineNumber+" (" +
                "timestamp bigint PRIMARY KEY," +
                "subjectuuid uuid," +
                "objectuuid uuid," +
                "eventName int)" +
                " WITH default_time_to_live = "+ttl +
                " and compression = {'sstable_compression': 'DeflateCompressor'}" +
                " and compaction = {'class':'org.apache.cassandra.db.compaction.DateTieredCompactionStrategy'};";
        getSession().execute(sqlEvent);
        String insertDBEvent = "insert into test.event"+machineNumber+"" +
                "(timestamp,subjectuuid,objectuuid,eventName) " +
                "values(?,?,?,?)";
        this.pstaEvent = getSession().prepare(insertDBEvent);

        //store entities
        String sqlObject = "CREATE TABLE IF NOT EXISTS test.object"+machineNumber+" (" +
                "timestamp bigint PRIMARY KEY," +
                "uuid uuid," +
                "name varchar)" +
                " WITH default_time_to_live = "+ttl +
                " and compression = {'sstable_compression': 'DeflateCompressor'}" +
                " and compaction = {'class':'org.apache.cassandra.db.compaction.DateTieredCompactionStrategy'};";
        getSession().execute(sqlObject);
        String insertDBObject = "insert into test.object"+machineNumber+"(timestamp,uuid,name) " +
                "values(?,?,?)";
        this.pstaObject = getSession().prepare(insertDBObject);

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
        getSession().execute(sqlNetwork);
        String insertDBNetwork = "insert into test.network"+machineNumber+"(timestamp,subjectuuid,eventName,daddress,dport,sport) " +
                "values(?,?,?,?,?,?)";
        this.pstaNetwork = getSession().prepare(insertDBNetwork);
    }

    public void insertSubjectData(List<Subject> subjectList) {
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
                    getSession().execute(batchStatement);
                    batchStatement = new BatchStatement();
                }
            }
            getSession().execute(batchStatement);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void insertEventData(List<Event> eventList) {
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
                    getSession().execute(batchStatementEvent);
                    getSession().execute(batchStatementObject);
                    batchStatementObject = new BatchStatement();
                    batchStatementEvent = new BatchStatement();
                }
            }
            getSession().execute(batchStatementEvent);
            getSession().execute(batchStatementObject);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void insertNetworkEvent(List<NetFlowObject> networkList){
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
                    getSession().execute(batchStatementEvent);
                    batchStatementEvent = new BatchStatement();
                }
            }
            getSession().execute(batchStatementEvent);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public String eventObjectUUIDToFilename(Event event){
        String filename = new String();
        try{
            List<LatestObject> latestObjectList = new ArrayList<>();
            UUID uuidOfFile = event.getSubjectUUID();
            long timestamp = 0;
            String fileSearch = "select * from test.object"+ machineNumber +" where uuid=" + uuidOfFile + " allow filtering;";
            ResultSet resultSet = getSession().execute(fileSearch);
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

    public Map<String, UUID> filenameToUUID(String filename){
        List<String> possibleFilename = new ArrayList<>();
        Map<String, Long> filenameToTimestamp = new HashMap<>();
        Map<String, UUID> filenameToUUID = new HashMap<>();
        try {
            String fileSearch = "select * from test.object"+ machineNumber + ";";
            ResultSet resultSet = getSession().execute(fileSearch);
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
//            Iterator iterator = possibleFilename.iterator();
//            while(iterator.hasNext()){
//                String thisFilename = iterator.next().toString();
//                System.err.println(thisFilename);
//                System.err.println(filenameToTimestamp.get(thisFilename));
//                System.err.println(filenameToUUID.get(thisFilename));
//            }
        } catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
        return filenameToUUID;
    }

    public void close() {
        cluster.close();
        System.out.println("closed！");
    }
}
