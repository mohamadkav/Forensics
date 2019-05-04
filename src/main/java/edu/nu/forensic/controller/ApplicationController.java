package edu.nu.forensic.controller;


import com.bbn.tc.schema.SchemaNotInitializedException;
import edu.nu.forensic.dto.ReadTraceRequest;
import edu.nu.forensic.reader.AvroReader;
import edu.nu.forensic.reader.KafkaReader;
import edu.nu.forensic.reducer.Reducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("command")
public class ApplicationController {

    @Autowired
    AvroReader avroReader;

    @Autowired
    KafkaReader kafkaReader;

    @Autowired
    Reducer reducer;


    @RequestMapping(method = RequestMethod.POST, value = "/read")
    public String read(@RequestBody ReadTraceRequest request){
        System.out.println(request.getTrace());
        if(request.getTrace()!=null && new File(request.getTrace()).exists())
            try {
                avroReader.readTraceWithoutIndex(new File(request.getTrace()));
//                avroReader.readTrace(new File(request.getTrace()));
            }catch (IOException|SchemaNotInitializedException e){
                e.printStackTrace();
                return "ERROR";
            }
        else
            kafkaReader.readTrace();
        return "OK";
    }

    @RequestMapping(method = RequestMethod.GET, value = "/reduce")
    public String reduce(@RequestBody ReadTraceRequest request){
        if(request.getTrace()!=null && new File(request.getTrace()).exists()){
            try{
                reducer.reduce(new File(request.getTrace()));
            }catch (Exception e){
                e.printStackTrace();
                return "ERROR";
            }
        }
        else return "File Not Existed";
        return "OK";
    }
}
