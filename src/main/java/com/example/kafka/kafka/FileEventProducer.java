package com.example.kafka.kafka;

import com.example.kafka.model.FileMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * FileEventProducer sends file upload events to Kafka
 *
 * When a file is uploaded to S3, this producer sends an event to Kafka
 * so other services can react to the upload (e.g., generate thumbnails,
 * scan for viruses, send notifications, etc.)
 *
 * Event Flow:
 * 1. File uploaded to S3
 * 2. Metadata saved to database
 * 3. Event sent to Kafka → "file-events" topic
 * 4. Consumers can process the event
 */
public class FileEventProducer {
    private static final Logger logger = LoggerFactory.getLogger(FileEventProducer.class);

    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;
    private final String topic;

    /**
     * Initialize Kafka producer for file events
     */
    public FileEventProducer(String bootstrapServers, String topic) {
        this.topic = topic;
        this.objectMapper = new ObjectMapper();

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        props.put(ProducerConfig.RETRIES_CONFIG, 3); // Retry up to 3 times
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1); // Small delay to batch messages

        this.producer = new KafkaProducer<>(props);
        logger.info("FileEventProducer initialized for topic: {}", topic);
    }

    /**
     * Send file upload event to Kafka
     *
     * @param fileMetadata The metadata of the uploaded file
     * @param eventType Type of event (e.g., "UPLOADED", "DELETED", "UPDATED")
     */
    public void sendFileEvent(FileMetadata fileMetadata, String eventType) {
        try {
            // Create event payload
            FileEvent event = new FileEvent(
                eventType,
                fileMetadata,
                System.currentTimeMillis()
            );

            // Convert to JSON
            String eventJson = objectMapper.writeValueAsString(event);

            // Send to Kafka
            ProducerRecord<String, String> record = new ProducerRecord<>(
                topic,
                fileMetadata.getId(), // Key = file ID for partitioning
                eventJson
            );

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Failed to send file event for: {}", fileMetadata.getFilename(), exception);
                } else {
                    logger.info("File event sent: {} - {} [partition={}, offset={}]",
                        eventType, fileMetadata.getFilename(),
                        metadata.partition(), metadata.offset());
                }
            });

        } catch (Exception e) {
            logger.error("Error sending file event", e);
        }
    }

    /**
     * Send file uploaded event
     */
    public void sendUploadedEvent(FileMetadata fileMetadata) {
        sendFileEvent(fileMetadata, "UPLOADED");
    }

    /**
     * Send file deleted event
     */
    public void sendDeletedEvent(FileMetadata fileMetadata) {
        sendFileEvent(fileMetadata, "DELETED");
    }

    /**
     * Flush and close producer
     */
    public void close() {
        if (producer != null) {
            producer.flush();
            producer.close();
            logger.info("FileEventProducer closed");
        }
    }

    /**
     * Inner class representing a file event
     */
    public static class FileEvent {
        private String eventType;
        private FileMetadata fileMetadata;
        private long timestamp;

        public FileEvent() {
        }

        public FileEvent(String eventType, FileMetadata fileMetadata, long timestamp) {
            this.eventType = eventType;
            this.fileMetadata = fileMetadata;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public FileMetadata getFileMetadata() {
            return fileMetadata;
        }

        public void setFileMetadata(FileMetadata fileMetadata) {
            this.fileMetadata = fileMetadata;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
