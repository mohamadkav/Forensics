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



@Component
public class Reducer {

    //    private HashMap<String, HashSet<Integer>> fileToProcessesWhichHaveAccessedIt=new HashMap<>();

    //I remove FP tree because I think it is slow.
    public Set<String> getFileList(String machineNum) {
        PostGreSqlApi postGreSqlApi = new PostGreSqlApi(machineNum);
        //Extract all file reads and writes
        Map<String, Map<String, Integer>> processIdToFileFrequences = postGreSqlApi.getProcessToFileFrequences();
        //Building FP tree
//        Node root = new Node(null);
//        for (String process : processIdToFileFrequences.keySet()) {
//            Node head = new Node(null);
//            Map<String, Integer> fileFrequences = processIdToFileFrequences.get(process);
//            if (fileFrequences.size() != 0) {
//                for (Node temp : root.getChildren()) {
//                    if (temp.getFileName().equals(fileFrequences.keySet().iterator().next())) {
//                        head = temp;
//                        break;
//                    }
//                }
//                root.insert(fileFrequences, head);
//                root.addChild(head);
//            }
//        }
//        /////////Done building FP tree
//        System.out.println(processIdToFileFrequences.size());
//
//        for (Node node : root.getChildren()) {
//            Queue<Node> q = new LinkedList<>();
//            q.add(node);
//            while (q.size() != 0) {
//                Node temp = q.poll();
//                q.addAll(temp.getChildren());
//            }
//        }
//
//        Set<List<String>> CFAP = findFrequentItemsetWithSuffix(root, 0);
        Set<String> filelists = new HashSet<>();

//        System.out.println("frequent scequence");
//        for (List<String> it : CFAP) {
//            System.out.println(it.toString());
//            filelists.addAll(it);
//        }
        for(String it:processIdToFileFrequences.keySet()){
            for(String its:processIdToFileFrequences.get(it).keySet()){
                filelists.add(its);
                System.out.println(its);
            }
        }
        System.out.println(filelists.size());
        postGreSqlApi.closeConnection();
        return filelists;
    }

    //reduce event, input: filelists and event, output: true means this event should be reduced
    //judgeProcessId should be inited as a empty list, you can get fileslists from the function "getFileList"
    //bufferedWriter is used to write the data that are reduced by node merge
    //if you come across some bugs, please confirm these: 1. the event.Names stores the file name;
    //2. event.Type stores the event type

