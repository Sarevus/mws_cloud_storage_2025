package com.cloudstorage.storage;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;

public class S3FileStorage {
    private final S3Client s3Client;
    private final String bucketName;

    public S3FileStorage(String endpoint, String accessKey, String secretKey, String bucketName) {
        this.s3Client = S3Client.builder()
                .endpointOverride(java.net.URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .forcePathStyle(true)
                .build();
        this.bucketName = bucketName;

        createBucketIfNotExists();
    }

    private void createBucketIfNotExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            System.out.println("✅ Bucket " + bucketName + " уже существует");
        } catch (NoSuchBucketException e) {
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                System.out.println("✅ Bucket " + bucketName + " создан");
            } catch (Exception createException) {
                System.out.println("❌ Ошибка создания bucket: " + createException.getMessage());
            }
        } catch (Exception e) {
            System.out.println("❌ Ошибка проверки bucket: " + e.getMessage());
        }
    }

    public void uploadFile(String s3Key, InputStream fileStream, long fileSize, String mimeType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(mimeType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(fileStream, fileSize));
            System.out.println("✅ Файл загружен в S3: " + s3Key);

        } catch (Exception e) {
            System.out.println("❌ Ошибка загрузки файла: " + e.getMessage());
            throw new RuntimeException("Ошибка загрузки файла в S3", e);
        }
    }

    public InputStream downloadFile(String s3Key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            return s3Client.getObject(getObjectRequest);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка скачивания файла из S3", e);
        }
    }

    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            System.out.println("✅ Файл удалён из S3: " + s3Key);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка удаления файла из S3", e);
        }
    }
}