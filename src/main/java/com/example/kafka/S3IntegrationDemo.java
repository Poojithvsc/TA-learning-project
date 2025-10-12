package com.example.kafka;

import com.example.kafka.db.DatabaseManager;
import com.example.kafka.db.FileRepository;
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

/**
 * S3IntegrationDemo - Demonstrates complete S3 + Database integration
 *
 * What this demo does:
 * 1. Creates sample text files
 * 2. Uploads them to S3 (MinIO)
 * 3. Saves file metadata to PostgreSQL database
 * 4. Shows how to query and retrieve files
 *
 * This is the foundation for:
 * - Document management systems
 * - Photo sharing apps
 * - Video platforms
 * - Backup systems
 */
public class S3IntegrationDemo {
    private static final Logger logger = LoggerFactory.getLogger(S3IntegrationDemo.class);

    private final S3Manager s3Manager;
    private final FileRepository fileRepository;
    private final DatabaseManager dbManager;

    public S3IntegrationDemo() {
        logger.info("=== Initializing S3 Integration Demo ===\n");

        // Initialize Database
        this.dbManager = DatabaseManager.getInstance();
        if (!dbManager.testConnection()) {
            throw new RuntimeException("Cannot connect to database!");
        }
        logger.info("✓ Database connection successful");

        // Initialize File Repository
        this.fileRepository = new FileRepository();
        try {
            fileRepository.createTable();
            logger.info("✓ Files table ready");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create files table", e);
        }

        // Initialize S3Manager
        this.s3Manager = new S3Manager(
                "http://localhost:9000",  // MinIO endpoint
                "minioadmin",              // Access key
                "minioadmin123",           // Secret key
                "demo-bucket"              // Bucket name
        );
        logger.info("✓ S3Manager initialized\n");
    }

    /**
     * Run the complete demo
     */
    public void runDemo() {
        logger.info("=== Starting S3 + Database Integration Demo ===\n");

        try {
            // STEP 1: Create sample files
            logger.info("--- Step 1: Creating sample files ---");
            List<File> sampleFiles = createSampleFiles();
            logger.info("✓ Created {} sample files\n", sampleFiles.size());

            // STEP 2: Upload files to S3 and save metadata to database
            logger.info("--- Step 2: Uploading files to S3 and saving metadata ---");
            for (int i = 0; i < sampleFiles.size(); i++) {
                uploadAndSaveFile(sampleFiles.get(i), "user" + (i % 2 + 1));
            }
            logger.info("✓ All files uploaded and metadata saved\n");

            // STEP 3: Query database to see all files
            logger.info("--- Step 3: Querying database for files ---");
            displayAllFiles();

            // STEP 4: Search for specific files
            logger.info("\n--- Step 4: Searching for files ---");
            searchAndDisplay("sample");

            // STEP 5: Get files by user
            logger.info("\n--- Step 5: Getting files by user ---");
            displayFilesByUser("user1");

            // STEP 6: Show storage statistics
            logger.info("\n--- Step 6: Storage statistics ---");
            displayStatistics();

            // STEP 7: Download a file from S3
            logger.info("\n--- Step 7: Downloading a file from S3 ---");
            demonstrateDownload();

            logger.info("\n=== Demo completed successfully! ===");
            logger.info("\nWhat you just did:");
            logger.info("✓ Uploaded files to S3 (blob storage)");
            logger.info("✓ Saved file metadata in PostgreSQL database");
            logger.info("✓ Queried and searched files in database");
            logger.info("✓ Downloaded files from S3");
            logger.info("\nThis is the same architecture used by Dropbox, Google Drive, etc!");

        } catch (Exception e) {
            logger.error("Demo failed", e);
        } finally {
            // Cleanup
            s3Manager.shutdown();
            dbManager.shutdown();
        }
    }

    /**
     * Create sample text files for demo
     */
    private List<File> createSampleFiles() throws IOException {
        List<File> files = new ArrayList<>();
        Path tempDir = Files.createTempDirectory("s3-demo");

        for (int i = 1; i <= 5; i++) {
            File file = tempDir.resolve("sample-file-" + i + ".txt").toFile();
            String content = String.format("This is sample file #%d\nUploaded to S3 for demo\nTimestamp: %d",
                    i, System.currentTimeMillis());
            Files.writeString(file.toPath(), content);
            files.add(file);
            logger.info("  Created: {}", file.getName());
        }

        return files;
    }