    public boolean reduce(Event event, Set<String> fileslists, List<String> judgeProcessId, BufferedWriter bufferedWriter){
        try {
            try{
                if(event.getType().contains("FileIoRead")) {
                    if (fileslists.contains(event.getNames())) {
                        if (!judgeProcessId.contains(event.getSubjectUUID())) {
                            judgeProcessId.add(event.getSubjectUUID());
                            event.setNames("Init Process");
                            return true;
                        } else bufferedWriter.append(event.getNames()+"\r\n");
                    }
                }
                }catch (Exception e){
                    e.printStackTrace();
                }
            return false;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

//    public void JsonReduce(File source){
//        try{
//            BufferedReader bufferedReader = new BufferedReader(new FileReader(source));
////            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("temp.out"));
//            Set<String> filelists = getFileList();
//            List<String> judgeprocessID = new ArrayList<>();
//            String line = null;
//            String replacement = "Init Process";
//            Gson gson = new Gson();
//            int i = 0;
//            int j = 0;
//            Map<String, Integer> k = new HashMap<>();
//            while((line = bufferedReader.readLine())!=null) {
//                try {
//                    ETWEvent etwEvent = gson.fromJson(line, ETWEvent.class);
//                    if(!k.keySet().contains(etwEvent.EventName)){
//                        k.put(etwEvent.EventName, 1);
//                        System.out.println(line);
//                    }
//                    else {
//                        Integer temp = k.get(etwEvent.EventName);
//                        ++temp;
//                        k.put(etwEvent.EventName,temp);
//                    }
//                    if (line.contains("FileIoWrite")) {
//                        String target = etwEvent.arguments.FileName;
//                        if (filelists.contains(target)) {
//                            i++;
//                            target = target.replaceAll("\\\\", "\\\\\\\\");
//                            if (!judgeprocessID.contains(String.valueOf(etwEvent.processID))) {
//                                j++;
//                                judgeprocessID.add(String.valueOf(etwEvent.processID));
//                                String newLine = line.replace(target, replacement);
////                                bufferedWriter.append(newLine + "\r\n");
//                            }
//                        }
////                        else bufferedWriter.append(line + "\r\n");
//                    }
////                    else bufferedWriter.append(line + "\r\n");
//                    }catch (Exception e){
//                    System.out.println(line);
//                    e.printStackTrace();
//                }
//            }
//            bufferedReader.close();
////            bufferedWriter.close();
//            for(String it:k.keySet()){
//                System.out.println(it+" "+k.get(it));
//            }
//            System.out.println(i+" "+j);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//    }


    //Json file
    public void JsonReduce(File source, String machineNum){
        try{
            BufferedReader bufferedReader = new BufferedReader(new FileReader(source));
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("C:\\Data\\reduce.out"));
            Set<String> filelists = getFileList(machineNum);
            List<String> judgeprocessID = new ArrayList<>();
            String line;
            String replacement = "Init Process";
            Gson gson = new Gson();
            int i = 0;
            int j = 0;
            while((line = bufferedReader.readLine())!=null) {
                try {
                    if (line.contains("FileIoRead")) {
                        ETWEvent etwEvent = gson.fromJson(line, ETWEvent.class);
                        Event event = new Event();
                        event.setNames(etwEvent.arguments.FileName);
                        event.setType(etwEvent.EventName);
                        if(!reduce(event, filelists, judgeprocessID, bufferedWriter)){
                            //store the data
                        }
//                        else bufferedWriter.append(line + "\r\n");
                    }
//                    else bufferedWriter.append(line + "\r\n");
                }catch (Exception e){
                    System.out.println(line);
                    e.printStackTrace();
                }
            }
            bufferedReader.close();
            bufferedWriter.close();
            System.out.println(filelists.size());
            PostGreSqlApi postGreSqlApi = new PostGreSqlApi(machineNum);
            postGreSqlApi.storeTempFileName(filelists);
            postGreSqlApi.closeConnection();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

//    public void JsonReduce(File source){
//        try{
//            BufferedReader bufferedReader = new BufferedReader(new FileReader(source));
//            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("temp.out"));
//            Set<String> filelists = getFileList();
//            List<String> judgeprocessID = new ArrayList<>();
//            String line = null;
//            String replacement = "Init Process";
//            Gson gson = new Gson();
//            int i = 0;
//            int j = 0;
//            Map<String, Integer> k = new HashMap<>();
//            while((line = bufferedReader.readLine())!=null) {
//                try {
//                    ETWEvent etwEvent = gson.fromJson(line, ETWEvent.class);
//                    if(!k.keySet().contains(etwEvent.EventName)){
//                        k.put(etwEvent.EventName, 1);
//                        System.out.println(line);
//                    }
//                    else {
//                        Integer temp = k.get(etwEvent.EventName);
//                        ++temp;
//                        k.put(etwEvent.EventName,temp);
//                    }
//                    if (line.contains("FileIoRead")) {
//                        String target = etwEvent.arguments.FileName;
//                        if (filelists.contains(target)) {
//                            i++;
//                            target = target.replaceAll("\\\\", "\\\\\\\\");
//                            if (!judgeprocessID.contains(String.valueOf(etwEvent.processID))) {
//                                j++;
//                                judgeprocessID.add(String.valueOf(etwEvent.processID));
//                                String newLine = line.replace(target, replacement);
//                                bufferedWriter.append(newLine + "\r\n");
//                            }
//                        }
//                        else bufferedWriter.append(line + "\r\n");
//                    }
//                    else bufferedWriter.append(line + "\r\n");
//                }catch (Exception e){
//                    System.out.println(line);
//                    e.printStackTrace();
//                }
//            }
//            bufferedReader.close();
//            bufferedWriter.close();
//            for(String it:k.keySet()){
//                System.out.println(it+" "+k.get(it));
//            }
//            System.out.println(i+" "+j);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//    }

    //CDM 20
    public void CDMreduce(File source, String machineNum){
        Set<String> filelists = getFileList(machineNum);

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


