package edu.nu.forensic.reducer;

import com.bbn.tc.schema.avro.cdm19.Event;
import com.bbn.tc.schema.avro.cdm19.TCCDMDatum;
import com.bbn.tc.schema.serialization.AvroGenericDeserializer;
import edu.nu.forensic.db.DBApi.PostGreSqlApi;
import org.apache.avro.generic.GenericContainer;

import java.io.*;
import java.util.*;
import org.springframework.stereotype.Component;

import static edu.nu.forensic.reducer.FPGrowth.findFrequentItemsetWithSuffix;


@Component
public class Reducer {

    private PostGreSqlApi postGreSqlApi = new PostGreSqlApi();

//    private HashMap<String, HashSet<Integer>> fileToProcessesWhichHaveAccessedIt=new HashMap<>();
    private HashSet<Integer> test=new HashSet<>();

    public void reduce(File source){


        //Extract all file reads and writes
        Map<String,Map<String, Integer>> processIdToFileFrequences = postGreSqlApi.getProcessToFileFrequences();

        //Building FP tree
        Node root=new Node(null);
        for(String process: processIdToFileFrequences.keySet()){
            Node head = new Node(null);
            Map<String, Integer> fileFrequences = processIdToFileFrequences.get(process);
            if(fileFrequences.size()!=0) {
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

        System.out.println("FP-tree");
        for(Node node:root.getChildren())
        {
            System.out.println("root");
            Queue<Node> q = new LinkedList<>();
            q.add(node);
            while(q.size()!=0)
            {
                Node temp = q.poll();
                System.out.println(temp.getFileName()+" "+temp.counter);
                q.addAll(temp.getChildren());
            }
        }

        Set<List<String>> CFAP = findFrequentItemsetWithSuffix(root, 0);
        Set<String> filelists = new HashSet<>();

        System.out.println("frequent scequence");
        for(List<String> it:CFAP) {
            System.out.println(it.toString());
            filelists.addAll(it);
        }


        try{
            AvroGenericDeserializer avroGenericDeserializer=new AvroGenericDeserializer("schema/TCCDMDatum.avsc","schema/TCCDMDatum.avsc",
                    true,source);
            int i=0;
            List<String> judgeprocessID = new ArrayList<>();
            List<String> writeFiles = new LinkedList<>();
            BufferedWriter  bufferedWriter = new BufferedWriter(new FileWriter("temp.txt"));
            while(true){
                GenericContainer data= (GenericContainer)avroGenericDeserializer.deserializeNextRecordFromFile();
                if(data==null)
                    break;
                TCCDMDatum CDMdatum=(TCCDMDatum) data;
                try {
                    if (i % 10000 == 0) System.out.println(i);
                    i++;
                    if (CDMdatum.getDatum() instanceof Event) {
                        if (((Event) CDMdatum.getDatum()).getNames().contains("FileIoRead")) {
                            if(!judgeprocessID.contains(((Event) CDMdatum.getDatum()).getSubject().toString())
                                    &&filelists.contains(((Event)CDMdatum.getDatum()).getPredicateObjectPath())){
                                judgeprocessID.add(((Event) CDMdatum.getDatum()).getSubject().toString());
                                String temp = CDMdatum.getDatum().toString();
                                temp = temp.replace(((Event)CDMdatum.getDatum()).getPredicateObjectPath(),"Initial process");
                                bufferedWriter.append(temp+"\r\n");
                            }
                            else if(!filelists.contains(((Event)CDMdatum.getDatum()).getPredicateObjectPath())){
                                bufferedWriter.append(CDMdatum.getDatum().toString()+"\r\n");
                            }
                        }
                        else if(((Event) CDMdatum.getDatum()).getNames().contains("FileIoWrite")){
                            String WrittenFile = ((Event)CDMdatum.getDatum()).getPredicateObjectPath().toString();
                            if(!writeFiles.contains(WrittenFile)) writeFiles.add(WrittenFile);
                            if(filelists.contains(WrittenFile)) filelists.remove(WrittenFile);
                        }
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
            avroGenericDeserializer.close();
        }catch (Exception e){
            e.printStackTrace();
        }

//        try {
//            AvroGenericDeserializer avroGenericDeserializer = new AvroGenericDeserializer("schema/TCCDMDatum.avsc", "schema/TCCDMDatum.avsc",
//                    true, source);
//            final Scanner scanner = new Scanner(new FileReader("C:\\Data\\ta1-marple-e4-A.index"));
//            int i = 0;
//            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File("C:\\Data\\test.json")));
//
//            List<String> judgeprocessID = new ArrayList<>();
//            List<String> writeFiles = new LinkedList<>();
//            while (scanner.hasNextInt()) {
//                final int length = scanner.nextInt();
////            GenericContainer data= (GenericContainer)avroGenericDeserializer.deserializeNextRecordFromFile();
//                GenericContainer data = (GenericContainer) avroGenericDeserializer.deserializeNextRecordFromFile(length);
//                if (data == null) break;
//                TCCDMDatum CDMdatum = (TCCDMDatum) data;
//                try {
//                    if (i % 10000 == 0) System.out.println(i);
//                    i++;
//                    if (CDMdatum.getDatum() instanceof Event) {
//                        if (((Event) CDMdatum.getDatum()).getNames().contains("FileIoRead")) {
//                            if(!judgeprocessID.contains(((Event) CDMdatum.getDatum()).getSubjectUUID().toString())
//                                    &&filelists.contains(((Event)CDMdatum.getDatum()).getPredicateObjectPath())){
//                                judgeprocessID.add(((Event) CDMdatum.getDatum()).getSubjectUUID().toString());
//                                String temp = CDMdatum.getDatum().toString();
//                                temp = temp.replace(((Event)CDMdatum.getDatum()).getPredicateObjectPath(),"Initial process");
//                                bufferedWriter.append(temp+"\r\n");
//                            }
//                            else if(!filelists.contains(((Event)CDMdatum.getDatum()).getPredicateObjectPath())){
//                                bufferedWriter.append(CDMdatum.getDatum().toString()+"\r\n");
//                            }
//                        }
//                        else if(((Event) CDMdatum.getDatum()).getNames().contains("FileIoWrite")){
//                            String WrittenFile = ((Event)CDMdatum.getDatum()).getPredicateObjectPath();
//                            if(!writeFiles.contains(WrittenFile)) writeFiles.add(WrittenFile);
//                            if(filelists.contains(WrittenFile)) filelists.remove(WrittenFile);
//                        }
//                    }
//                    else bufferedWriter.append(CDMdatum.getDatum().toString()+"\r\n");
//                } catch (Exception e) {
//                    System.err.println("Darn! We have an unknown bug over: ");
////                System.err.println(CDMdatum);
//                    e.printStackTrace();
//                }
//            }
//            bufferedWriter.flush();
//            bufferedWriter.close();
//        }catch (Exception e){
//            e.printStackTrace();
//        }

//        StatementRoot FSARoot = new StatementRoot();
//        Map<String, Integer> FileToNum = new HashMap<>();
//        Integer n =0;
//        FSARoot = buildFSA(FileToNum, CFAP, n);
//
//        System.out.println("number to file name");
//        for(String it:FileToNum.keySet()) System.out.println(it+" "+FileToNum.get(it));
//
//        System.out.println("finite state automaton");
//        printFSA(FSARoot);
    }
}