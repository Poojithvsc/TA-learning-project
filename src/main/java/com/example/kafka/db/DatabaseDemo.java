package com.example.kafka.db;

import com.example.kafka.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * DatabaseDemo demonstrates how to use the database connection and repository.
 * This class shows basic CRUD operations with PostgreSQL.
 */
public class DatabaseDemo {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseDemo.class);

    public static void main(String[] args) {
        DatabaseDemo demo = new DatabaseDemo();
        demo.run();
    }

    public void run() {
        logger.info("Starting Database Demo...");

        DatabaseManager dbManager = DatabaseManager.getInstance();
        MessageRepository repository = new MessageRepository();

        try {
            // Test database connection
            logger.info("Testing database connection...");
            if (dbManager.testConnection()) {
                logger.info("✓ Database connection successful!");
                logger.info("Connection pool stats: {}", dbManager.getPoolStats());
            } else {
                logger.error("✗ Database connection failed!");
                return;
            }

            // Create table
            logger.info("\n--- Creating table ---");
            repository.createTable();
            logger.info("✓ Table created successfully");

            // Insert sample messages
            logger.info("\n--- Inserting sample messages ---");
            for (int i = 1; i <= 5; i++) {
                Message message = new Message();
                message.setId(UUID.randomUUID().toString());
                message.setContent("Sample message #" + i + " from database demo");
                message.setTimestamp(System.currentTimeMillis());

                repository.insertMessage(message);
                logger.info("✓ Inserted message: {}", message.getContent());
            }

            // Get message count
            logger.info("\n--- Message count ---");
            int count = repository.getMessageCount();
            logger.info("Total messages in database: {}", count);

            // Retrieve all messages
            logger.info("\n--- Retrieving all messages ---");
            List<Message> allMessages = repository.getAllMessages();
            logger.info("Retrieved {} messages:", allMessages.size());
            for (Message msg : allMessages) {
                logger.info("  - ID: {}, Content: {}, Timestamp: {}",
                           msg.getId(), msg.getContent(), msg.getTimestamp());
            }

            // Retrieve recent messages
            logger.info("\n--- Retrieving recent 3 messages ---");
            List<Message> recentMessages = repository.getRecentMessages(3);
            logger.info("Retrieved {} recent messages:", recentMessages.size());
            for (Message msg : recentMessages) {
                logger.info("  - {}", msg.getContent());
            }

            // Update a message (upsert)
            logger.info("\n--- Updating a message ---");
            if (!allMessages.isEmpty()) {
                Message firstMessage = allMessages.get(0);
                firstMessage.setContent("UPDATED: " + firstMessage.getContent());
                repository.insertMessage(firstMessage);
                logger.info("✓ Updated message: {}", firstMessage.getId());
            }

            // Retrieve a specific message
            logger.info("\n--- Retrieving specific message ---");
            if (!allMessages.isEmpty()) {
                String messageId = allMessages.get(0).getId();
                Message retrieved = repository.getMessageById(messageId);
                if (retrieved != null) {
                    logger.info("✓ Retrieved message: {}", retrieved.getContent());
                }
            }

            // Delete a message
            logger.info("\n--- Deleting a message ---");
            if (allMessages.size() > 1) {
                String messageId = allMessages.get(1).getId();
                boolean deleted = repository.deleteMessage(messageId);
                if (deleted) {
                    logger.info("✓ Deleted message: {}", messageId);
                }
            }

            // Final count
            logger.info("\n--- Final message count ---");
            int finalCount = repository.getMessageCount();
            logger.info("Total messages after deletion: {}", finalCount);

            logger.info("\n--- Connection pool stats ---");
            logger.info(dbManager.getPoolStats());

        } catch (SQLException e) {
            logger.error("Database error occurred", e);
        } finally {
            // Shutdown connection pool
            logger.info("\n--- Shutting down ---");
            dbManager.shutdown();
            logger.info("✓ Database demo completed");
        }
    }
}
