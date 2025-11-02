package com.MWS.service;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

public class FileServiceRelease implements FileService {
    @Override
    public List<String> getFileLinksByUserId(long userId) {
        return List.of();
    }//временно пустой список

    @Override
    public String saveUserFile(Long userId, MultipartFile file) {
        try {
            String originalFileName = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFileName);
            String uniqueFileName = generateUniqueFileName(userId, fileExtension);

            // !!!времянка имитация сохранения в S3
            System.out.println("Сохраняем файл: " + uniqueFileName);
            System.out.println("Размер файла: " + file.getSize() + " bytes");
            System.out.println("Тип файла: " + file.getContentType());
            // тоже заглушка
            return "http://yourserver.com/files/" + uniqueFileName;
        } catch (Exception e){
            throw new RuntimeException("Ошибка при сохранении файла", e);
        }
    }

    @Override
    public void deleteUserFile(Long userId, String fileId) {
        // времянка
        System.out.println("Удаляем файл: " + fileId + " для пользователя: " + userId);
        // todo: Реальная логика удаления из S3 и БД
    }

    private String generateUniqueFileName(Long userId, String extension) {
        String uuid = UUID.randomUUID().toString();
        return "user_" + userId + "_" + uuid + extension;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
