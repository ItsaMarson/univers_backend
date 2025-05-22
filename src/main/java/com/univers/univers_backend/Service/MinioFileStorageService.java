/* (C)2025 */
package com.univers.univers_backend.Service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// Note: com.luciad.imageio.webp.WebPWriteParam is not directly used
// but the webp-imageio library is needed for Thumbnailator to output WebP.

@Service
public class MinioFileStorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(MinioFileStorageService.class);
    private final MinioClient minioClient;
    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;
    private static final float QUALITY = 0.8f; // 80% quality
    private static final long DEFAULT_PART_SIZE = 5 * 1024 * 1024; // 5MB

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
            String determinedExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                determinedExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String finalExtension;
            InputStream processedStream;
            String contentType;

            if (isImageFile(determinedExtension)) {
                finalExtension = ".webp";
                processedStream = processImage(file.getInputStream());
                contentType = "image/webp";
            } else {
                finalExtension = determinedExtension;
                processedStream = file.getInputStream();
                contentType = file.getContentType();
            }

            String prefix =
                    (objectNamePrefix != null && !objectNamePrefix.isBlank())
                            ? objectNamePrefix.endsWith("/")
                                    ? objectNamePrefix
                                    : objectNamePrefix + "/"
                            : "";

            String uniqueObjectName = prefix + UUID.randomUUID().toString() + finalExtension;

            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(uniqueObjectName).stream(
                                    processedStream, -1, DEFAULT_PART_SIZE)
                            .contentType(contentType)
                            .build());

            logger.info("File uploaded successfully to MinIO: {}/{}", bucketName, uniqueObjectName);
            return uniqueObjectName;

        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            logger.error("Error uploading file to MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to MinIO. " + e.getMessage(), e);
        }
    }

    private boolean isImageFile(String extension) {
        if (extension == null || extension.isEmpty()) return false;
        String lowerExt = extension.toLowerCase();
        return lowerExt.equals(".jpg")
                || lowerExt.equals(".jpeg")
                || lowerExt.equals(".png")
                || lowerExt.equals(".gif")
                || lowerExt.equals(".bmp")
                || lowerExt.equals(".webp");
    }

    private InputStream processImage(InputStream inputStream) throws IOException {
        var originalImage = ImageIO.read(inputStream);
        if (originalImage == null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.warn("Failed to close input stream after ImageIO.read returned null", e);
            }
            throw new IOException(
                    "Failed to read image, possibly unsupported format or corrupt file.");
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        if (originalWidth > MAX_WIDTH || originalHeight > MAX_HEIGHT) {
            double widthRatio = (double) MAX_WIDTH / originalWidth;
            double heightRatio = (double) MAX_HEIGHT / originalHeight;
            double ratio = Math.min(widthRatio, heightRatio);
            newWidth = (int) (originalWidth * ratio);
            newHeight = (int) (originalHeight * ratio);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(originalImage)
                .size(newWidth, newHeight)
                .outputQuality(QUALITY)
                .outputFormat("webp")
                .toOutputStream(outputStream);

        try {
            inputStream.close();
        } catch (IOException e) {
            logger.warn("Failed to close original image input stream after processing", e);
        }

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    @Override
    public void deleteFile(String objectName, String bucketName) {
        if (objectName == null
                || objectName.isBlank()
                || bucketName == null
                || bucketName.isBlank()) {
            logger.warn("Attempted to delete file with null or blank objectName/bucketName.");
            return;
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
        }
    }

    @Override
    public String getFileUrl(String objectName, String bucketName) {
        if (objectName == null
                || objectName.isBlank()
                || bucketName == null
                || bucketName.isBlank()) {
            return null;
        }
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(1, TimeUnit.HOURS)
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
