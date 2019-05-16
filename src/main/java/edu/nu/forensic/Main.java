package edu.nu.forensic;

import com.bbn.tc.schema.SchemaNotInitializedException;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.reader.AvroReader;
import edu.nu.forensic.reducer.Reducer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

//@SpringBootApplication
public class Main {

//    public static void main(String[] args) {
//        SpringApplication.run(Main.class,args);
//    }
    public static void main(String[] args) {
        String file = "C:\\Data\\ta1-marple-1-e5-bgt-2.bin.1";
        try {
            AvroReader avroReader = new AvroReader();
            avroReader.readTraceWithoutIndex(new File(file));
//          avroReader.readTrace(new File(request.getTrace()));
        } catch (IOException | SchemaNotInitializedException e) {
            e.printStackTrace();
        }
        Reducer reducer = new Reducer();
        Set<String> fileList = reducer.getFileList();
        List<Event> events = new ArrayList<>();
        //Here you should put some event in events, after this, you can call reduce function;
        events = reducer.reduce(events, fileList);
        //Here 
    }
}