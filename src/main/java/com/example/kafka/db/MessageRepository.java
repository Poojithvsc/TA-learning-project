package com.example.kafka.db;

import com.example.kafka.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MessageRepository handles all database operations for Message entities.
 * This class demonstrates CRUD operations with PostgreSQL.
 */
public class MessageRepository {
    private static final Logger logger = LoggerFactory.getLogger(MessageRepository.class);
    private final DatabaseManager dbManager;

    public MessageRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Create the messages table if it doesn't exist
     */
    public void createTable() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS messages (
                id SERIAL PRIMARY KEY,
                message_id VARCHAR(255) UNIQUE NOT NULL,
                content TEXT NOT NULL,
                timestamp BIGINT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            logger.info("Messages table created or already exists");
        } catch (SQLException e) {
            logger.error("Failed to create messages table", e);
            throw e;
        }
    }

    /**
     * Insert a message into the database
     */
    public void insertMessage(Message message) throws SQLException {
        String insertSQL = """
            INSERT INTO messages (message_id, content, timestamp)
            VALUES (?, ?, ?)
            ON CONFLICT (message_id) DO UPDATE
            SET content = EXCLUDED.content,
                timestamp = EXCLUDED.timestamp
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, message.getId());
            pstmt.setString(2, message.getContent());
            pstmt.setLong(3, message.getTimestamp());

            int rowsAffected = pstmt.executeUpdate();
            logger.debug("Inserted message with ID: {}, rows affected: {}",
                        message.getId(), rowsAffected);

        } catch (SQLException e) {
            logger.error("Failed to insert message: {}", message.getId(), e);
            throw e;
        }
    }

    /**
     * Retrieve a message by its ID
     */
    public Message getMessageById(String messageId) throws SQLException {
        String selectSQL = "SELECT * FROM messages WHERE message_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setString(1, messageId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMessage(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve message: {}", messageId, e);
            throw e;
        }

        return null;
    }

    /**
     * Retrieve all messages
     */
    public List<Message> getAllMessages() throws SQLException {
        String selectSQL = "SELECT * FROM messages ORDER BY created_at DESC";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {

            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }

            logger.debug("Retrieved {} messages from database", messages.size());

        } catch (SQLException e) {
            logger.error("Failed to retrieve all messages", e);
            throw e;
        }

        return messages;
    }

    /**
     * Retrieve messages with limit
     */
    public List<Message> getRecentMessages(int limit) throws SQLException {
        String selectSQL = "SELECT * FROM messages ORDER BY created_at DESC LIMIT ?";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }

            logger.debug("Retrieved {} recent messages from database", messages.size());

        } catch (SQLException e) {
            logger.error("Failed to retrieve recent messages", e);
            throw e;
        }

        return messages;
    }

    /**
     * Delete a message by ID
     */
    public boolean deleteMessage(String messageId) throws SQLException {
        String deleteSQL = "DELETE FROM messages WHERE message_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {

            pstmt.setString(1, messageId);
            int rowsAffected = pstmt.executeUpdate();

            logger.debug("Deleted message: {}, rows affected: {}", messageId, rowsAffected);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Failed to delete message: {}", messageId, e);
            throw e;
        }
    }

    /**
     * Get count of all messages
     */
    public int getMessageCount() throws SQLException {
        String countSQL = "SELECT COUNT(*) FROM messages";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSQL)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            logger.error("Failed to get message count", e);
            throw e;
        }

        return 0;
    }

    /**
     * Delete all messages (use with caution!)
     */
    public void deleteAllMessages() throws SQLException {
        String deleteSQL = "DELETE FROM messages";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            int rowsAffected = stmt.executeUpdate(deleteSQL);
            logger.info("Deleted all messages, rows affected: {}", rowsAffected);

        } catch (SQLException e) {
            logger.error("Failed to delete all messages", e);
            throw e;
        }
    }

    /**
     * Helper method to map ResultSet to Message object
     */
    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getString("message_id"));
        message.setContent(rs.getString("content"));
        message.setTimestamp(rs.getLong("timestamp"));
        return message;
    }
}
