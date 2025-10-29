package com.MWS.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {


    public List<String> getFileLinksByUserId(long userId); // я отдаю user id должна получить список ссылок на файлы доступные пользовавтелю

    public String saveUserFile(Long userId, MultipartFile file);

    void deleteUserFile(Long userId, String fileId);

}
