/* (C)2025 */
package com.univers.univers_backend.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${minio.bucket.users}")
    private String usersBucketName;

    @Value("${minio.bucket.events}")
    private String eventsBucketName;

    @Value("${minio.bucket.venues}")
    private String venuesBucketName;

    @Value("${minio.bucket.equipments}")
    private String equipmentsBucketName;

    @Value("${minio.bucket.letters}")
    private String lettersBucketName;

    @Value("${minio.bucket.venuesreservationletters}")
    private String venuesReservationLettersBucketName;

    @Bean
    public MinioClient minioClient()
            throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        MinioClient minioClient =
                MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();

        createBucketIfNotExists(minioClient, usersBucketName);
        createBucketIfNotExists(minioClient, venuesBucketName);
        createBucketIfNotExists(minioClient, eventsBucketName);
        createBucketIfNotExists(minioClient, equipmentsBucketName);
        createBucketIfNotExists(minioClient, lettersBucketName);
        createBucketIfNotExists(minioClient, venuesReservationLettersBucketName);

        return minioClient;
    }

    private void createBucketIfNotExists(MinioClient client, String bucketName)
            throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        boolean found = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            System.out.println("MinIO bucket created: " + bucketName);
        } else {
            System.out.println("MinIO bucket already exists: " + bucketName);
        }
    }
}
