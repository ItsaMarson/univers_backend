/* (C)2026 */
package com.univers.univers_backend.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path rootLocation;
    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;
    private static final float QUALITY = 0.8f;

    public LocalFileStorageService(@Value("${storage.location:./storage}") String storageLocation) {
        this.rootLocation = Paths.get(storageLocation);
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
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

            String finalExtension;
            boolean isImage = isImageFile(extension);
            if (isImage) {
                finalExtension = ".webp";
            } else {
                finalExtension = extension;
            }

            String prefix =
                    (objectNamePrefix != null && !objectNamePrefix.isBlank())
                            ? objectNamePrefix.endsWith("/")
                                    ? objectNamePrefix
                                    : objectNamePrefix + "/"
                            : "";

            String uniqueFileName = UUID.randomUUID().toString() + finalExtension;
            String relativePath = prefix + uniqueFileName;

            Path bucketPath = rootLocation.resolve(bucketName);
            Files.createDirectories(bucketPath);

            Path destinationFile = bucketPath.resolve(relativePath).normalize();
            if (!destinationFile.getParent().startsWith(bucketPath.normalize())) {
                // This is a security check to prevent directory traversal
                throw new RuntimeException("Cannot store file outside current directory.");
            }

            Files.createDirectories(destinationFile.getParent());

            if (isImage) {
                processAndSaveImage(file.getInputStream(), destinationFile);
            } else {
                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            logger.info(
                    "File uploaded successfully to local storage: {}/{}", bucketName, relativePath);
            return relativePath;

        } catch (IOException e) {
            logger.error("Error uploading file to local storage: {}", e.getMessage(), e);
            throw new RuntimeException(
                    "Failed to upload file to local storage. " + e.getMessage(), e);
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

    private void processAndSaveImage(InputStream inputStream, Path destinationFile)
            throws IOException {
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

        Thumbnails.of(originalImage)
                .size(newWidth, newHeight)
                .outputQuality(QUALITY)
                .outputFormat("webp")
                .toFile(destinationFile.toFile());

        try {
            inputStream.close();
        } catch (IOException e) {
            logger.warn("Failed to close original image input stream after processing", e);
        }
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
            Path file = rootLocation.resolve(bucketName).resolve(objectName).normalize();
            if (Files.exists(file)) {
                Files.delete(file);
                logger.info(
                        "File deleted successfully from local storage: {}/{}",
                        bucketName,
                        objectName);
            }
        } catch (IOException e) {
            logger.error(
                    "Error deleting file from local storage: {}/{}. Error: {}",
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

        // Clean up objectName to ensure no leading slash
        String cleanedObjectName =
                objectName.startsWith("/") ? objectName.substring(1) : objectName;

        // Construct the URL pointing to our internal FileController
        // Example: /api/files/univers-users/user-profile-images/uuid.webp
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/files/")
                .path(bucketName)
                .path("/")
                .path(cleanedObjectName)
                .toUriString();
    }
}
