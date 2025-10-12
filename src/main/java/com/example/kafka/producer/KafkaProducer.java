package com.example.kafka.producer;

import com.example.kafka.config.KafkaConfig;
import com.example.kafka.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaProducer implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);

    private final Producer<String, String> producer;
    private final ObjectMapper objectMapper;
    private final String topicName;
    private final AtomicBoolean running;

    public KafkaProducer() {
        this.producer = new org.apache.kafka.clients.producer.KafkaProducer<>(KafkaConfig.getProducerProperties());
        this.objectMapper = new ObjectMapper();
        this.topicName = KafkaConfig.getTopicName();
        this.running = new AtomicBoolean(true);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Kafka producer...");
            close();
        }));

        logger.info("Kafka producer initialized for topic: {}", topicName);
    }

    public Future<RecordMetadata> sendMessage(Message message) {
        if (!running.get()) {
            throw new IllegalStateException("Producer has been closed");
        }

        try {
            String messageJson = objectMapper.writeValueAsString(message);
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    topicName,
                    message.getId(),
                    messageJson
            );

            logger.debug("Sending message with key: {} to topic: {}", message.getId(), topicName);

            return producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Error sending message with key: {}", message.getId(), exception);
                } else {
                    logger.debug("Message sent successfully - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                            metadata.topic(), metadata.partition(), metadata.offset(), message.getId());
                }
            });

        } catch (Exception e) {
            logger.error("Failed to send message with key: {}", message.getId(), e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    public void sendMessageSync(Message message) {
        try {
            Future<RecordMetadata> future = sendMessage(message);
            RecordMetadata metadata = future.get();
            logger.info("Message sent synchronously - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                    metadata.topic(), metadata.partition(), metadata.offset(), message.getId());
        } catch (Exception e) {
            logger.error("Failed to send message synchronously with key: {}", message.getId(), e);
            throw new RuntimeException("Failed to send message synchronously", e);
        }
    }

    public void flush() {
        if (running.get()) {
            producer.flush();
            logger.debug("Producer flushed");
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void close() {
        if (running.compareAndSet(true, false)) {
            try {
                logger.info("Closing Kafka producer...");
                producer.flush();
                producer.close();
                logger.info("Kafka producer closed successfully");
            } catch (Exception e) {
                logger.error("Error closing Kafka producer", e);
            }
        }
    }
}