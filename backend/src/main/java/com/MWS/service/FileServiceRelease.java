package com.MWS.service;

import com.MWS.storage.S3FileStorage;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileServiceRelease implements FileService{
    private final S3FileStorage cephStorage;

    public FileServiceRelease() {
        // Настройки Ceph
        String endpoint = "http://localhost:9000";
        String accessKey = "admin";        // MINIO_ROOT_USER
        String secretKey = "password123";  // MINIO_ROOT_PASSWORD
        String bucketName = "userdata";

        this.cephStorage = new S3FileStorage(endpoint, accessKey, secretKey, bucketName);
    }

    @Override
    public List<String> getFileLinksByUserId(long userId) {
        System.out.println("DEBUG: Getting REAL files for user " + userId);

        try {
            List<String> files = cephStorage.listUserFiles(userId);
            System.out.println("DEBUG: Found " + files.size() + " real files: " + files);
            return files;
        } catch (Exception e) {
            System.out.println("DEBUG: Ceph error: " + e.getMessage());
            e.printStackTrace();
            // В случае ошибки верни пустой список
            return new ArrayList<>();
        }
    }

    @Override
    public String saveUserFile(Long userId, String filename, InputStream fileStream, long fileSize) {
        System.out.println("DEBUG: Upload file for user " + userId + ", filename: " + filename);

        try {
            String objectKey = (String) cephStorage.uploadFile(userId, filename, fileStream, fileSize);
            System.out.println("DEBUG: File successfully uploaded: " + objectKey);
            return objectKey;
        } catch (Exception e) {
            System.out.println("DEBUG: Upload failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void deleteUserFile(Long userId, String fileId) {
        System.out.println("DEBUG: Delete file: " + fileId);
        // Пока ничего не делаем
    }

    @Override
    public InputStream downloadFile(String objectKey) {
        System.out.println("DEBUG: Download file: " + objectKey);

        // Временно верни тестовый InputStream
        String testContent = "This is test content for file: " + objectKey;
        return new ByteArrayInputStream(testContent.getBytes());
    }
}
