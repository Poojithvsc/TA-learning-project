package com.example.kafka.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * S3Manager handles all interactions with S3 (MinIO) storage.
 *
 * What is S3/MinIO?
 * - S3 = Amazon's blob storage service
 * - MinIO = Open-source S3-compatible storage (runs locally)
 * - Stores files of any size: images, videos, documents, etc.
 *
 * Why use blob storage?
 * - Don't store large files in database
 * - Scalable - can store unlimited data
 * - Cost-effective
 * - Fast access from anywhere
 */
public class S3Manager {
    private static final Logger logger = LoggerFactory.getLogger(S3Manager.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final String endpoint;

    /**
     * Initialize S3Manager with MinIO connection details
     */
    public S3Manager(String endpoint, String accessKey, String secretKey, String bucketName) {
        this.endpoint = endpoint;
        this.bucketName = bucketName;

        logger.info("Initializing S3Manager...");
        logger.info("Endpoint: {}", endpoint);
        logger.info("Bucket: {}", bucketName);

        // Create S3 client with MinIO configuration
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.US_EAST_1)  // MinIO doesn't use regions, but SDK requires it
                .forcePathStyle(true)       // Required for MinIO
                .build();

        // Create bucket if it doesn't exist
        createBucketIfNotExists();

        logger.info("✓ S3Manager initialized successfully");
    }

    /**
     * Create bucket if it doesn't exist
     *
     * Bucket = Like a folder that holds files
     * You must create a bucket before uploading files
     */
    private void createBucketIfNotExists() {
        try {
            // Check if bucket exists
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build());

            logger.info("Bucket '{}' already exists", bucketName);

        } catch (NoSuchBucketException e) {
            // Bucket doesn't exist, create it
            logger.info("Creating bucket '{}'...", bucketName);

            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build());

            logger.info("✓ Bucket '{}' created successfully", bucketName);

        } catch (Exception e) {
            logger.error("Error checking/creating bucket", e);
            throw new RuntimeException("Failed to initialize bucket", e);
        }
    }

    /**
     * Upload a file to S3
     *
     * @param file The file to upload
     * @param objectKey The name/path to store the file as (e.g., "images/photo.jpg")
     * @return The URL to access the uploaded file
     */
    public String uploadFile(File file, String objectKey) throws IOException {
        logger.info("Uploading file to S3: {}", objectKey);

        try {
            // Upload the file
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .contentType(getContentType(file))
                            .build(),
                    RequestBody.fromFile(file));

            // Generate URL to access the file
            String fileUrl = String.format("%s/%s/%s", endpoint, bucketName, objectKey);

            logger.info("✓ File uploaded successfully: {}", fileUrl);
            return fileUrl;

        } catch (Exception e) {
            logger.error("Failed to upload file: {}", objectKey, e);
            throw new IOException("S3 upload failed", e);
        }
    }

    /**
     * Upload file from byte array
     * Useful when file is in memory
     */
    public String uploadFile(byte[] data, String objectKey, String contentType) throws IOException {
        logger.info("Uploading data to S3: {} ({} bytes)", objectKey, data.length);

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(data));

            String fileUrl = String.format("%s/%s/%s", endpoint, bucketName, objectKey);

            logger.info("✓ Data uploaded successfully: {}", fileUrl);
            return fileUrl;

        } catch (Exception e) {
            logger.error("Failed to upload data: {}", objectKey, e);
            throw new IOException("S3 upload failed", e);
        }
    }

    /**
     * Download a file from S3
     *
     * @param objectKey The file's key/path in S3
     * @param destinationPath Where to save the downloaded file
     */
    public void downloadFile(String objectKey, Path destinationPath) throws IOException {
        logger.info("Downloading file from S3: {}", objectKey);

        try {
            // If file exists, delete it first
            if (Files.exists(destinationPath)) {
                logger.info("File already exists, deleting: {}", destinationPath);
                Files.delete(destinationPath);
            }

            // Create parent directories if they don't exist
            if (destinationPath.getParent() != null) {
                Files.createDirectories(destinationPath.getParent());
            }

            s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build(),
                    ResponseTransformer.toFile(destinationPath));

            logger.info("✓ File downloaded successfully to: {}", destinationPath);

        } catch (Exception e) {
            logger.error("Failed to download file: {}", objectKey, e);
            throw new IOException("S3 download failed", e);
        }
    }

    /**
     * Download file as byte array
     */
    public byte[] downloadFileAsBytes(String objectKey) throws IOException {
        logger.info("Downloading file as bytes from S3: {}", objectKey);

        try {
            return s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build(),
                    ResponseTransformer.toBytes()).asByteArray();

        } catch (Exception e) {
            logger.error("Failed to download file: {}", objectKey, e);
            throw new IOException("S3 download failed", e);
        }
    }

    /**
     * Delete a file from S3
     */
    public void deleteFile(String objectKey) throws IOException {
        logger.info("Deleting file from S3: {}", objectKey);

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());

            logger.info("✓ File deleted successfully: {}", objectKey);

        } catch (Exception e) {
            logger.error("Failed to delete file: {}", objectKey, e);
            throw new IOException("S3 delete failed", e);
        }
    }

    /**
     * List all files in the bucket
     */
    public List<String> listFiles() {
        logger.info("Listing files in bucket: {}", bucketName);
        List<String> fileKeys = new ArrayList<>();

        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);

            for (S3Object object : response.contents()) {
                fileKeys.add(object.key());
            }

            logger.info("Found {} files in bucket", fileKeys.size());
            return fileKeys;

        } catch (Exception e) {
            logger.error("Failed to list files", e);
            return fileKeys;
        }
    }

    /**
     * Check if a file exists in S3
     */
    public boolean fileExists(String objectKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
            return true;

        } catch (NoSuchKeyException e) {
            return false;

        } catch (Exception e) {
            logger.error("Error checking file existence: {}", objectKey, e);
            return false;
        }
    }

    /**
     * Get file size in bytes
     */
    public long getFileSize(String objectKey) throws IOException {
        try {
            HeadObjectResponse response = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build());

            return response.contentLength();

        } catch (Exception e) {
            logger.error("Failed to get file size: {}", objectKey, e);
            throw new IOException("Failed to get file size", e);
        }
    }

    /**
     * Determine content type based on file extension
     */
    private String getContentType(File file) {
        String filename = file.getName().toLowerCase();

        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".pdf")) return "application/pdf";
        if (filename.endsWith(".txt")) return "text/plain";
        if (filename.endsWith(".json")) return "application/json";
        if (filename.endsWith(".xml")) return "application/xml";
        if (filename.endsWith(".zip")) return "application/zip";

        return "application/octet-stream"; // Default binary type
    }

    /**
     * Close S3 client when done
     */
    public void shutdown() {
        if (s3Client != null) {
            s3Client.close();
            logger.info("S3Client closed");
        }
    }

    // Getters
    public String getBucketName() {
        return bucketName;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
