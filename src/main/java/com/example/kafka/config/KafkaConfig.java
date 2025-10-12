package com.example.kafka.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class KafkaConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);
    private static final String KAFKA_CONFIG_FILE = "kafka.properties";

    private static Properties kafkaProperties;

    static {
        loadKafkaProperties();
    }

    private static void loadKafkaProperties() {
        kafkaProperties = new Properties();
        try (InputStream input = KafkaConfig.class.getClassLoader().getResourceAsStream(KAFKA_CONFIG_FILE)) {
            if (input == null) {
                logger.error("Unable to find {}", KAFKA_CONFIG_FILE);
                throw new RuntimeException("Configuration file not found: " + KAFKA_CONFIG_FILE);
            }
            kafkaProperties.load(input);
            logger.info("Kafka configuration loaded successfully");
        } catch (IOException ex) {
            logger.error("Error loading Kafka configuration", ex);
            throw new RuntimeException("Failed to load Kafka configuration", ex);
        }
    }

    public static Properties getProducerProperties() {
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", kafkaProperties.getProperty("bootstrap.servers"));
        producerProps.put("key.serializer", kafkaProperties.getProperty("producer.key.serializer"));
        producerProps.put("value.serializer", kafkaProperties.getProperty("producer.value.serializer"));
        producerProps.put("acks", kafkaProperties.getProperty("producer.acks"));
        producerProps.put("retries", kafkaProperties.getProperty("producer.retries"));
        producerProps.put("batch.size", kafkaProperties.getProperty("producer.batch.size"));
        producerProps.put("linger.ms", kafkaProperties.getProperty("producer.linger.ms"));
        producerProps.put("buffer.memory", kafkaProperties.getProperty("producer.buffer.memory"));
        producerProps.put("max.block.ms", kafkaProperties.getProperty("producer.max.block.ms"));

        return producerProps;
    }

    public static Properties getConsumerProperties() {
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", kafkaProperties.getProperty("bootstrap.servers"));
        consumerProps.put("key.deserializer", kafkaProperties.getProperty("consumer.key.deserializer"));
        consumerProps.put("value.deserializer", kafkaProperties.getProperty("consumer.value.deserializer"));
        consumerProps.put("group.id", kafkaProperties.getProperty("consumer.group.id"));
        consumerProps.put("auto.offset.reset", kafkaProperties.getProperty("consumer.auto.offset.reset"));
        consumerProps.put("enable.auto.commit", kafkaProperties.getProperty("consumer.enable.auto.commit"));
        consumerProps.put("auto.commit.interval.ms", kafkaProperties.getProperty("consumer.auto.commit.interval.ms"));
        consumerProps.put("session.timeout.ms", kafkaProperties.getProperty("consumer.session.timeout.ms"));
        consumerProps.put("fetch.min.bytes", kafkaProperties.getProperty("consumer.fetch.min.bytes"));
        consumerProps.put("fetch.max.wait.ms", kafkaProperties.getProperty("consumer.fetch.max.wait.ms"));

        return consumerProps;
    }

    public static String getTopicName() {
        return kafkaProperties.getProperty("topic.name");
    }

    public static int getTopicPartitions() {
        return Integer.parseInt(kafkaProperties.getProperty("topic.partitions", "1"));
    }

    public static short getTopicReplicationFactor() {
        return Short.parseShort(kafkaProperties.getProperty("topic.replication.factor", "1"));
    }

    public static long getPollTimeoutMs() {
        return Long.parseLong(kafkaProperties.getProperty("poll.timeout.ms", "1000"));
    }

    public static int getMessagesPerSecond() {
        return Integer.parseInt(kafkaProperties.getProperty("messages.per.second", "1"));
    }

    public static String getBootstrapServers() {
        return kafkaProperties.getProperty("bootstrap.servers");
    }
}