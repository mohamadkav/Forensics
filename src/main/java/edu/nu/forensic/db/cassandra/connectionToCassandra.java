package edu.nu.forensic.db.cassandra;

import com.datastax.driver.core.*;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.NetFlowObject;
import edu.nu.forensic.db.entity.Subject;

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

    public void close() {
        cluster.close();
        System.out.println("closed！");
    }
}
