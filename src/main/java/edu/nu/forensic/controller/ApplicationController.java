package edu.nu.forensic.controller;


import com.bbn.tc.schema.SchemaNotInitializedException;
import edu.nu.forensic.dto.ReadTraceRequest;
import edu.nu.forensic.reader.AvroReader;
import edu.nu.forensic.reader.KafkaReader;
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

    @RequestMapping(method = RequestMethod.POST, value = "/read")
    public String read(@RequestBody ReadTraceRequest request){
        System.out.println(request.getTrace());
        if(request.getTrace()!=null && new File(request.getTrace()).exists())
            try {
                avroReader.readTrace(new File(request.getTrace()));
            }catch (IOException|SchemaNotInitializedException e){
                e.printStackTrace();
                return "ERROR";
            }
        else
            kafkaReader.readTrace();
        return "OK";
    }
}