    /**
     * Upload file to S3 and save metadata to database
     */
    private void uploadAndSaveFile(File file, String username) throws IOException, SQLException {
        // Generate unique ID
        String fileId = UUID.randomUUID().toString();

        // Generate S3 key (path in S3)
        String s3Key = "uploads/" + username + "/" + file.getName();

        logger.info("  Uploading: {} → S3", file.getName());

        // Upload to S3
        String s3Url = s3Manager.uploadFile(file, s3Key);

        // Create metadata object
        FileMetadata metadata = new FileMetadata();
        metadata.setId(fileId);
        metadata.setFilename(file.getName());
        metadata.setS3Key(s3Key);
        metadata.setS3Url(s3Url);
        metadata.setFileSize(file.length());
        metadata.setContentType("text/plain");
        metadata.setUploadedBy(username);
        metadata.setUploadedAt(System.currentTimeMillis());
        metadata.setDescription("Sample file for S3 integration demo");

        // Save metadata to database
        fileRepository.insertFileMetadata(metadata);

        logger.info("    ✓ Uploaded to S3: {}", s3Url);
        logger.info("    ✓ Metadata saved to database");
    }

    /**
     * Display all files from database
     */
    private void displayAllFiles() throws SQLException {
        List<FileMetadata> files = fileRepository.getAllFiles();

        logger.info("Found {} files in database:\n", files.size());
        for (int i = 0; i < files.size(); i++) {
            FileMetadata file = files.get(i);
            logger.info("  {}. {}", i + 1, file.getFilename());
            logger.info("     Size: {}", file.getFileSizeFormatted());
            logger.info("     Uploaded by: {}", file.getUploadedBy());
            logger.info("     S3 URL: {}", file.getS3Url());
        }
    }

    /**
     * Search files by filename
     */
    private void searchAndDisplay(String searchTerm) throws SQLException {
        List<FileMetadata> results = fileRepository.searchFiles(searchTerm);
        logger.info("Search results for '{}': {} files found", searchTerm, results.size());

        for (FileMetadata file : results) {
            logger.info("  - {} ({})", file.getFilename(), file.getFileSizeFormatted());
        }
    }

    /**
     * Display files by specific user
     */
    private void displayFilesByUser(String username) throws SQLException {
        List<FileMetadata> files = fileRepository.getFilesByUser(username);
        logger.info("Files uploaded by '{}': {} files", username, files.size());

        for (FileMetadata file : files) {
            logger.info("  - {}", file.getFilename());
        }
    }

    /**
     * Display storage statistics
     */
    private void displayStatistics() throws SQLException {
        int fileCount = fileRepository.getFileCount();
        long totalSize = fileRepository.getTotalStorageUsed();

        logger.info("Total files: {}", fileCount);
        logger.info("Total storage: {} bytes ({} MB)",
                totalSize, String.format("%.2f", totalSize / (1024.0 * 1024.0)));

        // List files in S3
        List<String> s3Files = s3Manager.listFiles();
        logger.info("Files in S3 bucket: {}", s3Files.size());
    }

    /**
     * Demonstrate downloading a file from S3
     */
    private void demonstrateDownload() throws SQLException, IOException {
        List<FileMetadata> files = fileRepository.getRecentFiles(1);

        if (files.isEmpty()) {
            logger.info("No files to download");
            return;
        }

        FileMetadata file = files.get(0);
        logger.info("Downloading: {}", file.getFilename());

        // Download to temp directory
        Path downloadPath = Files.createTempFile("downloaded-", "-" + file.getFilename());
        s3Manager.downloadFile(file.getS3Key(), downloadPath);

        logger.info("✓ Downloaded to: {}", downloadPath);
        logger.info("File size: {}", Files.size(downloadPath) + " bytes");

        // Read and display content
        String content = Files.readString(downloadPath);
        logger.info("Content preview:\n{}", content.substring(0, Math.min(100, content.length())));
    }

    public static void main(String[] args) {
        logger.info("\n");
        logger.info("╔════════════════════════════════════════════════════════════╗");
        logger.info("║   S3 (Blob Storage) + Kafka + Database Integration       ║");
        logger.info("║                                                            ║");
        logger.info("║   This demo shows how to store files in S3 and            ║");
        logger.info("║   track them in a database - just like Dropbox!           ║");
        logger.info("╚════════════════════════════════════════════════════════════╝");
        logger.info("\n");

        // Check if MinIO is running
        logger.info("Prerequisites:");
        logger.info("  1. Docker services running (docker-compose up -d)");
        logger.info("  2. MinIO available at http://localhost:9000");
        logger.info("  3. PostgreSQL available at localhost:5432\n");

        S3IntegrationDemo demo = new S3IntegrationDemo();
        demo.runDemo();
    }
}
