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

    public connectionToCassandra(String nodeIP, String machineNum) {
        cluster = Cluster.builder().addContactPoint(nodeIP).build();
        setSession(cluster.connect());
        this.machineNumber = machineNum;
        createSubjectTable();
        createEventAndObjectTable();
    }

    private void createSubjectTable(){
        String sql = "CREATE TABLE IF NOT EXISTS test.subject"+machineNumber+" (" +
                "uuid varchar PRIMARY KEY," +
                "name varchar," +
                "timestamp varchar," +
                "parentuuid varchar," +
                "Usersid varchar," +
                "visibleWindow varchar)";
        getSession().execute(sql);
        String insertDB = "insert into test.subject"+machineNumber+"(uuid ,name,timestamp,parentuuid,Usersid,visibleWindow) " +
                "values(?,?,?,?,?,?)";
        this.psta = getSession().prepare(insertDB);
    }

    private void createEventAndObjectTable(){
        String sqlEvent = "CREATE TABLE IF NOT EXISTS test.event"+machineNumber+" (" +
                "uuid varchar PRIMARY KEY," +
                "subjectuuid varchar," +
                "objectuuid varchar," +
                "eventName varchar," +
                "tid varchar," +
                "timestamp varchar)";
        getSession().execute(sqlEvent);
        String insertDBEvent = "insert into test.event"+machineNumber+"" +
                "(uuid,subjectuuid,objectuuid,eventName,tid,timestamp) " +
                "values(?,?,?,?,?,?)";
        this.pstaEvent = getSession().prepare(insertDBEvent);


        //store entities
        String sqlObject = "CREATE TABLE IF NOT EXISTS test.object"+machineNumber+" (" +
                "uuid varchar PRIMARY KEY," +
                "mark varchar," +
                "daddress varchar," +
                "dport varchar," +
                "saddress varchar," +
                "sport varchar," +
                "name varchar)";
        getSession().execute(sqlObject);
        String insertDBObject = "insert into test.object"+machineNumber+"(uuid,mark,daddress,dport,saddress,sport,name) " +
                "values(?,?,?,?,?,?,?)";
        this.pstaObject = getSession().prepare(insertDBObject);
    }

    public void insertSubjectData(Set<Subject> subjectList) {
        try{
            BatchStatement batchStatement = new BatchStatement();
            int i = 0;
            for(Subject subject:subjectList) {
                BoundStatement boundSta = new BoundStatement(psta);
                boundSta.bind(subject.getUuid().toString(), subject.getCmdLine(), String.valueOf(subject.getStartTimestampNanos()),
                        subject.getParentSubject()==null?null:subject.getParentSubject().toString(),
                        subject.getUsersid(), subject.getVisibleWindowInfo());
                batchStatement.add(boundSta);
                ++i;
                if(i%50==0) {
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
        try {
        //store event
            BatchStatement batchStatementEvent = new BatchStatement();
            BatchStatement batchStatementObject = new BatchStatement();
            int i = 0;
            for(IoEventAfterCPR event: eventList){
                BoundStatement boundStaEvent = new BoundStatement(pstaEvent);
                boundStaEvent.bind(UUID.randomUUID().toString(), event.getSubjectUUID().toString(), event.getId().toString(),
                        event.getType(), String.valueOf(event.getThreadId()), String.valueOf(event.getStartTimestampNanos()));
                batchStatementEvent.add(boundStaEvent);

                BoundStatement boundStaObject = new BoundStatement(pstaObject);
                boundStaObject.bind(event.getId().toString(),"N","1", "2", "3", "4", event.getPredicateObjectPath());
                batchStatementObject.add(boundStaObject);
                ++i;
                if(i%50==0) {
                    getSession().execute(batchStatementEvent);
                    getSession().execute(batchStatementObject);
                    batchStatementObject = new BatchStatement();
                    batchStatementEvent = new BatchStatement();
                }
            }
            System.out.println("successful insertion！");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void insertNetworkEvent(Set<NetFlowObject> networkList){
        try {
            //store event
            BatchStatement batchStatementEvent = new BatchStatement();

            //store entities
            BatchStatement batchStatementObject = new BatchStatement();

            int i = 0;
            for(NetFlowObject netFlowObject: networkList){
                BoundStatement boundStaEvent = new BoundStatement(pstaEvent);
                boundStaEvent.bind(UUID.randomUUID().toString(), netFlowObject.getSubjectUUID().toString(), netFlowObject.getId().toString(),
                        netFlowObject.getType(), String.valueOf(netFlowObject.getThreadId()), String.valueOf(netFlowObject.getStartTimestampNanos()));
                batchStatementEvent.add(boundStaEvent);

                BoundStatement boundStaObject = new BoundStatement(pstaObject);
                boundStaObject.bind(netFlowObject.getId().toString(),"Y",netFlowObject.getRemoteAddress(), String.valueOf(netFlowObject.getRemotePort()),
                        netFlowObject.getLocalAddress(), String.valueOf(netFlowObject.getLocalPort()), "network");
                batchStatementObject.add(boundStaObject);
                ++i;
                if(i%50==0) {
                    getSession().execute(batchStatementEvent);
                    getSession().execute(batchStatementObject);
                    batchStatementObject = new BatchStatement();
                    batchStatementEvent = new BatchStatement();
                }
            }
            System.out.println("successful insertion！");
        } catch (Exception e) {

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
