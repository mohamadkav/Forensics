package edu.nu.forensic.reader;

import com.bbn.tc.schema.SchemaNotInitializedException;
import com.bbn.tc.schema.avro.cdm19.*;
import com.bbn.tc.schema.serialization.AvroGenericDeserializer;
import edu.nu.forensic.reducer.Reducer;
import edu.nu.forensic.util.RecordConverter;
import org.apache.avro.generic.GenericContainer;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.Scanner;

public class RuducerReader {

    @Autowired
    private RecordConverter recordConverter;

    private void writeResult(String data, BufferedWriter bufferedWriter){
        try {
            bufferedWriter.append(data + "\r\n");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void ReadTraceforReduce(File source) throws IOException, SchemaNotInitializedException {
        AvroGenericDeserializer avroGenericDeserializer=new AvroGenericDeserializer("schema/TCCDMDatum.avsc","schema/TCCDMDatum.avsc",
                true,source);
        final Scanner scanner = new Scanner(new FileReader("C:\\Data\\ta1-marple-e4-A.index"));
        int i=0;

        BufferedWriter  bufferedWriter = new BufferedWriter(new FileWriter("temp.txt"));
        while(scanner.hasNextInt()){
            final int length  = scanner.nextInt();
//            GenericContainer data= (GenericContainer)avroGenericDeserializer.deserializeNextRecordFromFile();
            GenericContainer data= (GenericContainer)avroGenericDeserializer.deserializeNextRecordFromFile(length);
            if(data==null)
                break;
            TCCDMDatum CDMdatum=(TCCDMDatum) data;
            try {
                if(i%10000==0) System.out.println(i);
                i++;
                    if (CDMdatum.getDatum() instanceof Event ) {
                    if (((Event) CDMdatum.getDatum()).getType().toString().contains("READ") || ((Event) CDMdatum.getDatum()).getType().toString().contains("WRITE"))
                        recordConverter.saveAndConvertBBNEventToEvent((Event) CDMdatum.getDatum());
                    else writeResult(CDMdatum.getDatum().toString(), bufferedWriter);
                }
                else writeResult(CDMdatum.getDatum().toString(), bufferedWriter);
            }catch (Exception e){
                System.err.println("Darn! We have an unknown bug over: ");
                System.err.println(CDMdatum);
                e.printStackTrace();
            }
        }
        bufferedWriter.flush();
        bufferedWriter.close();
        avroGenericDeserializer.close();
    }

    public void ReadTraceforReduceWithoutIndex(File source) throws IOException, SchemaNotInitializedException {
        AvroGenericDeserializer avroGenericDeserializer=new AvroGenericDeserializer("schema/TCCDMDatum.avsc","schema/TCCDMDatum.avsc",
                true,source);
        int i=0;
        BufferedWriter  bufferedWriter = new BufferedWriter(new FileWriter("temp.txt"));
        while(true){
            GenericContainer data= (GenericContainer)avroGenericDeserializer.deserializeNextRecordFromFile();
            if(data==null)
                break;
            TCCDMDatum CDMdatum=(TCCDMDatum) data;
            try {
                if(i%10000==0) System.out.println(i);
                i++;
                if (CDMdatum.getDatum() instanceof Event ) {
                    if (((Event) CDMdatum.getDatum()).getType().toString().contains("READ") || ((Event) CDMdatum.getDatum()).getType().toString().contains("WRITE"))
                        recordConverter.saveAndConvertBBNEventToEvent((Event) CDMdatum.getDatum());
                    else writeResult(CDMdatum.getDatum().toString(), bufferedWriter);
                }
                else writeResult(CDMdatum.getDatum().toString(), bufferedWriter);
            }catch (Exception e){
                System.err.println("Darn! We have an unknown bug over: ");
                System.err.println(CDMdatum);
                e.printStackTrace();
            }
        }
        bufferedWriter.flush();
        bufferedWriter.close();
        avroGenericDeserializer.close();
    }
}
