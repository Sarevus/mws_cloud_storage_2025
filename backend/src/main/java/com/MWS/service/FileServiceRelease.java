package com.MWS.service;

import com.MWS.storage.S3FileStorage;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.io.InputStream;
import java.util.List;

public class FileServiceRelease implements FileService{
    private final S3FileStorage cephStorage;

    public FileServiceRelease() {
        // Настройки Ceph
        String endpoint = "http://localhost:9000";
        String accessKey = "admin_access_key";
        String secretKey = "admin_secret_key";
        String bucketName = "userdata";

        this.cephStorage = new S3FileStorage(endpoint, accessKey, secretKey, bucketName);
    }

    @Override
    public List<String> getFileLinksByUserId(long userId) {
        List<String> fileKeys = cephStorage.listUserFiles(userId);
        return fileKeys;
    }

    @Override
    public String saveUserFile(Long userId, String filename, InputStream fileStream, long fileSize) {
        // Просто передаем в CephStorage
        return cephStorage.uploadFile(userId, filename, fileStream, fileSize).toString();
    }

    @Override
    public void deleteUserFile(Long userId, String fileId) {
        cephStorage.deleteFile(fileId);
    }

    @Override
    public InputStream downloadFile(String objectKey) {
        return cephStorage.downloadFile(objectKey);
    }
}
