package edu.nu.forensic.reducer;


import com.bbn.tc.schema.avro.cdm19.SubjectType;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.Subject;
import edu.nu.forensic.db.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static edu.nu.forensic.reducer.FPGrowth.findFrequentItemsetWithSuffix;
import static edu.nu.forensic.reducer.FSA.buildFSA;
import static edu.nu.forensic.reducer.StatementRoot.printFSA;
import static java.util.stream.Collectors.toMap;


@Component
public class Reducer {
    @Autowired
    EventRepository eventRepository;

    public void reduce(){
    }
}