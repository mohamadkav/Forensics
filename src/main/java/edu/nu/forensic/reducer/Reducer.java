package edu.nu.forensic.reducer;


import com.bbn.tc.schema.avro.cdm19.SubjectType;
import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.Subject;
import edu.nu.forensic.db.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;

import static java.util.stream.Collectors.toMap;

@Component
public class Reducer {
    @Autowired
    EventRepository eventRepository;

    private HashMap<String, HashSet<Integer>> fileToProcessesWhichHaveAccessedIt=new HashMap<>();
    private HashSet<Integer> test=new HashSet<>();
    public void reduce(){
        //Extract all file reads and writes
        Set<Event> allFileAccesses=eventRepository.findByNamesEqualsOrNamesEqualsOrderByTimestampNanosAsc("FileIoRead","FileIoWrite");
        HashMap<Integer,List<String>> processIdToFiles=new HashMap<>();

        //Begin extract Read-Only files
        Set<Event> fileIoWrites=eventRepository.findByNamesEquals("FileIoWrite");
        Set<String> fileIoWriteFileNames=new HashSet<>();
        for(Event event:fileIoWrites)
            fileIoWriteFileNames.add(event.getPredicateObjectPath());
        Set<Event> results=new HashSet<>();
        for(Event event:allFileAccesses){
            if(!fileIoWriteFileNames.contains(event.getPredicateObjectPath()))
                results.add(event);
        }
        allFileAccesses=results;
        ////////////////// End calculating read-only files


        //In this section, we find the process which has accessed a file, Generating FAP
        for(Event event:allFileAccesses){
            Subject process=event.getSubject();
            while(!process.getType().equals(SubjectType.SUBJECT_PROCESS.name())){
                process=process.getParentSubject();
                if(process==null)
                    throw new RuntimeException("Dude; the process did not have a parent. No can do.");
            }
            //We build the unsorted FAP but we use fileToProcessesWhichHaveAccessedIt to get the ranked table
            if(processIdToFiles.containsKey(process.getCid())){
                if(!processIdToFiles.get(process.getCid()).contains(event.getPredicateObjectPath()))
                    processIdToFiles.get(process.getCid()).add(event.getPredicateObjectPath());
            }
            else {
                List<String> newVal=new ArrayList<String>(){{add(event.getPredicateObjectPath());}};
                processIdToFiles.put(process.getCid(),newVal);
            }
            ///////////End generating the unsorted FAP
            if(!fileToProcessesWhichHaveAccessedIt.containsKey(event.getPredicateObjectPath())){
                HashSet<Integer> newVal=new HashSet<>();
                newVal.add(process.getCid());
                fileToProcessesWhichHaveAccessedIt.put(event.getPredicateObjectPath(),newVal);
            }
            else {
                fileToProcessesWhichHaveAccessedIt.get(event.getPredicateObjectPath()).add(process.getCid());
            }
        }
        /////////////////End generating FAP

        //Sort them so we'll have the ranked FAP to build the tree
        fileToProcessesWhichHaveAccessedIt = fileToProcessesWhichHaveAccessedIt
                .entrySet()
                .stream()
                .sorted((Comparator<Map.Entry<String, HashSet<Integer>>> & Serializable)
                        (c1, c2) -> Integer.compare(c2.getValue().size(), c1.getValue().size()))
                .collect(
                        toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                LinkedHashMap::new));
        ////////////End sorting

        //Building FP tree
        Node root=new Node(null);
        for(Integer pid:processIdToFiles.keySet()){
            List<String> files=processIdToFiles.get(pid);
            files.sort(((o1, o2) -> fileToProcessesWhichHaveAccessedIt.get(o2).size() - fileToProcessesWhichHaveAccessedIt.get(o1).size()));
            root.insert(files,root);
        }
        /////////Done building FP tree
        System.out.println(fileToProcessesWhichHaveAccessedIt.size());
    }
}
