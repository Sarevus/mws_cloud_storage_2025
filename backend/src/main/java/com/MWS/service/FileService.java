package com.MWS.service;

import java.util.List;
import java.io.InputStream;

public interface FileService {
    List<String> getFileLinksByUserId(long userId);

    String saveUserFile(Long userId, String filename, InputStream fileStream, long fileSize);

    void deleteUserFile(Long userId, String fileId);

    InputStream downloadFile(String objectKey);
}
