package edu.nu.forensic;

import com.bbn.tc.schema.SchemaNotInitializedException;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.reader.AvroReader;
import edu.nu.forensic.reader.ReadJsonToDB;
import edu.nu.forensic.reducer.Reducer;

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
    public static void main(String[] args) throws IOException{
        String file = "C:\\Data\\2019-05-10-15-20-50.out";
//        System.out.println(file+" "+args[0]);
//        try {
//            ReadJsonToDB readJsonToDB = new ReadJsonToDB();
//            readJsonToDB.readEvent(new File(file), args[0]);
////          avroReader.readTrace(new File(request.getTrace()));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        Reducer reducer = new Reducer();
        reducer.JsonReduce(new File(file), args[0]);

//        for(int i=0;i<2;i++){
//            String execution = "java -jar C:\\NodeMerge\\NodeMerge\\Forensics\\experiment\\gs-accessing-data-jpa-0.1.0.jar "+i+" " +
//                    ">C:\\NodeMerge\\NodeMerge\\Forensics\\experiment\\temp"+i+".txt &";
//            System.out.println(execution);
//            Runtime.getRuntime().exec("C:/Windows/System32/cmd.exe /k start "+execution);
//        }
    }
}