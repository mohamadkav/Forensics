package edu.nu.forensic.db.CassandraApi;

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

    public connectionToCassandra(String nodeIP, String machineNum) {
        cluster = Cluster.builder().addContactPoint(nodeIP).build();
        Metadata metadata = cluster.getMetadata();
        System.out.printf("Connected to cluster: %s\n", metadata.getClusterName());
        for (Host host : metadata.getAllHosts()) {
            System.out.printf("Datatacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(), host.getRack());
        }
        setSession(cluster.connect());
        this.machineNumber = machineNum;
    }

    public void insertSubjectData(Set<Subject> subjectList) {
        String sql = "CREATE TABLE IF NOT EXISTS test.subject"+machineNumber+" (" +
                "uuid varchar PRIMARY KEY," +
                "name varchar," +
                "timestamp varchar NOT NULL," +
                "parentuuid varchar," +
                "Usersid," +
                "visibleWindow)";
        getSession().execute(sql);
        String insertDB = "insert into test.subject"+machineNumber+"(uuid ,name,timestamp,parentuuid,Usersid,visibleWindow) " +
                "values(?,?,?,?,?,?)";
        PreparedStatement psta = getSession().prepare(insertDB);

        BatchStatement batchStatement = new BatchStatement();

        try{
            int i = 0;
            for(Subject subject:subjectList) {
                BoundStatement boundSta = new BoundStatement(psta);
                boundSta.bind(subject.getUuid().toString(), subject.getCmdLine(), String.valueOf(subject.getStartTimestampNanos()),
                        subject.getParentSubject().toString(), subject.getUsersid(), subject.getVisibleWindowInfo());
                batchStatement.add(boundSta);
                ++i;
                if(i%50==0) {
                    System.out.println(i);
                    getSession().execute(batchStatement);
                    batchStatement = new BatchStatement();
                }
            }
            System.out.println("successful insertion！");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void insertEventData(Set<IoEventAfterCPR> eventList) {
        //store event
        String sqlEvent = "CREATE TABLE IF NOT EXISTS test.event"+machineNumber+" (" +
                "uuid varchar PRIMARY KEY," +
                "subjectuuid varchar," +
                "objectuuid varchar NOT NULL," +
                "eventName varchar," +
                "tid varchar," +
                "timestamp varchar)";
        getSession().execute(sqlEvent);
        String insertDBEvent = "insert into test.event"+machineNumber+"" +
                "(uuid,subjectuuid,objectuuid,eventName,tid,timestamp) " +
                "values(?,?,?,?,?,?)";
        PreparedStatement pstaEvent = getSession().prepare(insertDBEvent);
        BatchStatement batchStatementEvent = new BatchStatement();

        //store entities
        String sqlObject = "CREATE TABLE IF NOT EXISTS test.object"+machineNumber+" (" +
                "uuid varchar PRIMARY KEY," +
                "mark varchar," +
                "daddress varchar," +
                "dport varchar," +
                "saddress varchar," +
                "sport varchar," +
                "name)";
        getSession().execute(sqlObject);
        String insertDBObject = "insert into test.object"+machineNumber+"(uuid,mark,daddress,dport,saddress,sport,name) " +
                "values(?,?,?,?,?,?)";
        PreparedStatement pstaObject = getSession().prepare(insertDBObject);

        BatchStatement batchStatementObject = new BatchStatement();

        try {
            int i = 0;
            for(IoEventAfterCPR event: eventList){
                BoundStatement boundStaEvent = new BoundStatement(pstaEvent);
                boundStaEvent.bind(UUID.randomUUID().toString(), event.getSubjectUUID().toString(), event.getId().toString(),
                        event.getType(), String.valueOf(event.getThreadId()), String.valueOf(event.getStartTimestampNanos()));
                batchStatementEvent.add(boundStaEvent);

                BoundStatement boundStaObject = new BoundStatement(pstaObject);
                boundStaObject.bind(event.getId().toString(),"N",null, null, null, null, event.getPredicateObjectPath());
                ++i;
                if(i%50==0) {
                    System.out.println(i);
                    getSession().execute(batchStatementEvent);
                    getSession().execute(batchStatementObject);
                    batchStatementObject = new BatchStatement();
                    batchStatementEvent = new BatchStatement();
                }
            }
            System.out.println("successful insertion！");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void insertNetworkEvent(Set<NetFlowObject> networkList){
        //store event
        String sqlEvent = "CREATE TABLE IF NOT EXISTS test.event"+machineNumber+" (" +
                "uuid varchar PRIMARY KEY," +
                "subjectuuid varchar," +
                "objectuuid varchar NOT NULL," +
                "eventName varchar," +
                "tid varchar," +
                "timestamp varchar)";
        getSession().execute(sqlEvent);
        String insertDBEvent = "insert into test.event"+machineNumber+"" +
                "(uuid,subjectuuid,objectuuid,eventName,tid,timestamp) " +
                "values(?,?,?,?,?,?)";
        PreparedStatement pstaEvent = getSession().prepare(insertDBEvent);
        BatchStatement batchStatementEvent = new BatchStatement();

        //store entities
        String sqlObject = "CREATE TABLE IF NOT EXISTS test.object"+machineNumber+" (" +
                "uuid varchar PRIMARY KEY," +
                "mark varchar," +
                "daddress varchar," +
                "dport varchar," +
                "saddress varchar," +
                "sport varchar," +
                "name)";
        getSession().execute(sqlObject);
        String insertDBObject = "insert into test.object"+machineNumber+"(uuid,mark,daddress,dport,saddress,sport,name) " +
                "values(?,?,?,?,?,?)";
        PreparedStatement pstaObject = getSession().prepare(insertDBObject);

        BatchStatement batchStatementObject = new BatchStatement();

        try {
            int i = 0;
            for(NetFlowObject netFlowObject: networkList){
                BoundStatement boundStaEvent = new BoundStatement(pstaEvent);
                boundStaEvent.bind(UUID.randomUUID().toString(), netFlowObject.getSubjectUUID().toString(), netFlowObject.getId().toString(),
                        netFlowObject.getType(), String.valueOf(netFlowObject.getThreadId()), String.valueOf(netFlowObject.getStartTimestampNanos()));
                batchStatementEvent.add(boundStaEvent);

                BoundStatement boundStaObject = new BoundStatement(pstaObject);
                boundStaObject.bind(netFlowObject.getId().toString(),"Y",netFlowObject.getRemoteAddress(), netFlowObject.getRemotePort(),
                        netFlowObject.getLocalAddress(), netFlowObject.getLocalPort(), null);
                ++i;
                if(i%50==0) {
                    System.out.println(i);
                    getSession().execute(batchStatementEvent);
                    getSession().execute(batchStatementObject);
                    batchStatementObject = new BatchStatement();
                    batchStatementEvent = new BatchStatement();
                }
            }
            System.out.println("successful insertion！");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void close() {
        cluster.close();
        Map<Integer, Integer> count = new LinkedHashMap<>();
        System.out.println("程序正常关闭！");
    }
}
