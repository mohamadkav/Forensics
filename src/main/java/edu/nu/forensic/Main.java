package edu.nu.forensic;

import com.bbn.tc.schema.SchemaNotInitializedException;
import edu.nu.forensic.reader.AvroReader;
import edu.nu.forensic.reducer.Reducer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.File;
import java.io.IOException;

//@SpringBootApplication
public class Main {

//    public static void main(String[] args) {
//        SpringApplication.run(Main.class,args);
//    }
    public static void main(String[] args){
        String file = "C:\\Data\\ta1-marple-1-e5-bgt-2.bin.1";
        try {
            AvroReader avroReader = new AvroReader();
            avroReader.readTraceWithoutIndex(new File(file));
//          avroReader.readTrace(new File(request.getTrace()));
        }catch (IOException | SchemaNotInitializedException e) {
            e.printStackTrace();
        }
        Reducer reducer = new Reducer();
        reducer.reduce(new File(file));
    }
}