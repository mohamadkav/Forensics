import com.bbn.tc.schema.SchemaNotInitializedException;
import com.bbn.tc.schema.avro.cdm19.TCCDMDatum;
import com.bbn.tc.schema.serialization.AvroGenericDeserializer;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class Main {
    private static final Logger logger = Logger.getLogger("KafkaDemo");

    protected static String topic = "test_";
    protected static String kafkaServer = "localhost:9092";
    protected static String schemaFilename = "src/TCCDMDatum.avsc";
    protected static String groupId = "MARPLE";
//    protected static String hdfsUrl = "hdfs://localhost:8020";
    protected static int pollPeriod = 100;

    public static void main(String[] args) throws IOException, SchemaNotInitializedException {
//        AvroGenericDeserializer avroGenericDeserializer=new AvroGenericDeserializer(schemaFilename,schemaFilename,false,new File("C:\\Users\\Mohammad\\Desktop\\trytoduild\\Kafkaproj\\output.avro"));
//        IndexedRecord data= (IndexedRecord)avroGenericDeserializer.deserializeNextRecordFromFile();
//        System.out.println(data.toString());

        final KafkaReader tcConsumer = new KafkaReader(kafkaServer, groupId, topic, schemaFilename, pollPeriod);
        tcConsumer.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    if (tcConsumer != null) {
                        logger.info("Shutting down consumer.");
                        tcConsumer.setShutdown();
                        tcConsumer.join();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            if (tcConsumer != null && tcConsumer.isAlive()) {
                tcConsumer.join();
            }
        } catch (InterruptedException e) {
            logger.error(e);
        }
    }

}


