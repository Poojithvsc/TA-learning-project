package com.example.kafka.db;

import com.example.kafka.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FileRepository handles all database operations for file metadata.
 *
 * Remember: We store METADATA (info about files) in the database,
 * not the actual files themselves. Files are stored in S3.
 */
public class FileRepository {
    private static final Logger logger = LoggerFactory.getLogger(FileRepository.class);
    private final DatabaseManager dbManager;

    public FileRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Create the files table if it doesn't exist
     */
    public void createTable() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS files (
                id VARCHAR(255) PRIMARY KEY,
                filename VARCHAR(500) NOT NULL,
                s3_key VARCHAR(1000) NOT NULL,
                s3_url VARCHAR(1000) NOT NULL,
                file_size BIGINT NOT NULL,
                content_type VARCHAR(255),
                uploaded_by VARCHAR(255),
                uploaded_at BIGINT NOT NULL,
                description TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            logger.info("Files table created or already exists");
        } catch (SQLException e) {
            logger.error("Failed to create files table", e);
            throw e;
        }
    }

    /**
     * Insert file metadata into database
     */
    public void insertFileMetadata(FileMetadata metadata) throws SQLException {
        String insertSQL = """
            INSERT INTO files (id, filename, s3_key, s3_url, file_size,
                              content_type, uploaded_by, uploaded_at, description)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE
            SET filename = EXCLUDED.filename,
                s3_url = EXCLUDED.s3_url,
                file_size = EXCLUDED.file_size
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, metadata.getId());
            pstmt.setString(2, metadata.getFilename());
            pstmt.setString(3, metadata.getS3Key());
            pstmt.setString(4, metadata.getS3Url());
            pstmt.setLong(5, metadata.getFileSize());
            pstmt.setString(6, metadata.getContentType());
            pstmt.setString(7, metadata.getUploadedBy());
            pstmt.setLong(8, metadata.getUploadedAt());
            pstmt.setString(9, metadata.getDescription());

            int rowsAffected = pstmt.executeUpdate();
            logger.debug("Inserted file metadata: {}, rows affected: {}",
                        metadata.getFilename(), rowsAffected);

        } catch (SQLException e) {
            logger.error("Failed to insert file metadata: {}", metadata.getFilename(), e);
            throw e;
        }
    }

    /**
     * Get file metadata by ID
     */
    public FileMetadata getFileById(String id) throws SQLException {
        String selectSQL = "SELECT * FROM files WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFileMetadata(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve file by ID: {}", id, e);
            throw e;
        }

        return null;
    }

    /**
     * Get file metadata by filename
     */
    public FileMetadata getFileByFilename(String filename) throws SQLException {
        String selectSQL = "SELECT * FROM files WHERE filename = ? ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setString(1, filename);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFileMetadata(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve file by filename: {}", filename, e);
            throw e;
        }

        return null;
    }

    /**
     * Get all files
     */
    public List<FileMetadata> getAllFiles() throws SQLException {
        String selectSQL = "SELECT * FROM files ORDER BY created_at DESC";
        List<FileMetadata> files = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {

            while (rs.next()) {
                files.add(mapResultSetToFileMetadata(rs));
            }

            logger.debug("Retrieved {} files from database", files.size());

        } catch (SQLException e) {
            logger.error("Failed to retrieve all files", e);
            throw e;
        }

        return files;
    }

    /**
     * Get files by user
     */
    public List<FileMetadata> getFilesByUser(String username) throws SQLException {
        String selectSQL = "SELECT * FROM files WHERE uploaded_by = ? ORDER BY created_at DESC";
        List<FileMetadata> files = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    files.add(mapResultSetToFileMetadata(rs));
                }
            }

            logger.debug("Retrieved {} files for user: {}", files.size(), username);

        } catch (SQLException e) {
            logger.error("Failed to retrieve files for user: {}", username, e);
            throw e;
        }

        return files;
    }

    /**
     * Search files by filename pattern
     */
    public List<FileMetadata> searchFiles(String searchTerm) throws SQLException {
        String selectSQL = "SELECT * FROM files WHERE filename LIKE ? ORDER BY created_at DESC";
        List<FileMetadata> files = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setString(1, "%" + searchTerm + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    files.add(mapResultSetToFileMetadata(rs));
                }
            }

            logger.debug("Found {} files matching search: {}", files.size(), searchTerm);

        } catch (SQLException e) {
            logger.error("Failed to search files", e);
            throw e;
        }

        return files;
    }

    /**
     * Get recent files with limit
     */
    public List<FileMetadata> getRecentFiles(int limit) throws SQLException {
        String selectSQL = "SELECT * FROM files ORDER BY created_at DESC LIMIT ?";
        List<FileMetadata> files = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    files.add(mapResultSetToFileMetadata(rs));
                }
            }

            logger.debug("Retrieved {} recent files", files.size());

        } catch (SQLException e) {
            logger.error("Failed to retrieve recent files", e);
            throw e;
        }

        return files;
    }

    /**
     * Delete file metadata
     */
    public boolean deleteFileMetadata(String id) throws SQLException {
        String deleteSQL = "DELETE FROM files WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {

            pstmt.setString(1, id);
            int rowsAffected = pstmt.executeUpdate();

            logger.debug("Deleted file metadata: {}, rows affected: {}", id, rowsAffected);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Failed to delete file metadata: {}", id, e);
            throw e;
        }
    }

    /**
     * Get total file count
     */
    public int getFileCount() throws SQLException {
        String countSQL = "SELECT COUNT(*) FROM files";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSQL)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            logger.error("Failed to get file count", e);
            throw e;
        }

        return 0;
    }

    /**
     * Get total storage used (sum of all file sizes)
     */
    public long getTotalStorageUsed() throws SQLException {
        String sumSQL = "SELECT SUM(file_size) FROM files";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sumSQL)) {

            if (rs.next()) {
                return rs.getLong(1);
            }

        } catch (SQLException e) {
            logger.error("Failed to get total storage", e);
            throw e;
        }

        return 0;
    }

    /**
     * Helper method to map ResultSet to FileMetadata object
     */
    private FileMetadata mapResultSetToFileMetadata(ResultSet rs) throws SQLException {
        FileMetadata metadata = new FileMetadata();
        metadata.setId(rs.getString("id"));
        metadata.setFilename(rs.getString("filename"));
        metadata.setS3Key(rs.getString("s3_key"));
        metadata.setS3Url(rs.getString("s3_url"));
        metadata.setFileSize(rs.getLong("file_size"));
        metadata.setContentType(rs.getString("content_type"));
        metadata.setUploadedBy(rs.getString("uploaded_by"));
        metadata.setUploadedAt(rs.getLong("uploaded_at"));
        metadata.setDescription(rs.getString("description"));
        return metadata;
    }
}
