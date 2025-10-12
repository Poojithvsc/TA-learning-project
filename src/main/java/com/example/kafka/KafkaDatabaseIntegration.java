package com.example.kafka;

import com.example.kafka.consumer.KafkaConsumer;
import com.example.kafka.db.DatabaseManager;
import com.example.kafka.db.MessageRepository;
import com.example.kafka.model.Message;
import com.example.kafka.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * Integrated application that demonstrates Kafka and PostgreSQL working together.
 *
 * Flow:
 * 1. Producer sends messages to Kafka
 * 2. Consumer reads messages from Kafka
 * 3. Consumer saves messages to PostgreSQL database
 * 4. You can query the database to see all saved messages
 */
public class KafkaDatabaseIntegration {
    private static final Logger logger = LoggerFactory.getLogger(KafkaDatabaseIntegration.class);

    private final KafkaProducer producer;
    private final KafkaConsumer consumer;
    private final MessageRepository repository;
    private final DatabaseManager dbManager;

    public KafkaDatabaseIntegration() {
        logger.info("Initializing Kafka-Database Integration...");

        // Initialize database
        this.dbManager = DatabaseManager.getInstance();
        this.repository = new MessageRepository();

        // Create table if it doesn't exist
        try {
            repository.createTable();
            logger.info("✓ Database table ready");
        } catch (SQLException e) {
            logger.error("Failed to create database table", e);
            throw new RuntimeException("Database initialization failed", e);
        }

        // Initialize Kafka components
        this.producer = new KafkaProducer();
        this.consumer = new KafkaConsumer();

        logger.info("✓ Kafka-Database Integration initialized successfully");
    }

    /**
     * Run the demo: send messages via Kafka and save them to database
     */
    public void runDemo() {
        logger.info("\n=== Starting Kafka-Database Integration Demo ===\n");

        // Start consumer in a separate thread that saves messages to database
        Thread consumerThread = new Thread(() -> {
            consumer.start((message, record) -> {
                logger.info("📨 Received from Kafka: {}", message.getContent());

                try {
                    // Save to database
                    repository.insertMessage(message);
                    logger.info("💾 Saved to database: {}", message.getId());

                } catch (SQLException e) {
                    logger.error("❌ Failed to save message to database", e);
                }
            });
        });

        consumerThread.start();
        logger.info("✓ Consumer started and listening...");

        // Give consumer time to subscribe
        sleep(2000);

        // Send sample messages
        logger.info("\n--- Sending messages to Kafka ---");
        for (int i = 1; i <= 5; i++) {
            Message message = new Message();
            message.setId(UUID.randomUUID().toString());
            message.setContent(String.format("Integrated message #%d: Kafka → Database", i));
            message.setTimestamp(System.currentTimeMillis());

            producer.sendMessage(message);
            logger.info("📤 Sent to Kafka: {}", message.getContent());

            sleep(1000); // Wait a bit between messages
        }

        logger.info("\n--- All messages sent! ---");

        // Wait for messages to be processed
        logger.info("⏳ Waiting for messages to be processed...");
        sleep(3000);

        // Query and display messages from database
        displayDatabaseMessages();

        // Shutdown
        logger.info("\n--- Shutting down ---");
        consumer.shutdown();
        producer.close();

        try {
            consumerThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        dbManager.shutdown();
        logger.info("✓ Shutdown complete");
    }

    /**
     * Interactive mode: send custom messages
     */
    public void runInteractive() {
        logger.info("\n=== Kafka-Database Integration - Interactive Mode ===\n");

        Scanner scanner = new Scanner(System.in);

        // Start consumer in background
        Thread consumerThread = new Thread(() -> {
            consumer.start((message, record) -> {
                logger.info("📨 Received from Kafka: {}", message.getContent());

                try {
                    repository.insertMessage(message);
                    logger.info("💾 Saved to database");
                } catch (SQLException e) {
                    logger.error("❌ Failed to save message", e);
                }
            });
        });

        consumerThread.start();
        logger.info("✓ Consumer started\n");

        sleep(2000);

        while (true) {
            System.out.println("\n=== Menu ===");
            System.out.println("1. Send a message");
            System.out.println("2. View all messages from database");
            System.out.println("3. View message count");
            System.out.println("4. Exit");
            System.out.print("Choose option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    sendCustomMessage(scanner);
                    break;

                case "2":
                    displayDatabaseMessages();
                    break;

                case "3":
                    displayMessageCount();
                    break;

                case "4":
                    logger.info("Shutting down...");
                    consumer.shutdown();
                    producer.close();
                    try {
                        consumerThread.join(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    dbManager.shutdown();
                    scanner.close();
                    return;

                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void sendCustomMessage(Scanner scanner) {
        System.out.print("Enter your message: ");
        String content = scanner.nextLine();

        if (content.trim().isEmpty()) {
            System.out.println("Message cannot be empty!");
            return;
        }

        Message message = new Message();
        message.setId(UUID.randomUUID().toString());
        message.setContent(content);
        message.setTimestamp(System.currentTimeMillis());

        producer.sendMessage(message);
        logger.info("📤 Message sent to Kafka!");

        // Give it a moment to be processed
        sleep(1500);
    }

    private void displayDatabaseMessages() {
        logger.info("\n--- Messages in Database ---");

        try {
            List<Message> messages = repository.getAllMessages();

            if (messages.isEmpty()) {
                logger.info("No messages found in database.");
                return;
            }

            logger.info("Total messages: {}\n", messages.size());

            for (int i = 0; i < messages.size(); i++) {
                Message msg = messages.get(i);
                logger.info("{}. [{}] {}",
                    i + 1,
                    msg.getId().substring(0, 8) + "...",
                    msg.getContent());
            }

        } catch (SQLException e) {
            logger.error("Failed to retrieve messages from database", e);
        }
    }

    private void displayMessageCount() {
        try {
            int count = repository.getMessageCount();
            logger.info("\n📊 Total messages in database: {}", count);
            logger.info("📊 Connection pool stats: {}", dbManager.getPoolStats());
        } catch (SQLException e) {
            logger.error("Failed to get message count", e);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        logger.info("=== Kafka + PostgreSQL Integration Application ===\n");

        // Test database connection first
        DatabaseManager dbManager = DatabaseManager.getInstance();
        if (!dbManager.testConnection()) {
            logger.error("❌ Cannot connect to database. Please ensure PostgreSQL is running.");
            logger.error("   Run: docker-compose up -d postgres");
            System.exit(1);
        }

        logger.info("✓ Database connection successful\n");

        KafkaDatabaseIntegration app = new KafkaDatabaseIntegration();

        if (args.length > 0 && args[0].equals("--demo")) {
            app.runDemo();
        } else {
            app.runInteractive();
        }
    }
}
