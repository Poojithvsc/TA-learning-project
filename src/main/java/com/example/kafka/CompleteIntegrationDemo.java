package com.example.kafka;

import com.example.kafka.db.DatabaseManager;
import com.example.kafka.db.FileRepository;
import com.example.kafka.kafka.FileEventConsumer;
import com.example.kafka.kafka.FileEventProducer;
import com.example.kafka.model.FileMetadata;
import com.example.kafka.storage.S3Manager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * CompleteIntegrationDemo - Demonstrates S3 + Kafka + Database working together
 *
 * This is ENTERPRISE-GRADE architecture used by:
 * - Dropbox, Google Drive (file storage)
 * - Netflix, YouTube (video platforms)
 * - Spotify (music streaming)
 * - Instagram, Facebook (photo sharing)
 *
 * Flow:
 * 1. User uploads file
 * 2. File stored in S3 (MinIO)
 * 3. Metadata saved to PostgreSQL
 * 4. Event sent to Kafka
 * 5. Consumer receives event and processes it
 *
 * This is called EVENT-DRIVEN ARCHITECTURE - the foundation of modern cloud systems!
 */
public class CompleteIntegrationDemo {
    private static final Logger logger = LoggerFactory.getLogger(CompleteIntegrationDemo.class);

    private final S3Manager s3Manager;
    private final FileRepository fileRepository;
    private final DatabaseManager dbManager;
    private final FileEventProducer eventProducer;
    private final FileEventConsumer eventConsumer;

    public CompleteIntegrationDemo() {
        logger.info("=== Initializing Complete Integration Demo ===\n");

        // 1. Initialize Database
        logger.info("Step 1: Initializing Database...");
        this.dbManager = DatabaseManager.getInstance();
        if (!dbManager.testConnection()) {
            throw new RuntimeException("Cannot connect to database!");
        }
        logger.info("✓ Database connected\n");

        // 2. Initialize File Repository
        logger.info("Step 2: Initializing File Repository...");
        this.fileRepository = new FileRepository();
        try {
            fileRepository.createTable();
            logger.info("✓ Files table ready\n");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create files table", e);
        }

        // 3. Initialize S3Manager
        logger.info("Step 3: Initializing S3 Storage...");
        this.s3Manager = new S3Manager(
            "http://localhost:9000",
            "minioadmin",
            "minioadmin123",
            "complete-integration-bucket"
        );
        logger.info("✓ S3 Storage ready\n");

        // 4. Initialize Kafka Producer
        logger.info("Step 4: Initializing Kafka Producer...");
        this.eventProducer = new FileEventProducer(
            "localhost:9092",
            "file-events"
        );
        logger.info("✓ Kafka Producer ready\n");

        // 5. Initialize Kafka Consumer
        logger.info("Step 5: Initializing Kafka Consumer...");
        this.eventConsumer = new FileEventConsumer(
            "localhost:9092",
            "file-processor-group",
            "file-events"
        );
        logger.info("✓ Kafka Consumer ready\n");

        logger.info("=== All Systems Initialized! ===\n");
    }

