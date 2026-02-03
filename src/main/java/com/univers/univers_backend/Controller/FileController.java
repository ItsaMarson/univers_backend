/* (C)2026 */
package com.univers.univers_backend.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

@RestController
@RequestMapping("/files")
@Tag(name = "File Storage", description = "File retrieval APIs")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private final Path rootLocation;

    public FileController(@Value("${storage.location:./storage}") String storageLocation) {
        this.rootLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
    }

    @Operation(summary = "Get a file", description = "Retrieves a file from the local storage")
    @GetMapping("/{bucket}/**")
    public ResponseEntity<Resource> getFile(
            @PathVariable String bucket, HttpServletRequest request) {
        // Extract the full path after the bucket name
        String fullPath =
                (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchingPattern =
                (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String objectName =
                new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, fullPath);

        // Remove leading slash if present to prevent Path.resolve issues
        if (objectName.startsWith("/")) {
            objectName = objectName.substring(1);
        }

        try {
            Path filePath = rootLocation.resolve(bucket).resolve(objectName).normalize();

            // Security check: ensure the file is still within the rootLocation
            if (!filePath.startsWith(rootLocation)) {
                logger.warn("Security alert: Attempted directory traversal access to {}", filePath);
                return ResponseEntity.status(403).build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                logger.warn("File not found or not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            logger.error("Error retrieving file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
