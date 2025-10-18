package com.example.kafka;

import com.example.kafka.db.DatabaseManager;
import com.example.kafka.db.FileRepository;
import com.example.kafka.db.FileEventRepository;
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
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * InteractiveFileManager - Upload and Download YOUR OWN files!
 *
 * This application lets you:
 * 1. Upload files from your computer
 * 2. View all uploaded files
 * 3. Download files when you need them
 * 4. Search for files
 * 5. See real-time Kafka events
 *
 * All integrated with S3, PostgreSQL, and Kafka!
 */
public class InteractiveFileManager {
    private static final Logger logger = LoggerFactory.getLogger(InteractiveFileManager.class);

    private final S3Manager s3Manager;
    private final FileRepository fileRepository;
    private final FileEventRepository eventRepository;
    private final DatabaseManager dbManager;
    private final FileEventProducer eventProducer;
    private final FileEventConsumer eventConsumer;
    private final Scanner scanner;
    private Thread consumerThread;

    public InteractiveFileManager() {
        this.scanner = new Scanner(System.in);

        logger.info("=== Initializing Interactive File Manager ===\n");

        // Initialize Database
        logger.info("Connecting to PostgreSQL...");
        this.dbManager = DatabaseManager.getInstance();
        if (!dbManager.testConnection()) {
            throw new RuntimeException("Cannot connect to database!");
        }
        logger.info("✓ Database connected\n");

        // Initialize File Repository
        this.fileRepository = new FileRepository();
        try {
            fileRepository.createTable();
            logger.info("✓ Files table ready\n");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create files table", e);
        }

        // Initialize Event Repository
        this.eventRepository = new FileEventRepository(dbManager.getDataSource());
        logger.info("✓ Event tracking ready\n");

        // Initialize S3
        logger.info("Connecting to MinIO (S3)...");
        this.s3Manager = new S3Manager(
            "http://localhost:9000",
            "minioadmin",
            "minioadmin123",
            "my-files"
        );
        logger.info("✓ S3 Storage ready\n");

        // Initialize Kafka Producer
        logger.info("Connecting to Kafka...");
        this.eventProducer = new FileEventProducer(
            "localhost:9092",
            "file-events"
        );
        logger.info("✓ Kafka Producer ready\n");

        // Initialize Kafka Consumer
        this.eventConsumer = new FileEventConsumer(
            "localhost:9092",
            "file-manager-group",
            "file-events"
        );

        // Start consumer in background
        startEventConsumer();

        logger.info("=== All Systems Ready! ===\n");
    }

