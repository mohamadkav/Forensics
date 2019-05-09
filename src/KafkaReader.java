import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bbn.tc.schema.avro.cdm20.TCCDMDatum;
import org.apache.avro.generic.GenericContainer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


import com.bbn.tc.schema.serialization.AvroConfig;

public class KafkaReader extends Thread{
    private final Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

    protected final KafkaConsumer<String, GenericContainer> consumer;
    protected long recordCounter = 0;
    protected AtomicBoolean shutdown = new AtomicBoolean(false);
    protected int pollPeriod = 100;

    private boolean setSpecificOffset = true;
    private long forcedOffset = 0;
    private String topic;


    public KafkaReader(String kafkaServer, String groupId, String topic, String schemaFilename, int pollPeriod) {

        logger.setLevel(Level.DEBUG);
        this.pollPeriod = pollPeriod;
        this.topic = topic;

        Properties properties = new Properties();
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        //add some other properties
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 20000);
        properties.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, "0"); //ensure no temporal batching
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // latest or earliestautoOffset

        //serialization properties
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");

        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                com.bbn.tc.schema.serialization.kafka.KafkaAvroGenericDeserializer.class);

        properties.put(AvroConfig.SCHEMA_READER_FILE, schemaFilename);
        properties.put(AvroConfig.SCHEMA_WRITER_FILE, schemaFilename);
        properties.put(AvroConfig.SCHEMA_SERDE_IS_SPECIFIC, true);

//        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
//        properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/var/private/ssl/kafka.client.truststore.jks");
//        properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "TransparentComputing");
//        properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, "/var/private/ssl/kafka.client.keystore.jks");
//        properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "TransparentComputing");
//        properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "TransparentComputing");

        consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Arrays.asList(topic.split(",")));

    }

    public void setShutdown(){
        this.shutdown.set(true);
    }
    public void run(){
        logger.info("Started KafkaReader");
        recordCounter = 0;
        PrintWriter output =null;
        try {
             output = new PrintWriter("outputtest.json", "UTF-8");
        }catch (Exception e){
            e.printStackTrace();
            return;
        }
        boolean receivedSomethingYet = false;
        ConsumerRecords<String, GenericContainer> records = null;
        ReverseConversion.init();
        try{
            while (!shutdown.get()) {
                // =================== <KAFKA consumer> ===================
                records = consumer.poll(pollPeriod);

                Iterator recIter = records.iterator();
                ConsumerRecord<String, GenericContainer> record = null;
                while (recIter.hasNext()){
                    record = (ConsumerRecord<String, GenericContainer>)  recIter.next();
                    TCCDMDatum CDMdatum = (TCCDMDatum) record.value();
                    EventRecord tempRecord = new EventRecord();
                    try{
                        tempRecord = ReverseConversion.parse(CDMdatum);
                        if(tempRecord.eventName!=null&&!tempRecord.eventName.isEmpty()) {
                    //        output.println(tempRecord.toString());
                    //        output.flush();
                        }
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
                // =================== </KAFKA consumer> ===================
            }
            closeConsumer();
            logger.info("Done.");
        } catch (Exception e){
            logger.error("Error while consuming from Kafka", e);
//            e.printStackTrace();
        }
    }

    protected void closeConsumer() {
        if(consumer != null) {
            logger.info("Closing consumer session ...");
            consumer.commitSync();
            logger.info("Committed");
            consumer.unsubscribe();
            logger.info("Unsubscribed");
            consumer.close();
            logger.info("Consumer session closed.");
        }
    }

}
