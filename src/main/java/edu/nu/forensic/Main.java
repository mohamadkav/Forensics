package edu.nu.forensic;

import com.bbn.tc.schema.SchemaNotInitializedException;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.reader.AvroReader;
import edu.nu.forensic.reader.ReadJsonToDB;
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
    //this is json test section
    public static void main(String[] args) {
        String file = "C:\\Data\\47g.out";
//        try {
//            ReadJsonToDB readJsonToDB = new ReadJsonToDB();
//            readJsonToDB.readEvent(new File(file));
////          avroReader.readTrace(new File(request.getTrace()));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        Reducer reducer = new Reducer();
        reducer.JsonReduce(new File(file));
        //Here is the reduction result
    }
}