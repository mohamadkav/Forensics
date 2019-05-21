package edu.nu.forensic.reducer;

import com.bbn.tc.schema.avro.cdm20.TCCDMDatum;
import com.google.gson.Gson;
import edu.nu.forensic.JsonFormat.ETWEvent;
import edu.nu.forensic.db.DBApi.PostGreSqlApi;

import java.io.*;
import java.util.*;

import edu.nu.forensic.db.entity.Event;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.springframework.stereotype.Component;

import static edu.nu.forensic.reducer.FPGrowth.findFrequentItemsetWithSuffix;


@Component
public class Reducer {

    //    private HashMap<String, HashSet<Integer>> fileToProcessesWhichHaveAccessedIt=new HashMap<>();
    private HashSet<Integer> test = new HashSet<>();

    public Set<String> getFileList() {
        PostGreSqlApi postGreSqlApi = new PostGreSqlApi();
        //Extract all file reads and writes
        Map<String, Map<String, Integer>> processIdToFileFrequences = postGreSqlApi.getProcessToFileFrequences();
        //Building FP tree
        Node root = new Node(null);
        for (String process : processIdToFileFrequences.keySet()) {
            Node head = new Node(null);
            Map<String, Integer> fileFrequences = processIdToFileFrequences.get(process);
            if (fileFrequences.size() != 0) {
                for (Node temp : root.getChildren()) {
                    if (temp.getFileName().equals(fileFrequences.keySet().iterator().next())) {
                        head = temp;
                        break;
                    }
                }
                root.insert(fileFrequences, head);
                root.addChild(head);
            }
        }
        /////////Done building FP tree
        System.out.println(processIdToFileFrequences.size());

        for (Node node : root.getChildren()) {
            Queue<Node> q = new LinkedList<>();
            q.add(node);
            while (q.size() != 0) {
                Node temp = q.poll();
                q.addAll(temp.getChildren());
            }
        }

        Set<List<String>> CFAP = findFrequentItemsetWithSuffix(root, 0);
        Set<String> filelists = new HashSet<>();

        System.out.println("frequent scequence");
        for (List<String> it : CFAP) {
            System.out.println(it.toString());
            filelists.addAll(it);
        }
        postGreSqlApi.closeConnection();
        return filelists;
    }

    public List<Event> reduce(List<Event> events, Set<String> fileslists){
        try {
            List<String> judgeProcessId = new ArrayList<>();
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("temp.txt"));
            List<Event> result = new ArrayList<>();
            for (Event event : events) {
                try{
                    if(event.getType().contains("EVENT_READ")){
                        if(fileslists.contains(event.getNames())){
                            if(!judgeProcessId.contains(event.getSubjectUUID().toString())){
                                judgeProcessId.add(event.getSubjectUUID().toString());
                                event.setNames("Init Process");
                                result.add(event);
                            }
                        }
                        else result.add(event);
                    }
                    else result.add(event);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public void JsonReduce(File source){
        try{
            BufferedReader bufferedReader = new BufferedReader(new FileReader(source));
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("temp.txt"));
            Set<String> filelists = getFileList();
            List<String> judgeprocessID = new ArrayList<>();
            String line = null;
            Gson gson = new Gson();
            while((line = bufferedReader.readLine())!=null) {
                try {
                    ETWEvent etwEvent = gson.fromJson(line, ETWEvent.class);
                    if (line.contains("FileIoRead")) {
                        if (filelists.contains(etwEvent.arguments.FileName)) {
                            if (!judgeprocessID.contains(String.valueOf(etwEvent.processID))) {
                                judgeprocessID.add(String.valueOf(etwEvent.processID));
                                line.replace(etwEvent.arguments.FileName, "Init Process");
                                bufferedWriter.append(line + "\r\n");
                            }
                        } else bufferedWriter.append(line + "\r\n");
                    } else bufferedWriter.append(line + "\r\n");
                }catch (Exception e){
                    System.out.println(line);
                    e.printStackTrace();
                }
            }
            bufferedReader.close();
            bufferedWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void reduce(File source){
        Set<String> filelists = getFileList();

        try{
            int i=0;
            List<String> judgeprocessID = new ArrayList<>();
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("temp.txt"));
            DatumReader<TCCDMDatum> reader = new SpecificDatumReader<>(TCCDMDatum.class);

            DataFileReader<TCCDMDatum> dataFileReader = new DataFileReader<>(source, reader);
            TCCDMDatum CDMdatum = null;
            while (dataFileReader.hasNext()) {
                CDMdatum = dataFileReader.next();

                if(CDMdatum==null)
                    break;
//                bufferedWriter.append(CDMdatum.getDatum().toString()+"\r\n");
                try {
                    if (i % 10000 == 0) System.out.println(i);
                    i++;
                    if (CDMdatum.getDatum() instanceof Event) {
                        if (((Event) CDMdatum.getDatum()).getType().toString().contains("EVENT_READ")) {
                            if (filelists.contains(((Event) CDMdatum.getDatum()).getPredicateObjectPath().toString())){
                                if(!judgeprocessID.contains(((Event) CDMdatum.getDatum()).getSubject().toString())) {
                                    judgeprocessID.add(((Event) CDMdatum.getDatum()).getSubject().toString());
                                    String temp = CDMdatum.getDatum().toString();
                                    temp = temp.replace(((Event) CDMdatum.getDatum()).getPredicateObjectPath().toString(), "Initial process");
                                    bufferedWriter.append(temp + "\r\n");
                                }
                            } else bufferedWriter.append(CDMdatum.getDatum().toString() + "\r\n");
                        }
                        else bufferedWriter.append(CDMdatum.getDatum().toString()+"\r\n");
                    }
                    else bufferedWriter.append(CDMdatum.getDatum().toString()+"\r\n");
                } catch (Exception e) {
                    System.err.println("Darn! We have an unknown bug over: ");
//                System.err.println(CDMdatum);
                    e.printStackTrace();
                }
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}


