package edu.nu.forensic.reader;

import com.bbn.tc.schema.avro.cdm19.*;
import com.bbn.tc.schema.serialization.AvroConfig;
import edu.nu.forensic.util.RecordConverter;
import org.apache.avro.generic.GenericContainer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

@Component

public class KafkaReader {

    private KafkaConsumer<String, GenericContainer> consumer;
    private AtomicBoolean shutdown = new AtomicBoolean(false);

    private boolean setSpecificOffset = true;
    private long forcedOffset = 0;

    @Value("${kafkaServer}")
    private String kafkaServer;
    @Value("${groupId}")
    private String groupId;
    @Value("${topic}")
    private String topic;
    @Value("${pollPeriod}")
    private Integer pollPeriod;

    @Autowired
    private RecordConverter recordConverter;

    public void readTrace() {
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

        String schemaFilename = "schema/TCCDMDatum.avsc";
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


/*        PrintWriter out=null;
        try {
            out=new PrintWriter(new File("D:\\ta1-marple-ped-live-s1"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/
        ConsumerRecords<String, GenericContainer> records = null;
        try{
            while (!shutdown.get()) {
                records = consumer.poll(pollPeriod);
                Iterator recIter = records.iterator();
                ConsumerRecord<String, GenericContainer> record = null;
                while (recIter.hasNext()){
                    record = (ConsumerRecord<String, GenericContainer>)  recIter.next();
                    TCCDMDatum CDMdatum = (TCCDMDatum) record.value();
                    try {
                        if (CDMdatum.getDatum() instanceof Subject)
                            recordConverter.saveAndConvertBBNSubjectToSubject((Subject) CDMdatum.getDatum());
                        else if (CDMdatum.getDatum() instanceof Principal)
                            recordConverter.saveAndConvertBBNPrincipalToPrincipal((Principal) CDMdatum.getDatum());
                        else if (CDMdatum.getDatum() instanceof FileObject)
                            recordConverter.saveAndConvertBBNFileObjectToFileObject((FileObject) CDMdatum.getDatum());
                        else if (CDMdatum.getDatum() instanceof RegistryKeyObject)
                            recordConverter.saveAndConvertBBNRegistryKeyObjectToRegistryKeyObject((RegistryKeyObject) CDMdatum.getDatum());
                        else if (CDMdatum.getDatum() instanceof NetFlowObject)
                            recordConverter.saveAndConvertBBNNetFlowObjectToNetFlowObject((NetFlowObject) CDMdatum.getDatum());
                        else if (CDMdatum.getDatum() instanceof Event)
                            recordConverter.saveAndConvertBBNEventToEvent((Event) CDMdatum.getDatum());
                        else if (CDMdatum.getDatum() instanceof UnitDependency)
                            recordConverter.saveAndConvertBBNUnitDependencyToUnitDependency((UnitDependency) CDMdatum.getDatum());
                        else
                            System.err.println(CDMdatum.toString());
                    }catch (Exception e){
                        System.err.println("Darn! We have an unknown bug over: ");
                        System.err.println(CDMdatum);
                        e.printStackTrace();
                    }
/*                    out.println(CDMdatum);
                    out.flush();*/
                }
            }
            closeConsumer();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void closeConsumer() {
        if(consumer != null) {
            consumer.commitSync();
            consumer.unsubscribe();
            consumer.close();
        }
    }

}
