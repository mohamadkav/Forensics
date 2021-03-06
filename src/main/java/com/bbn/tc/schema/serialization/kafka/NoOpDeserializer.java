package com.bbn.tc.schema.serialization.kafka;

import com.bbn.tc.schema.serialization.AvroGenericDeserializer;
import org.apache.avro.generic.GenericData;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/**
 * Responsible for deserializing single avro records from kafka <br>
 * Expects the following properties: <br>
 *   <code>com.bbn.tc.schema.writer.file</code> (mandatory) <br>
 *   <code>com.bbn.tc.schema.reader.file</code> (optional, defaults to writer.file) <br>
 *   <code>com.bbn.tc.schema.fullname</code> (mandatory) <br>
 *
 * @see {@link KafkaAvroGenericSerializer}
 * @author jkhoury
 */
public class NoOpDeserializer
        extends AvroGenericDeserializer implements Deserializer<Object>{
    
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        try {
            initialize(configs, isKey);
        }catch(Exception e) {
            throw new ConfigException("Failed to configure kafka deserializer", e);
        }
    }

    @Override
    public Object deserialize(String topic, byte[] data) {
    	return new GenericData.Record(readerSchema);
    }

    @Override
    public void close() {

    }
}
