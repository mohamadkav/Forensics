package edu.nu.forensic;


import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class Main {
    private static Cluster cluster;
    private static List<Session> sessions=new ArrayList<>();


    public static void main(String[] args) {
        List<UUID> result=fileObjectsAccessedBySubject(UUID.fromString("7e7cb2ba-3a6e-4c86-a9a8-e4eed5490fdd"),0);
        for(UUID s:result)
            System.out.println(s);
    }
    public static List<UUID> fileObjectsAccessedBySubject(UUID subjectUUID, int machineNumber){
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
    static{
        cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
        cluster.getConfiguration().getSocketOptions().setReadTimeoutMillis(300000);
        for(int i=0;i< GlobalConfig.NUM_SERVERS/GlobalConfig.NUM_SERVERS_PER_CONNECTION;i++){
            sessions.add(cluster.connect());
            System.out.println("Session "+i+" added");
        }
    }
    public static Session getSession(int machineNum){
        return sessions.get(machineNum/GlobalConfig.NUM_SERVERS_PER_CONNECTION);
    }
}