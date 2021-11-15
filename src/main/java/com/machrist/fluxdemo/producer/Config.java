package com.machrist.fluxdemo.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;

import static java.lang.String.format;

@Configuration
@ConfigurationProperties(prefix = "application")
public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    private Properties producer;
    private List<ProducerTopic> topics;

    public Properties getProducer() {
        return producer;
    }

    public void setProducer(Properties producer) {
        this.producer = producer;
    }

    public List<ProducerTopic> getTopics() {
        return topics;
    }

    public void setTopics(List<ProducerTopic> topics) {
        this.topics = topics;
    }

    public KafkaProducer<String, String> getKafkaProducer() throws JsonProcessingException {
        setSaslJaasConfig();
        log.info("Kafka Producer Properties:");
        for(Map.Entry<Object, Object> entry : producer.entrySet()) {
            log.info("{}: {}", entry.getKey(), entry.getValue());
        }
        return new KafkaProducer<>(producer);
    }

    public static class ProducerTopic {

        private String topic;
        private int iterations;
        private int batch;
        private int frequency;
        private String type;

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public int getIterations() {
            return iterations;
        }

        public void setIterations(int iterations) {
            this.iterations = iterations;
        }

        public int getFrequency() {
            return frequency;
        }

        public void setFrequency(int frequency) {
            this.frequency = frequency;
        }

        public int getBatch() {
            return batch;
        }

        public void setBatch(int batch) {
            this.batch = batch;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    private void setSaslJaasConfig() throws JsonProcessingException {
        // the 'sasl.jaas.config' property comes in as a JSON string from the ccloud operator
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(producer.getProperty("sasl.jaas.config"));
        producer.setProperty("sasl.jaas.config",
                format("org.apache.kafka.common.security.plain.PlainLoginModule required username='%s' password='%s';",
                        node.get("key").asText(), node.get("secret").asText()));
    }

    private void setSslSystemProperties() {
        // set SSL properties as system properties
        if (producer.get("ssl.keystore.location") != null) {
            System.setProperty("javax.net.ssl.keyStore", (String) producer.get("ssl.keystore.location"));
        }
        if (producer.get("ssl.keystore.password") != null) {
            System.setProperty("javax.net.ssl.keyStorePassword", (String) producer.get("ssl.keystore.password"));
        }
        if (producer.get("ssl.truststore.location") != null) {
            System.setProperty("javax.net.ssl.trustStore", (String) producer.get("ssl.truststore.location"));
        }
        if (producer.get("ssl.truststore.password") != null) {
            System.setProperty("javax.net.ssl.trustStorePassword", (String) producer.get("ssl.truststore.password"));
        }
        // Create a trust manager that does not validate certificate chains
        producer.setProperty("ssl.engine.factory.class", TrustingSslEngineFactory.class.getName());
    }
}
