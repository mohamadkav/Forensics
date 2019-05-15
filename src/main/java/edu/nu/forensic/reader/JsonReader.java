package edu.nu.forensic.reader;

import com.bbn.tc.schema.SchemaNotInitializedException;
import com.bbn.tc.schema.avro.cdm20.Event;
import com.bbn.tc.schema.avro.cdm20.Subject;
import com.bbn.tc.schema.avro.cdm20.TCCDMDatum;
import com.bbn.tc.schema.avro.cdm20.UnitDependency;
import com.bbn.tc.schema.serialization.AvroGenericDeserializer;
import com.google.gson.Gson;
import edu.nu.forensic.JsonFormat.ETWJsonFormat;
import edu.nu.forensic.db.DBApi.PostGreSqlApi;
import edu.nu.forensic.util.RecordConverter;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.crypto.dsig.spec.ExcC14NParameterSpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class JsonReader {

    @Autowired

    public void readTrace(File source) throws IOException, SchemaNotInitializedException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(source));
        int i=0;
        List<String> registryUUIDs = new LinkedList<>();
        List<edu.nu.forensic.db.entity.Subject> subjectList= new ArrayList<>();
        List<edu.nu.forensic.db.entity.Event> eventList= new ArrayList<>();
        Map<UUID,UUID> ThreadIDToProcessID = new HashMap<>();
        Map<UUID,UUID> UnitToDependency = new HashMap<>();

        //init db
//        Neo4jApi neo4jApi = new Neo4jApi("C:\\Data\\neo4j-community-3.5.3\\data\\databases\\graph.db");
        PostGreSqlApi postGreSqlApi = new PostGreSqlApi();
        String line = null;
        while((line=bufferedReader.readLine())!=null){
            Gson gson = new Gson();
            ETWJsonFormat etwJson = gson.fromJson(line, ETWJsonFormat.class);
//            GenericContainer data= (GenericContainer)avroGenericDeserializer.deserializeNextRecordFromFile();
            try {
                if (i % 10000 == 0) System.out.println(i);
                i++;
            }catch (Exception e){

            }

        }
        postGreSqlApi.storeSubject(subjectList);
        postGreSqlApi.storeEvent(eventList);
        Date day=new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(df.format(day));
    }
}