package edu.nu.forensic.db.DBApi;

import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.Subject;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Neo4jApi extends Thread{
    private GraphDatabaseService db = null;
    private Label subjectLabel = Label.label("Process");
    private Label fileLabel = Label.label("File");


    public Neo4jApi(String pathname){
        File DB_PATH = new File(pathname);
        if(DB_PATH.exists())System.out.println("DB exists, starting function");
        GraphDatabaseFactory factory = new GraphDatabaseFactory();
        GraphDatabaseBuilder graphDbBuilder = factory.newEmbeddedDatabaseBuilder(DB_PATH);
        //GraphDatabaseBuilder graphDbBuilder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(DB_PATH);
        graphDbBuilder.loadPropertiesFromFile("neo4j.properties");
        GraphDatabaseService db = graphDbBuilder.newGraphDatabase();
        this.db = db;
        try {
            Transaction tx = db.beginTx();
            Schema schema = db.schema();
            if (!schema.getIndexes(subjectLabel).iterator().hasNext()) {
                IndexDefinition subjectIndex = schema.indexFor(subjectLabel).on("index").create();
            }
            if (!schema.getIndexes(fileLabel).iterator().hasNext()) {
                IndexDefinition fileIndex = schema.indexFor(fileLabel).on("index").create();
            }
            tx.success();
            tx.close();
        }catch (Exception e){
            e.getMessage();
        }
    }

    public synchronized void storeSubject(List<Subject> subjectList){
        try{
            Transaction tx = db.beginTx();
            for(Subject subject: subjectList){
                Node node = db.findNode(subjectLabel,"UUID",subject.getUuid().toString());
                if(node==null){
                    node = db.createNode();
                    node.addLabel(subjectLabel);
                    node.setProperty("UUID",subject.getUuid().toString());
                }
                node.setProperty("CID", String.valueOf(subject.getCid()));
                node.setProperty("CMDLINE", subject.getCmdLine());
                Node parentNode = null;
                if((parentNode = db.findNode(subjectLabel, "UUID", subject.getParentSubjectUUID().toString()))!=null){
                    RelationshipType relationshipType = RelationshipType.withName("SUBJECT_PROCESS");
                    Relationship relationship = parentNode.createRelationshipTo(node, relationshipType);
                    relationship.setProperty("TIMESTAMP", String.valueOf(subject.getStartTimestampNanos()));
                    relationship.setProperty("OPERATION", "SUBJECT_PROCESS");
                }
            }
            tx.success();
            tx.close();
        }catch (Exception e){
            e.getMessage();
        }
    }


    public void storeEvent(List<Event> eventList){
        try{
            Transaction tx = db.beginTx();
            Map<String, Node> UUIDToNode = new HashMap<>();
            for(Event event: eventList){
                Node node = db.findNode(fileLabel, "UUID", event.getId().toString());
                Node subjectNode = db.findNode(subjectLabel, "UUID", event.getSubjectUUID().toString());
                if(subjectNode==null){
                    if(UUIDToNode.containsKey(event.getSubjectUUID().toString())) {
                        subjectNode = db.createNode();
                        subjectNode.setProperty("UUID", event.getSubjectUUID().toString());
                        subjectNode.setProperty("CID", "0");
                        subjectNode.setProperty("CMDLINE", "process");
                        UUIDToNode.put(event.getSubjectUUID().toString(), subjectNode);
                    }
                    else subjectNode = UUIDToNode.get(event.getSubjectUUID().toString());
                }
                if(node==null){
                    node = db.createNode();
                    node.addLabel(fileLabel);
                    node.setProperty("UUID",event.getId().toString());
                    node.setProperty("FILENAME", event.getPredicateObjectPath());
                }
                RelationshipType relationshipType = RelationshipType.withName(event.getType());
                Relationship relationship = null;
                if(event.getType().contains("WRITE")) relationship = subjectNode.createRelationshipTo(node, relationshipType);
                else relationship = node.createRelationshipTo(subjectNode,relationshipType);
                relationship.setProperty("TIMESTAMP", String.valueOf(event.getTimestampNanos()));
                relationship.setProperty("OPERATION", event.getType());
            }
            tx.success();
            tx.close();
        }catch (Exception e){
            e.getMessage();
        }
    }

    public void closeConnection(){
        try{
            db.shutdown();
        }catch (Exception e){
            System.err.println("error when shutting down");
        }
    }
}
