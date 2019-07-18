package edu.nu.forensic.db.cassandra;

import com.datastax.driver.core.*;
import edu.nu.forensic.db.entity.IoEventAfterCPR;
import edu.nu.forensic.db.entity.NetFlowObject;
import edu.nu.forensic.db.entity.Subject;

import java.util.*;

public class connectionToCassandra {
    private Cluster cluster;

    private Session session;

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

    public connectionToCassandra(String nodeIP, String machineNum) {
        cluster = Cluster.builder().addContactPoint(nodeIP).build();
        setSession(cluster.connect());
        this.machineNumber = machineNum;
        createSubjectTable();
        createEventAndObjectTable();
    }

    private void createSubjectTable(){
        String sql = "CREATE TABLE IF NOT EXISTS test.subject"+machineNumber+" (" +
                "uuid uuid PRIMARY KEY," +
                "name varchar," +
                "timestamp bigint," +
                "parentuuid uuid," +
                "Usersid varchar," +
                "visibleWindow boolean)";
        getSession().execute(sql);
        String insertDB = "insert into test.subject"+machineNumber+"(uuid ,name,timestamp,parentuuid,Usersid,visibleWindow) " +
                "values(?,?,?,?,?,?)";
        this.psta = getSession().prepare(insertDB);
    }

    private void createEventAndObjectTable(){
        String sqlEvent = "CREATE TABLE IF NOT EXISTS test.event"+machineNumber+" (" +
                "timestamp bigint PRIMARY KEY," +
                "subjectuuid uuid," +
                "objectuuid uuid," +
                "eventName varchar," +
                "tid int)";
        getSession().execute(sqlEvent);
        String insertDBEvent = "insert into test.event"+machineNumber+"" +
                "(timestamp,subjectuuid,objectuuid,eventName,tid) " +
                "values(?,?,?,?,?)";
        this.pstaEvent = getSession().prepare(insertDBEvent);


        //store entities
        String sqlObject = "CREATE TABLE IF NOT EXISTS test.object"+machineNumber+" (" +
                "timestamp bigint PRIMARY KEY," +
                "uuid uuid," +
                "name varchar)";
        getSession().execute(sqlObject);
        String insertDBObject = "insert into test.object"+machineNumber+"(timestamp,uuid,name) " +
                "values(?,?,?)";
        this.pstaObject = getSession().prepare(insertDBObject);

        String sqlNetwork = "CREATE TABLE IF NOT EXISTS test.network"+machineNumber+" (" +
                "timestamp bigint PRIMARY KEY,"+
                "subjectuuid uuid,"+
                "eventName varchar,"+
                "tid int,"+
                "daddress varchar," +
                "dport int," +
                "saddress varchar," +
                "sport int)";
        getSession().execute(sqlNetwork);
        String insertDBNetwork = "insert into test.network"+machineNumber+"(timestamp,subjectuuid,eventName,tid,daddress,dport,saddress,sport) " +
                "values(?,?,?,?,?,?,?,?)";
        this.pstaNetwork = getSession().prepare(insertDBNetwork);
    }

    public void insertSubjectData(Set<Subject> subjectList) {
        try{
            BatchStatement batchStatement = new BatchStatement();
            int i = 0;
            for(Subject subject:subjectList) {
                BoundStatement boundSta = new BoundStatement(psta);
                // you can see BatchStatement as a stl, boundSta is the class that can only be stored into batchStatement;
                // boundSta.bind means add data according to psta's format;
                boundSta.bind(subject.getUuid(), subject.getCmdLine(), subject.getStartTimestampNanos(),
                        subject.getParentSubject()==null?null:subject.getParentSubject(),
                        subject.getUsersid(), subject.getVisibleWindowInfo());
                batchStatement.add(boundSta);
                ++i;
                if(i%50==0) {
                    getSession().execute(batchStatement);
                    batchStatement = new BatchStatement();
                }
            }
            getSession().execute(batchStatement);
            System.out.println("successful insertion！");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void insertEventData(Set<IoEventAfterCPR> eventList) {
        try {
        //store event
            BatchStatement batchStatementEvent = new BatchStatement();
            BatchStatement batchStatementObject = new BatchStatement();

            int i = 0;
            for(IoEventAfterCPR event: eventList){
                BoundStatement boundStaEvent = new BoundStatement(pstaEvent);
                boundStaEvent.bind(event.getStartTimestampNanos(), event.getSubjectUUID(), event.getId(), event.getType(), event.getThreadId());
                batchStatementEvent.add(boundStaEvent);

                ++i;
                if(event.getNeedToWrite()){
                    BoundStatement boundStaObject = new BoundStatement(pstaObject);
                    boundStaObject.bind(event.getStartTimestampNanos(), event.getId(), event.getPredicateObjectPath());
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
            System.out.println("event successful insertion！");

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void insertNetworkEvent(Set<NetFlowObject> networkList){
        try {
            //store event
            BatchStatement batchStatementEvent = new BatchStatement();

            int i = 0;
            for(NetFlowObject netFlowObject: networkList){
                BoundStatement boundStaEvent = new BoundStatement(pstaNetwork);
                boundStaEvent.bind(netFlowObject.getStartTimestampNanos(), netFlowObject.getSubjectUUID(), netFlowObject.getType(),
                        netFlowObject.getThreadId(), netFlowObject.getRemoteAddress(), netFlowObject.getRemotePort(),
                        netFlowObject.getLocalAddress(), netFlowObject.getLocalPort());
                batchStatementEvent.add(boundStaEvent);

                ++i;
                if(i%50==0) {
                    getSession().execute(batchStatementEvent);
                    batchStatementEvent = new BatchStatement();
                }
            }
            getSession().execute(batchStatementEvent);
            System.out.println("network events successful insertion！");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void close() {
        cluster.close();
        Map<Integer, Integer> count = new LinkedHashMap<>();
        System.out.println("closed！");
    }
}
