package com.MWS.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class FileServiceRelease implements FileService {
    @Override
    public List<String> getFileLinksByUserId(long userId) {
        return List.of();
    }

    @Override
    public String saveUserFile(Long userId, MultipartFile file) {
        String fileName = ""; // здесь надо будет сегенерировать название файла
        return "http://yourserver.com/files/" + fileName;
    }

    @Override
    public void deleteUserFile(Long userId, String fileId) {
        // здесь должна быть проверка на то, имеет ли право юзер удалить файл или нет
        // если имеет, то удалить
    }
}
