package com.example.kafka.db;

import com.example.kafka.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing file event history in the database
 * Tracks uploads, downloads, and deletions
 */
public class FileEventRepository {
    private static final Logger logger = LoggerFactory.getLogger(FileEventRepository.class);
    private final DataSource dataSource;

    public FileEventRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        createTableIfNotExists();
    }

    /**
     * Create the file_events table if it doesn't exist
     */
    private void createTableIfNotExists() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS file_events (
                id SERIAL PRIMARY KEY,
                file_id VARCHAR(255) NOT NULL,
                event_type VARCHAR(50) NOT NULL,
                filename VARCHAR(500) NOT NULL,
                user_name VARCHAR(255),
                file_size BIGINT,
                event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                details TEXT,
                FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE
            )
            """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSQL);
            logger.info("file_events table created or already exists");

            // Create index for better query performance
            String createIndexSQL = "CREATE INDEX IF NOT EXISTS idx_file_events_file_id ON file_events(file_id)";
            stmt.execute(createIndexSQL);

            String createEventTypeIndexSQL = "CREATE INDEX IF NOT EXISTS idx_file_events_type ON file_events(event_type)";
            stmt.execute(createEventTypeIndexSQL);

        } catch (SQLException e) {
            logger.error("Error creating file_events table", e);
            throw new RuntimeException("Failed to create file_events table", e);
        }
    }

    /**
     * Record a file event (upload, download, delete)
     */
    public void recordEvent(String fileId, String eventType, String filename,
                          String userName, long fileSize, String details) {
        String sql = """
            INSERT INTO file_events (file_id, event_type, filename, user_name, file_size, details)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fileId);
            pstmt.setString(2, eventType);
            pstmt.setString(3, filename);
            pstmt.setString(4, userName);
            pstmt.setLong(5, fileSize);
            pstmt.setString(6, details);

            pstmt.executeUpdate();
            logger.info("Recorded {} event for file: {} (user: {})", eventType, filename, userName);

        } catch (SQLException e) {
            logger.error("Error recording file event", e);
        }
    }

    /**
     * Record event from FileMetadata
     */
    public void recordEvent(FileMetadata metadata, String eventType) {
        recordEvent(
            metadata.getId(),
            eventType,
            metadata.getFilename(),
            metadata.getUploadedBy(),
            metadata.getFileSize(),
            metadata.getDescription()
        );
    }

    /**
     * Get all events for a specific file
     */
    public List<FileEvent> getFileEvents(String fileId) {
        String sql = """
            SELECT id, file_id, event_type, filename, user_name, file_size, event_timestamp, details
            FROM file_events
            WHERE file_id = ?
            ORDER BY event_timestamp DESC
            """;

        List<FileEvent> events = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fileId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            logger.error("Error fetching file events", e);
        }

        return events;
    }

    /**
     * Get all events of a specific type
     */
    public List<FileEvent> getEventsByType(String eventType) {
        String sql = """
            SELECT id, file_id, event_type, filename, user_name, file_size, event_timestamp, details
            FROM file_events
            WHERE event_type = ?
            ORDER BY event_timestamp DESC
            """;

        List<FileEvent> events = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, eventType);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            logger.error("Error fetching events by type", e);
        }

        return events;
    }

    /**
     * Get recent events (last N events)
     */
    public List<FileEvent> getRecentEvents(int limit) {
        String sql = """
            SELECT id, file_id, event_type, filename, user_name, file_size, event_timestamp, details
            FROM file_events
            ORDER BY event_timestamp DESC
            LIMIT ?
            """;

        List<FileEvent> events = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }

        } catch (SQLException e) {
            logger.error("Error fetching recent events", e);
        }

        return events;
    }

    /**
     * Get download count for a specific file
     */
    public int getDownloadCount(String fileId) {
        String sql = "SELECT COUNT(*) FROM file_events WHERE file_id = ? AND event_type = 'DOWNLOADED'";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fileId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            logger.error("Error getting download count", e);
        }

        return 0;
    }

    /**
     * Map ResultSet to FileEvent object
     */
    private FileEvent mapResultSetToEvent(ResultSet rs) throws SQLException {
        FileEvent event = new FileEvent();
        event.setId(rs.getInt("id"));
        event.setFileId(rs.getString("file_id"));
        event.setEventType(rs.getString("event_type"));
        event.setFilename(rs.getString("filename"));
        event.setUserName(rs.getString("user_name"));
        event.setFileSize(rs.getLong("file_size"));
        event.setEventTimestamp(rs.getTimestamp("event_timestamp"));
        event.setDetails(rs.getString("details"));
        return event;
    }

    /**
     * FileEvent model class
     */
    public static class FileEvent {
        private int id;
        private String fileId;
        private String eventType;
        private String filename;
        private String userName;
        private long fileSize;
        private Timestamp eventTimestamp;
        private String details;

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getFileId() { return fileId; }
        public void setFileId(String fileId) { this.fileId = fileId; }

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }

        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        public Timestamp getEventTimestamp() { return eventTimestamp; }
        public void setEventTimestamp(Timestamp eventTimestamp) { this.eventTimestamp = eventTimestamp; }

        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }

        @Override
        public String toString() {
            return String.format("[%s] %s - %s by %s at %s",
                eventType, filename, fileSize, userName, eventTimestamp);
        }
    }
}