    /**
     * Start Kafka consumer in background to show real-time events
     */
    private void startEventConsumer() {
        consumerThread = new Thread(() -> {
            eventConsumer.start(event -> {
                // Save event to database
                eventRepository.recordEvent(
                    event.getFileMetadata(),
                    event.getEventType()
                );

                // Display event notification
                System.out.println("\n╔════════════════════════════════════════════════════════╗");
                System.out.println("║  📨 KAFKA EVENT RECEIVED                              ║");
                System.out.println("╚════════════════════════════════════════════════════════╝");
                System.out.println("  Event: " + event.getEventType());
                System.out.println("  File: " + event.getFileMetadata().getFilename());
                System.out.println("  Size: " + event.getFileMetadata().getFileSizeFormatted());
                System.out.println("  User: " + event.getFileMetadata().getUploadedBy());
                System.out.println("  Time: " + new java.util.Date(event.getTimestamp()));
                System.out.println("  ✓ Saved to database");
                System.out.println("════════════════════════════════════════════════════════\n");
            });
        });
        consumerThread.setDaemon(true);
        consumerThread.start();

        try {
            Thread.sleep(2000); // Give Kafka time to initialize
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Main interactive menu
     */
    public void start() {
        printWelcome();

        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> uploadFile();
                    case "2" -> listAllFiles();
                    case "3" -> downloadFile();
                    case "4" -> searchFiles();
                    case "5" -> viewFilesByUser();
                    case "6" -> deleteFile();
                    case "7" -> viewStatistics();
                    case "8" -> viewWebConsoles();
                    case "9" -> {
                        System.out.println("\n👋 Goodbye!");
                        cleanup();
                        return;
                    }
                    default -> System.out.println("❌ Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                logger.error("Error processing request", e);
                System.out.println("❌ Error: " + e.getMessage());
            }

            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }

    private void printWelcome() {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                                                          ║");
        System.out.println("║       INTERACTIVE FILE MANAGER                           ║");
        System.out.println("║       Upload & Download Your Files                       ║");
        System.out.println("║                                                          ║");
        System.out.println("║  Powered by: S3 + Kafka + PostgreSQL                    ║");
        System.out.println("║                                                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }

    private void printMenu() {
        System.out.println("\n╔══════════════════ MENU ══════════════════════╗");
        System.out.println("║                                              ║");
        System.out.println("║  1. 📤 Upload a File                         ║");
        System.out.println("║  2. 📋 List All Files                        ║");
        System.out.println("║  3. 📥 Download a File                       ║");
        System.out.println("║  4. 🔍 Search Files                          ║");
        System.out.println("║  5. 👤 View My Files                         ║");
        System.out.println("║  6. 🗑️  Delete a File                        ║");
        System.out.println("║  7. 📊 View Statistics                       ║");
        System.out.println("║  8. 🌐 Open Web Consoles                     ║");
        System.out.println("║  9. 🚪 Exit                                  ║");
        System.out.println("║                                              ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.print("\nYour choice: ");
    }

    /**
     * Upload a file to the system
     */
    private void uploadFile() throws IOException, SQLException {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║           UPLOAD FILE                        ║");
        System.out.println("╚══════════════════════════════════════════════╝\n");

        System.out.print("Enter file path (or drag & drop file here): ");
        String filePath = scanner.nextLine().trim().replace("\"", "");

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("❌ File not found: " + filePath);
            return;
        }

        System.out.print("Enter your username: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            username = "anonymous";
        }

        System.out.print("Enter description (optional): ");
        String description = scanner.nextLine().trim();

        // Generate metadata
        String fileId = UUID.randomUUID().toString();
        String s3Key = "uploads/" + username + "/" + file.getName();

        System.out.println("\n⏳ Uploading file...");

        // Step 1: Upload to S3
        System.out.println("  [1/4] Uploading to S3 (MinIO)...");
        String s3Url = s3Manager.uploadFile(file, s3Key);
        System.out.println("        ✓ Uploaded to S3");

        // Step 2: Create metadata
        System.out.println("  [2/4] Creating metadata...");
        FileMetadata metadata = new FileMetadata();
        metadata.setId(fileId);
        metadata.setFilename(file.getName());
        metadata.setS3Key(s3Key);
        metadata.setS3Url(s3Url);
        metadata.setFileSize(file.length());
        metadata.setContentType(guessContentType(file));
        metadata.setUploadedBy(username);
        metadata.setUploadedAt(System.currentTimeMillis());
        metadata.setDescription(description.isEmpty() ? null : description);
        System.out.println("        ✓ Metadata created");

        // Step 3: Save to database
        System.out.println("  [3/4] Saving to PostgreSQL...");
        fileRepository.insertFileMetadata(metadata);
        System.out.println("        ✓ Saved to database");

        // Step 4: Send event to Kafka
        System.out.println("  [4/4] Sending event to Kafka...");
        eventProducer.sendUploadedEvent(metadata);
        System.out.println("        ✓ Event sent to Kafka");

        System.out.println("\n✅ SUCCESS! File uploaded successfully!");
        System.out.println("\n📄 File Details:");
        System.out.println("  • File ID: " + fileId);
        System.out.println("  • Filename: " + file.getName());
        System.out.println("  • Size: " + metadata.getFileSizeFormatted());
        System.out.println("  • S3 URL: " + s3Url);
        System.out.println("  • Uploaded by: " + username);
    }

    /**
     * List all files in the system
     */
    private void listAllFiles() throws SQLException {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║           ALL FILES                          ║");
        System.out.println("╚══════════════════════════════════════════════╝\n");

        List<FileMetadata> files = fileRepository.getAllFiles();

        if (files.isEmpty()) {
            System.out.println("📭 No files found. Upload some files first!");
            return;
        }

        System.out.println("Total files: " + files.size() + "\n");

        for (int i = 0; i < files.size(); i++) {
            FileMetadata file = files.get(i);
            System.out.println("─────────────────────────────────────────────");
            System.out.println((i + 1) + ". " + file.getFilename());
            System.out.println("   ID: " + file.getId());
            System.out.println("   Size: " + file.getFileSizeFormatted());
            System.out.println("   Uploaded by: " + file.getUploadedBy());
            System.out.println("   Date: " + new java.util.Date(file.getUploadedAt()));
            if (file.getDescription() != null) {
                System.out.println("   Description: " + file.getDescription());
            }
        }
        System.out.println("─────────────────────────────────────────────");
    }

    /**
     * Download a file
     */
    private void downloadFile() throws SQLException, IOException {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║           DOWNLOAD FILE                      ║");
        System.out.println("╚══════════════════════════════════════════════╝\n");

        System.out.print("Enter filename to download: ");
        String filename = scanner.nextLine().trim();

        // Search for file
        FileMetadata metadata = fileRepository.getFileByFilename(filename);

        if (metadata == null) {
            System.out.println("❌ File not found: " + filename);
            return;
        }

        // Show file info
        System.out.println("\n📄 File found:");
        System.out.println("  • Filename: " + metadata.getFilename());
        System.out.println("  • Size: " + metadata.getFileSizeFormatted());
        System.out.println("  • Uploaded by: " + metadata.getUploadedBy());
        System.out.println("  • S3 Key: " + metadata.getS3Key());

        System.out.print("\nEnter download location (folder path): ");
        String downloadPath = scanner.nextLine().trim();

        if (downloadPath.isEmpty()) {
            downloadPath = System.getProperty("user.home") + File.separator + "Downloads";
        }

        Path destinationDir = Paths.get(downloadPath);
        if (!Files.exists(destinationDir)) {
            Files.createDirectories(destinationDir);
        }

        Path destinationFile = destinationDir.resolve(metadata.getFilename());

        System.out.println("\n⏳ Downloading from S3...");
        s3Manager.downloadFile(metadata.getS3Key(), destinationFile);

        System.out.println("\n✅ SUCCESS! File downloaded!");
        System.out.println("  📁 Location: " + destinationFile.toAbsolutePath());

        // Send download event to Kafka
        eventProducer.sendDownloadedEvent(metadata);
    }

    /**
     * Search for files
     */
    private void searchFiles() throws SQLException {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║           SEARCH FILES                       ║");
        System.out.println("╚══════════════════════════════════════════════╝\n");

        System.out.print("Enter search term: ");
        String searchTerm = scanner.nextLine().trim();

        List<FileMetadata> results = fileRepository.searchFiles(searchTerm);

        if (results.isEmpty()) {
            System.out.println("❌ No files found matching: " + searchTerm);
            return;
        }

        System.out.println("\n🔍 Found " + results.size() + " file(s):\n");

        for (int i = 0; i < results.size(); i++) {
            FileMetadata file = results.get(i);
            System.out.println("─────────────────────────────────────────────");
            System.out.println((i + 1) + ". " + file.getFilename());
            System.out.println("   Size: " + file.getFileSizeFormatted());
            System.out.println("   Uploaded by: " + file.getUploadedBy());
            System.out.println("   Date: " + new java.util.Date(file.getUploadedAt()));
        }
        System.out.println("─────────────────────────────────────────────");
    }

    /**
     * View files by user
     */
    private void viewFilesByUser() throws SQLException {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║           MY FILES                           ║");
        System.out.println("╚══════════════════════════════════════════════╝\n");

        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        List<FileMetadata> files = fileRepository.getFilesByUser(username);

        if (files.isEmpty()) {
            System.out.println("❌ No files found for user: " + username);
            return;
        }

        System.out.println("\n👤 Files for " + username + " (" + files.size() + " total):\n");

        for (int i = 0; i < files.size(); i++) {
            FileMetadata file = files.get(i);
            System.out.println("─────────────────────────────────────────────");
            System.out.println((i + 1) + ". " + file.getFilename());
            System.out.println("   Size: " + file.getFileSizeFormatted());
            System.out.println("   Date: " + new java.util.Date(file.getUploadedAt()));
        }
        System.out.println("─────────────────────────────────────────────");
    }

    /**
     * Delete a file
     */
    private void deleteFile() throws SQLException, IOException {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║           DELETE FILE                        ║");
        System.out.println("╚══════════════════════════════════════════════╝\n");

        System.out.print("Enter filename to delete: ");
        String filename = scanner.nextLine().trim();

        FileMetadata metadata = fileRepository.getFileByFilename(filename);

        if (metadata == null) {
            System.out.println("❌ File not found: " + filename);
            return;
        }

        System.out.println("\n⚠️  Are you sure you want to delete:");
        System.out.println("  • " + metadata.getFilename());
        System.out.println("  • Size: " + metadata.getFileSizeFormatted());
        System.out.print("\nType 'yes' to confirm: ");

        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("yes")) {
            System.out.println("❌ Deletion cancelled.");
            return;
        }

        System.out.println("\n⏳ Deleting...");

        // Delete from S3
        System.out.println("  [1/3] Deleting from S3...");
        s3Manager.deleteFile(metadata.getS3Key());
        System.out.println("        ✓ Deleted from S3");

        // Delete from database
        System.out.println("  [2/3] Deleting from database...");
        fileRepository.deleteFileMetadata(metadata.getId());
        System.out.println("        ✓ Deleted from database");

        // Send event to Kafka
        System.out.println("  [3/3] Sending event to Kafka...");
        eventProducer.sendDeletedEvent(metadata);
        System.out.println("        ✓ Event sent");

        System.out.println("\n✅ File deleted successfully!");
    }

    /**
     * View statistics
     */
    private void viewStatistics() throws SQLException {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║           STATISTICS                         ║");
        System.out.println("╚══════════════════════════════════════════════╝\n");

        int fileCount = fileRepository.getFileCount();
        long totalSize = fileRepository.getTotalStorageUsed();
        List<String> s3Files = s3Manager.listFiles();

        System.out.println("📊 System Statistics:");
        System.out.println("  • Total files in database: " + fileCount);
        System.out.println("  • Total files in S3: " + s3Files.size());
        System.out.println("  • Total storage used: " + formatBytes(totalSize));
        System.out.println("\n🗂️  Files in S3:");

        for (String s3File : s3Files) {
            System.out.println("  - " + s3File);
        }
    }

    /**
     * Show web console URLs
     */
    private void viewWebConsoles() {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║           WEB CONSOLES                       ║");
        System.out.println("╚══════════════════════════════════════════════╝\n");

        System.out.println("🌐 Access these URLs in your browser:\n");
        System.out.println("  📦 MinIO Console (S3 Storage)");
        System.out.println("     http://localhost:9001");
        System.out.println("     User: minioadmin / Pass: minioadmin123\n");

        System.out.println("  🗄️  pgAdmin (PostgreSQL Database)");
        System.out.println("     http://localhost:5050");
        System.out.println("     User: admin@admin.com / Pass: admin123\n");

        System.out.println("  📨 Kafka UI");
        System.out.println("     http://localhost:8080\n");
    }

    /**
     * Helper methods
     */
    private String guessContentType(File file) {
        String filename = file.getName().toLowerCase();
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".pdf")) return "application/pdf";
        if (filename.endsWith(".txt")) return "text/plain";
        if (filename.endsWith(".doc") || filename.endsWith(".docx")) return "application/msword";
        if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) return "application/vnd.ms-excel";
        if (filename.endsWith(".zip")) return "application/zip";
        if (filename.endsWith(".mp4")) return "video/mp4";
        if (filename.endsWith(".mp3")) return "audio/mpeg";
        return "application/octet-stream";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Cleanup resources
     */
    private void cleanup() {
        logger.info("Shutting down...");
        if (eventConsumer != null) {
            eventConsumer.stop();
        }
        if (eventProducer != null) {
            eventProducer.close();
        }
        if (s3Manager != null) {
            s3Manager.shutdown();
        }
        if (dbManager != null) {
            dbManager.shutdown();
        }
        scanner.close();
        logger.info("✓ Cleanup complete");
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  Starting Interactive File Manager...                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        System.out.println("Prerequisites Check:");
        System.out.println("  ✓ Docker services should be running");
        System.out.println("  ✓ Run: docker-compose up -d");
        System.out.println("  ✓ Wait for all services to start\n");

        try {
            InteractiveFileManager manager = new InteractiveFileManager();
            manager.start();
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.err.println("\n❌ Error: " + e.getMessage());
            System.err.println("\nMake sure:");
            System.err.println("  1. Docker is running");
            System.err.println("  2. Run: docker-compose up -d");
            System.err.println("  3. Wait 30-60 seconds for services to start");
        }
    }
}
