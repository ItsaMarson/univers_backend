/* (C)2025 */
package com.univers.univers_backend.Service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MinioFileStorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(MinioFileStorageService.class);
    private final MinioClient minioClient;

    public MinioFileStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public String uploadFile(MultipartFile file, String bucketName, String objectNamePrefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            // Sanitize prefix if provided
            String prefix =
                    (objectNamePrefix != null && !objectNamePrefix.isBlank())
                            ? objectNamePrefix.endsWith("/")
                                    ? objectNamePrefix
                                    : objectNamePrefix + "/"
                            : "";

            String uniqueObjectName = prefix + UUID.randomUUID().toString() + extension;

            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(uniqueObjectName).stream(
                                    file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            logger.info("File uploaded successfully to MinIO: {}/{}", bucketName, uniqueObjectName);
            return uniqueObjectName; // Return only the object name

        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            logger.error("Error uploading file to MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to MinIO. " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String objectName, String bucketName) {
        if (objectName == null
                || objectName.isBlank()
                || bucketName == null
                || bucketName.isBlank()) {
            logger.warn("Attempted to delete file with null or blank objectName/bucketName.");
            return; // Or throw exception based on requirements
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
            logger.info("File deleted successfully from MinIO: {}/{}", bucketName, objectName);
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            logger.error(
                    "Error deleting file from MinIO: {}/{}. Error: {}",
                    bucketName,
                    objectName,
                    e.getMessage(),
                    e);
            // Decide if you want to re-throw or just log
            // throw new RuntimeException("Failed to delete file from MinIO. " + e.getMessage(), e);
        }
    }

    @Override
    public String getFileUrl(String objectName, String bucketName) {
        if (objectName == null
                || objectName.isBlank()
                || bucketName == null
                || bucketName.isBlank()) {
            return null; // Or throw exception
        }
        try {
            // Generate a presigned URL valid for 1 hour (adjust as needed)
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(1, TimeUnit.HOURS) // Example: URL valid for 1 hour
                            .build());
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            logger.error(
                    "Error getting presigned URL for MinIO object: {}/{}. Error: {}",
                    bucketName,
                    objectName,
                    e.getMessage(),
                    e);
            // Depending on requirements, you might return null, a default placeholder URL, or
            // re-throw
            return null; // Return null or a placeholder if URL generation fails
            // throw new RuntimeException("Failed to get file URL from MinIO. " + e.getMessage(),
            // e);
        }
    }
}
