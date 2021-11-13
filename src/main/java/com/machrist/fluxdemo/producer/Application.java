package com.machrist.fluxdemo.producer;

import com.github.javafaker.Faker;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.*;

@SpringBootApplication
@EnableConfigurationProperties(Config.class)
public class Application implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    @Autowired
    private Config config;
    private final Faker faker = new Faker();

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) {

        KafkaProducer<String, String> producer = null;
        try {
            producer = config.getKafkaProducer();
            for (Config.ProducerTopic topic : config.getTopics()) {
                log.info("producing to topic {}", topic.getTopic());
                for (int i = 0; i < topic.getIterations(); i++) {
                    for (ProducerRecord<String, String> record : getRecords(topic.getBatch(), topic.getType())) {
                        producer.send(record, (m, e) -> {
                            if (e != null) {
                                e.printStackTrace();
                            } else {
                                log.debug("Produced record to topic {} partition [{}] @ offset {}/n", m.topic(), m.partition(), m.offset());
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            log.error("Fatal error occurred", e);
        } finally {
            if (producer != null) {
                producer.close();
            }
        }
    }

    private List<ProducerRecord<String, String>> getRecords(int batchSize, String type) {

        List<ProducerRecord<String, String>> list = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) {

            String key = String.valueOf(faker.number().numberBetween(0, Integer.MAX_VALUE));
            String value;
            switch(type) {
                case "chuckNorris":
                    //noinspection UnusedAssignment
                    value = faker.chuckNorris().fact();
                case "animal":
                    //noinspection UnusedAssignment
                    value = faker.animal().name();
                case "backToTheFuture":
                    //noinspection UnusedAssignment
                    value = faker.backToTheFuture().quote();
                default:
                    value = faker.ancient().titan();
            }
            list.add(new ProducerRecord<>(key, value));
        }
        return list;
    }
}