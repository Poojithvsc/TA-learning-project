package com.example.kafka.kafka;

import com.example.kafka.kafka.FileEventProducer.FileEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * FileEventConsumer receives file events from Kafka
 *
 * This consumer listens for file upload/delete events and can:
 * - Generate thumbnails for images
 * - Extract metadata from videos
 * - Scan files for viruses
 * - Send notifications to users
 * - Update search indexes
 * - Trigger workflows
 *
 * This is how distributed systems process events asynchronously!
 */
public class FileEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(FileEventConsumer.class);

    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper objectMapper;
    private final String topic;
    private volatile boolean running = false;

    /**
     * Initialize Kafka consumer for file events
     */
    public FileEventConsumer(String bootstrapServers, String groupId, String topic) {
        this.topic = topic;
        this.objectMapper = new ObjectMapper();

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Start from beginning
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        this.consumer = new KafkaConsumer<>(props);
        logger.info("FileEventConsumer initialized for topic: {} with group: {}", topic, groupId);
    }

    /**
     * Start consuming file events
     *
     * @param eventHandler Function to handle each file event
     */
    public void start(Consumer<FileEvent> eventHandler) {
        running = true;
        consumer.subscribe(Collections.singletonList(topic));

        logger.info("Starting to consume file events from topic: {}", topic);

        try {
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        // Parse event JSON
                        FileEvent event = objectMapper.readValue(record.value(), FileEvent.class);

                        logger.info("Received file event: {} - {} [partition={}, offset={}]",
                            event.getEventType(),
                            event.getFileMetadata().getFilename(),
                            record.partition(),
                            record.offset());

                        // Handle the event
                        eventHandler.accept(event);

                    } catch (Exception e) {
                        logger.error("Error processing file event from partition {} offset {}",
                            record.partition(), record.offset(), e);
                    }
                }
            }
        } finally {
            consumer.close();
            logger.info("FileEventConsumer stopped");
        }
    }

    /**
     * Stop consuming
     */
    public void stop() {
        running = false;
    }

    /**
     * Helper method to format file size
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
