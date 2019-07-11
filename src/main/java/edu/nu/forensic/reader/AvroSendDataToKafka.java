package edu.nu.forensic.reader;

import com.bbn.tc.schema.avro.cdm19.HostType;
import com.bbn.tc.schema.avro.cdm19.TCCDMDatum;
import com.bbn.tc.schema.serialization.AvroConfig;
import com.bbn.tc.schema.serialization.AvroGenericDeserializer;
import com.bbn.tc.schema.serialization.kafka.KafkaAvroGenericDeserializer;
import com.bbn.tc.schema.serialization.kafka.KafkaAvroGenericSerializer;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.generic.GenericData;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;

public class AvroSendDataToKafka {
    private static final int NUM_SERVERS=1;
    private static List<Thread> dataSavers=new ArrayList<>(NUM_SERVERS);
    private static List<BlockingQueue<TCCDMDatum>> queues=new ArrayList<>(NUM_SERVERS);
    public static void main(String[] args) throws Exception{
        for(int i=0;i<NUM_SERVERS;i++) {
            queues.add(new LinkedBlockingDeque<>(100));
            Thread t = new Thread(new DataSenderToKafka(i,queues.get(i)));
            dataSavers.add(t);
            dataSavers.get(i).start();
        }
        System.err.println("Start reading");
        int fileCounter = 0;
        int fileNum = 10;
        while(fileCounter < fileNum){
            System.err.println(fileCounter);
            String raw = "/home/mohammad/Downloads/E4/" + "ta1-marple-e4-A.bin";
            if(fileCounter !=0)
                raw+="."+ fileCounter;
            fileCounter += 1;
            AvroGenericDeserializer avroGenericDeserializer=new AvroGenericDeserializer("schema/TCCDMDatum.avsc","schema/TCCDMDatum.avsc",
                    true,new File(raw));
            while(true) {
                GenericContainer data = avroGenericDeserializer.deserializeNextRecordFromFile();
                if (data == null) {
                    avroGenericDeserializer.close();
                    break;
                }
                TCCDMDatum CDMdatum = (TCCDMDatum) data;

                if(CDMdatum.getDatum() instanceof com.bbn.tc.schema.avro.cdm19.Event){
                    com.bbn.tc.schema.avro.cdm19.Event bbnEvent=(com.bbn.tc.schema.avro.cdm19.Event) CDMdatum.getDatum();

                    if(bbnEvent.getType().toString().equals("EVENT_OTHER")){
                        if(bbnEvent.getNames()==null||!(bbnEvent.getNames().get(0).toString().equals("FileIoRead")||bbnEvent.getNames().get(0).toString().equals("FileIoWrite")))
                            continue;
                        else if (bbnEvent.getPredicateObjectPath().toString().equals(("UNKNOWN_FILE")))
                            continue;
                    }
                    else if(!(bbnEvent.getType().toString().equals("EVENT_READ")||bbnEvent.getType().toString().equals("EVENT_WRITE")))
                        continue;
                }
                for(int i=0;i<NUM_SERVERS;i++)
                    queues.get(i).put(CDMdatum);
            }
        }
    }
}

class DataSenderToKafka extends Thread implements Runnable{
    private int threadNumber;
    private final BlockingQueue<TCCDMDatum> queue;
    private KafkaProducer<String,TCCDMDatum> kafkaProducer;

    public DataSenderToKafka(int threadNumber,BlockingQueue<TCCDMDatum> queue){
        this.threadNumber=threadNumber;
        this.queue=queue;
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "MARPLE");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroGenericSerializer.class);
   //     props.put(AvroConfig.SCHEMA_READER_FILE, "schema/TCCDMDatum.avsc");
        props.put(AvroConfig.SCHEMA_WRITER_FILE, "schema/TCCDMDatum.avsc");
        props.put(AvroConfig.SCHEMA_SERDE_IS_SPECIFIC, false);
        kafkaProducer=new KafkaProducer<>(props);

    }

    public void run(){
        while (true){
            try {
                System.err.println("Taking from queue");
                TCCDMDatum CDMdatum = queue.take();
                ProducerRecord<String, TCCDMDatum> record = new ProducerRecord<>("MARPLE"+threadNumber,CDMdatum);
                kafkaProducer.send(record);

            }catch (InterruptedException e){
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }
}
