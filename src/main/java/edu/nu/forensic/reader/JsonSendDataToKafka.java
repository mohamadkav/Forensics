package edu.nu.forensic.reader;

import org.apache.avro.generic.GenericContainer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class JsonSendDataToKafka {
    private static final int NUM_SERVERS=1;
    private static List<Thread> dataSavers=new ArrayList<>(NUM_SERVERS);
    private static List<BlockingQueue<String>> queues=new ArrayList<>(NUM_SERVERS);
    public static void main(String[] args) throws Exception{
        long numIngested=0;
        long timeStampDiff=0;
        boolean diffSet=false;
        for(int i=0;i<NUM_SERVERS;i++) {
            queues.add(new LinkedBlockingDeque<>(100));
            Thread t = new Thread(new JsonDataSenderToKafka(i,queues.get(i)));
            dataSavers.add(t);
            dataSavers.get(i).start();
        }
        System.err.println("Start reading");
        Scanner input=new Scanner(new File("/home/mohammad/Downloads/47g.out"));
        while(input.hasNext()) {
            String rawEvent=input.nextLine();
            if(!rawEvent.contains("EventName"))
                continue;
            long timeStamp=Long.parseLong(rawEvent.substring(rawEvent.indexOf(":",rawEvent.indexOf("TimeStamp"))+1,rawEvent.indexOf(",",rawEvent.indexOf("TimeStamp"))).substring(0,13));
            if(!diffSet){
                timeStampDiff=System.currentTimeMillis()-timeStamp;
                diffSet=true;
            }
            if(System.currentTimeMillis()<(timeStamp+timeStampDiff))
                TimeUnit.MILLISECONDS.sleep(System.currentTimeMillis() - timeStamp - timeStampDiff);
            numIngested++;
            if(numIngested%10000==0)
                System.err.println("Ingested: "+numIngested);
            for(int i=0;i<NUM_SERVERS;i++)
                queues.get(i).put(rawEvent);
        }
    }
}

class JsonDataSenderToKafka extends Thread implements Runnable{
    private int threadNumber;
    private final BlockingQueue<String> queue;
    private KafkaProducer<String,String> kafkaProducer;

    public JsonDataSenderToKafka(int threadNumber,BlockingQueue<String> queue){
        this.threadNumber=threadNumber;
        this.queue=queue;
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "MARPLE");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
   //     props.put(AvroConfig.SCHEMA_READER_FILE, "schema/TCCDMDatum.avsc");
   //     props.put(AvroConfig.SCHEMA_WRITER_FILE, "schema/TCCDMDatum.avsc");
   //     props.put(AvroConfig.SCHEMA_SERDE_IS_SPECIFIC, false);
        kafkaProducer=new KafkaProducer<>(props);
        System.err.println("Started sender number "+this.threadNumber);

    }

    public void run(){
        while (true){
            try {
                String data = queue.take();
                ProducerRecord<String, String> record = new ProducerRecord<>("MARPLE"+threadNumber,data);
                kafkaProducer.send(record);

            }catch (InterruptedException e){
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }
}
