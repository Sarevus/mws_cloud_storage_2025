package com.cloudstorage.storage;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class S3FileStorage {
    private final S3Client  s3Client;
    private final String bucketName;

    public S3FileStorage(String endpoint, String accesKey, String secretKey, String bucketName){
        this.s3Client = S3Client.builder()
                .endpointOverride(java.net.URI.create("http://localhost:9000"))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accesKey, secretKey)
                ))
                .forcePathStyle(true)
                .build();
        this.bucketName = bucketName;

        createBucketIfNotExists();
    }

    private void createBucketIfNotExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            System.out.println("Bucket " + bucketName + " already exists");
        } catch (NoSuchBucketException e) {
            // Создаем бакет
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                System.out.println("Bucket " + bucketName + " created successfully");
            } catch (Exception createException) {
                System.out.println("Failed to create bucket: " + createException.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Error checking bucket: " + e.getMessage());
        }
    }

    private String generateObjectKey(Long userId, String filename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return String.format("user_%d/%s_%s", userId, timestamp, filename);
    }

    private String getContentType(String filename){
        String fileEnd = "";
        for (int i = 0; i < filename.length(); i++){
            if ( '.' != filename.charAt(filename.length()-1-i)){
                fileEnd = String.valueOf(filename.charAt(filename.length()-1-i)) + fileEnd;
            } else {
                break;
            }
        }

        if ("pdf".equals(fileEnd)) return "application/pdf";
        if ("png".equals(fileEnd)) return "image/png";
        if ("txt".equals(fileEnd)) return "text/plain";

        return "application/octet-stream";
    }

    public List<String> listUserFiles(Long userId) {
        try {
            String prefix = "user_" + userId + "/";

            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest);

            return listObjectsResponse.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Failed to list files from Ceph", e);
        }
    }

    public Object uploadFile(Long userId, String filename, InputStream fileStream, long fileSize){
        System.out.println("=== S3 UPLOAD DEBUG ===");
        System.out.println("User ID: " + userId);
        System.out.println("Filename: " + filename);
        System.out.println("File size: " + fileSize);

        String objectKey = generateObjectKey(userId, filename);
        System.out.println("Object key: " + objectKey);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(getContentType(filename))
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            System.out.println("Sending to S3...");

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(fileStream, fileSize));
            System.out.println("UPLOAD SUCCESSFUL to MinIO!");

            return objectKey;
        } catch (Exception e) {
            System.out.println("UPLOAD FAILED: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to upload file to Ceph", e);
        }
    }


    public InputStream downloadFile(String objectKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            return s3Client.getObject(getObjectRequest);

        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from Ceph", e);
        }
    }

    public void deleteFile(String objectKey) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from Ceph", e);
        }
    }

}
