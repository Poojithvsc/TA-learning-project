package com.example.kafka.consumer;

import com.example.kafka.config.KafkaConfig;
import com.example.kafka.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaConsumer implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    private final Consumer<String, String> consumer;
    private final ObjectMapper objectMapper;
    private final String topicName;
    private final AtomicBoolean running;
    private final Duration pollTimeout;

    public KafkaConsumer() {
        this.consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(KafkaConfig.getConsumerProperties());
        this.objectMapper = new ObjectMapper();
        this.topicName = KafkaConfig.getTopicName();
        this.running = new AtomicBoolean(false);
        this.pollTimeout = Duration.ofMillis(KafkaConfig.getPollTimeoutMs());

        consumer.subscribe(Collections.singletonList(topicName));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Kafka consumer...");
            shutdown();
        }));

        logger.info("Kafka consumer initialized and subscribed to topic: {}", topicName);
    }

    public void start() {
        start(null);
    }

    public void start(MessageHandler messageHandler) {
        if (!running.compareAndSet(false, true)) {
            logger.warn("Consumer is already running");
            return;
        }

        logger.info("Starting Kafka consumer...");

        try {
            while (running.get()) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(pollTimeout);

                    if (records.isEmpty()) {
                        logger.debug("No messages received in this poll cycle");
                        continue;
                    }

                    logger.debug("Received {} messages", records.count());

                    for (ConsumerRecord<String, String> record : records) {
                        processRecord(record, messageHandler);
                    }

                } catch (Exception e) {
                    logger.error("Error during message processing", e);
                    if (running.get()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        } catch (WakeupException e) {
            logger.info("Consumer wakeup called");
        } catch (Exception e) {
            logger.error("Unexpected error in consumer loop", e);
        } finally {
            cleanup();
        }
    }

    private void processRecord(ConsumerRecord<String, String> record, MessageHandler messageHandler) {
        try {
            logger.debug("Processing message - Key: {}, Partition: {}, Offset: {}",
                    record.key(), record.partition(), record.offset());

            Message message = objectMapper.readValue(record.value(), Message.class);

            logger.info("Received message: {}", message);

            if (messageHandler != null) {
                messageHandler.handle(message, record);
            }

        } catch (Exception e) {
            logger.error("Failed to process record with key: {} at offset: {}",
                    record.key(), record.offset(), e);
        }
    }

    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            logger.info("Shutting down consumer...");
            consumer.wakeup();
        }
    }

    private void cleanup() {
        try {
            consumer.close();
            logger.info("Kafka consumer closed successfully");
        } catch (Exception e) {
            logger.error("Error closing Kafka consumer", e);
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void close() {
        shutdown();
    }

    @FunctionalInterface
    public interface MessageHandler {
        void handle(Message message, ConsumerRecord<String, String> record);
    }
}