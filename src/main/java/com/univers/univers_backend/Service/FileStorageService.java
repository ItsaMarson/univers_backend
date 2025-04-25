/* (C)2025 */
package com.univers.univers_backend.Service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /**
     * Uploads a file to the specified bucket.
     * @param file The file to upload.
     * @param bucketName The target bucket name.
     * @param objectNamePrefix Optional prefix for the object name (e.g., "user-uploads/").
     * @return The unique object name assigned to the uploaded file.
     * @throws RuntimeException if upload fails.
     */
    String uploadFile(MultipartFile file, String bucketName, String objectNamePrefix);

    /**
     * Deletes a file from the specified bucket.
     * @param objectName The name of the object to delete.
     * @param bucketName The bucket containing the object.
     * @throws RuntimeException if deletion fails.
     */
    void deleteFile(String objectName, String bucketName);

    /**
     * Gets a publicly accessible or presigned URL for the object.
     * (Implementation might vary based on bucket policy - presigned is safer)
     * @param objectName The name of the object.
     * @param bucketName The bucket containing the object.
     * @return The URL string.
     * @throws RuntimeException if URL generation fails.
     */
    String getFileUrl(String objectName, String bucketName);
}