    /**
     * Run complete integration demo
     */
    public void runDemo() {
        logger.info("\n");
        logger.info("╔══════════════════════════════════════════════════════════════╗");
        logger.info("║  COMPLETE INTEGRATION: S3 + Kafka + Database                ║");
        logger.info("║                                                              ║");
        logger.info("║  This demo shows the EXACT architecture used by:            ║");
        logger.info("║  • Dropbox (file storage)                                   ║");
        logger.info("║  • Netflix (video platform)                                 ║");
        logger.info("║  • Instagram (photo sharing)                                ║");
        logger.info("╚══════════════════════════════════════════════════════════════╝");
        logger.info("\n");

        try {
            // Start consumer in background thread
            CountDownLatch consumerReady = new CountDownLatch(1);
            CountDownLatch eventsProcessed = new CountDownLatch(3); // Expect 3 files

            Thread consumerThread = new Thread(() -> {
                consumerReady.countDown();
                eventConsumer.start(event -> {
                    // This is called for EACH file event received!
                    logger.info("\n┌─────────────────────────────────────────────────────────┐");
                    logger.info("│  🎉 FILE EVENT RECEIVED FROM KAFKA!                    │");
                    logger.info("└─────────────────────────────────────────────────────────┘");
                    logger.info("Event Type: {}", event.getEventType());
                    logger.info("File: {}", event.getFileMetadata().getFilename());
                    logger.info("Size: {}", event.getFileMetadata().getFileSizeFormatted());
                    logger.info("Uploaded by: {}", event.getFileMetadata().getUploadedBy());
                    logger.info("S3 URL: {}", event.getFileMetadata().getS3Url());
                    logger.info("Timestamp: {}", new java.util.Date(event.getTimestamp()));

                    // Simulate processing (like generating thumbnails, scanning for viruses, etc.)
                    logger.info("\n→ Processing file...");
                    try {
                        Thread.sleep(500); // Simulate work
                        logger.info("✓ File processed successfully!");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    eventsProcessed.countDown();
                    logger.info("─────────────────────────────────────────────────────────\n");
                });
            });
            consumerThread.start();

            // Wait for consumer to be ready
            consumerReady.await(5, TimeUnit.SECONDS);
            Thread.sleep(2000); // Give Kafka time to assign partitions

            logger.info("=== Starting File Upload Flow ===\n");

            // Create and upload files
            List<File> sampleFiles = createSampleFiles();
            logger.info("✓ Created {} sample files\n", sampleFiles.size());

            for (int i = 0; i < sampleFiles.size(); i++) {
                String username = "user" + ((i % 2) + 1);
                uploadFileWithEvent(sampleFiles.get(i), username);
                Thread.sleep(1000); // Small delay between uploads
            }

            logger.info("\n=== All Files Uploaded! ===");
            logger.info("Files are now:");
            logger.info("  ✓ Stored in S3 (MinIO)");
            logger.info("  ✓ Metadata in PostgreSQL");
            logger.info("  ✓ Events sent to Kafka");
            logger.info("\nWaiting for Kafka consumer to process events...\n");

            // Wait for consumer to process all events
            boolean allProcessed = eventsProcessed.await(30, TimeUnit.SECONDS);

            if (allProcessed) {
                logger.info("\n┌──────────────────────────────────────────────────────────┐");
                logger.info("│  ✅ COMPLETE INTEGRATION SUCCESSFUL!                    │");
                logger.info("└──────────────────────────────────────────────────────────┘");
                logger.info("\nWhat just happened:");
                logger.info("  1. ✅ 3 files uploaded to S3 (MinIO)");
                logger.info("  2. ✅ 3 metadata records saved to PostgreSQL");
                logger.info("  3. ✅ 3 events sent through Kafka");
                logger.info("  4. ✅ 3 events consumed and processed");
                logger.info("\n📊 View your data:");
                logger.info("  • MinIO Console: http://localhost:9001");
                logger.info("  • pgAdmin: http://localhost:5050");
                logger.info("  • Kafka UI: http://localhost:8080");
            } else {
                logger.warn("Timeout waiting for all events to be processed");
            }

            // Show statistics
            logger.info("\n=== Final Statistics ===");
            showStatistics();

            // Stop consumer
            eventConsumer.stop();
            consumerThread.join(5000);

        } catch (Exception e) {
            logger.error("Demo failed", e);
        } finally {
            cleanup();
        }
    }

    /**
     * Upload file to S3, save metadata to DB, and send event to Kafka
     * This is the CORE integration method!
     */
    private void uploadFileWithEvent(File file, String username) throws IOException, SQLException {
        logger.info("┌─────────────────────────────────────────────────────────┐");
        logger.info("│  Uploading: {} (user: {})", file.getName(), username);
        logger.info("└─────────────────────────────────────────────────────────┘");

        // Generate unique ID
        String fileId = UUID.randomUUID().toString();
        String s3Key = "uploads/" + username + "/" + file.getName();

        // STEP 1: Upload to S3
        logger.info("  [1/4] Uploading to S3...");
        String s3Url = s3Manager.uploadFile(file, s3Key);
        logger.info("        ✓ Uploaded to S3: {}", s3Url);

        // STEP 2: Create metadata
        logger.info("  [2/4] Creating metadata...");
        FileMetadata metadata = new FileMetadata();
        metadata.setId(fileId);
        metadata.setFilename(file.getName());
        metadata.setS3Key(s3Key);
        metadata.setS3Url(s3Url);
        metadata.setFileSize(file.length());
        metadata.setContentType("text/plain");
        metadata.setUploadedBy(username);
        metadata.setUploadedAt(System.currentTimeMillis());
        metadata.setDescription("Complete integration demo file");
        logger.info("        ✓ Metadata created");

        // STEP 3: Save to database
        logger.info("  [3/4] Saving metadata to PostgreSQL...");
        fileRepository.insertFileMetadata(metadata);
        logger.info("        ✓ Metadata saved to database");

        // STEP 4: Send event to Kafka
        logger.info("  [4/4] Sending event to Kafka...");
        eventProducer.sendUploadedEvent(metadata);
        logger.info("        ✓ Event sent to Kafka topic 'file-events'");

        logger.info("✅ Complete! File fully integrated into system\n");
    }

    /**
     * Create sample files for demo
     */
    private List<File> createSampleFiles() throws IOException {
        List<File> files = new ArrayList<>();
        Path tempDir = Files.createTempDirectory("complete-integration-demo");

        for (int i = 1; i <= 3; i++) {
            File file = tempDir.resolve("demo-file-" + i + ".txt").toFile();
            String content = String.format(
                "Complete Integration Demo File #%d\n" +
                "========================\n" +
                "This file demonstrates:\n" +
                "- S3 blob storage\n" +
                "- PostgreSQL database\n" +
                "- Kafka event streaming\n" +
                "\n" +
                "Created at: %s\n" +
                "System: Event-Driven Architecture",
                i, new java.util.Date()
            );
            Files.writeString(file.toPath(), content);
            files.add(file);
        }

        return files;
    }

    /**
     * Show system statistics
     */
    private void showStatistics() throws SQLException {
        int fileCount = fileRepository.getFileCount();
        long totalSize = fileRepository.getTotalStorageUsed();
        List<String> s3Files = s3Manager.listFiles();

        logger.info("Files in database: {}", fileCount);
        logger.info("Files in S3: {}", s3Files.size());
        logger.info("Total storage: {} bytes", totalSize);
    }

    /**
     * Cleanup resources
     */
    private void cleanup() {
        logger.info("\n=== Cleaning up resources ===");
        if (eventProducer != null) {
            eventProducer.close();
        }
        if (s3Manager != null) {
            s3Manager.shutdown();
        }
        if (dbManager != null) {
            dbManager.shutdown();
        }
        logger.info("✓ Cleanup complete");
    }

    public static void main(String[] args) {
        logger.info("\n");
        logger.info("╔══════════════════════════════════════════════════════════════╗");
        logger.info("║                                                              ║");
        logger.info("║       COMPLETE CLOUD INTEGRATION DEMO                        ║");
        logger.info("║       S3 + Kafka + PostgreSQL                                ║");
        logger.info("║                                                              ║");
        logger.info("║  This is how modern cloud applications work!                ║");
        logger.info("║                                                              ║");
        logger.info("╚══════════════════════════════════════════════════════════════╝");
        logger.info("\n");

        logger.info("Prerequisites:");
        logger.info("  ✓ Docker services running (docker-compose up -d)");
        logger.info("  ✓ MinIO available at http://localhost:9000");
        logger.info("  ✓ PostgreSQL available at localhost:5432");
        logger.info("  ✓ Kafka available at localhost:9092\n");

        CompleteIntegrationDemo demo = new CompleteIntegrationDemo();
        demo.runDemo();
    }
}
