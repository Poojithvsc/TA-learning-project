package com.example.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FileMetadata represents information ABOUT a file stored in S3.
 *
 * Key Concept: Separation of File and Metadata
 * ============================================
 * - The ACTUAL FILE is stored in S3 (blob storage)
 * - The METADATA (info about the file) is stored in PostgreSQL database
 *
 * Why separate?
 * - Databases are expensive for large files
 * - S3 is optimized for file storage
 * - Database is fast for searching/querying metadata
 *
 * Example:
 * --------
 * File: photo.jpg (5MB) → Stored in S3
 * Metadata in Database:
 *   - filename: "photo.jpg"
 *   - fileSize: 5242880 bytes
 *   - s3Url: "http://localhost:9000/mybucket/photo.jpg"
 *   - uploadedBy: "user123"
 *   - uploadedAt: 1696500000000
 */
public class FileMetadata {

    @JsonProperty("id")
    private String id;  // Unique identifier (UUID)

    @JsonProperty("filename")
    private String filename;  // Original filename (e.g., "document.pdf")

    @JsonProperty("s3Key")
    private String s3Key;  // Path in S3 (e.g., "uploads/2024/document.pdf")

    @JsonProperty("s3Url")
    private String s3Url;  // Full URL to access file

    @JsonProperty("fileSize")
    private long fileSize;  // Size in bytes

    @JsonProperty("contentType")
    private String contentType;  // MIME type (e.g., "image/jpeg", "application/pdf")

    @JsonProperty("uploadedBy")
    private String uploadedBy;  // Who uploaded it

    @JsonProperty("uploadedAt")
    private long uploadedAt;  // When it was uploaded (timestamp)

    @JsonProperty("description")
    private String description;  // Optional description

    // Constructors
    public FileMetadata() {
    }

    public FileMetadata(String id, String filename, String s3Key, String s3Url,
                        long fileSize, String contentType, String uploadedBy, long uploadedAt) {
        this.id = id;
        this.filename = filename;
        this.s3Key = s3Key;
        this.s3Url = s3Url;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getS3Url() {
        return s3Url;
    }

    public void setS3Url(String s3Url) {
        this.s3Url = s3Url;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public long getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(long uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Helper methods
    public String getFileSizeFormatted() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "id='" + id + '\'' +
                ", filename='" + filename + '\'' +
                ", s3Key='" + s3Key + '\'' +
                ", fileSize=" + getFileSizeFormatted() +
                ", contentType='" + contentType + '\'' +
                ", uploadedBy='" + uploadedBy + '\'' +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
}
