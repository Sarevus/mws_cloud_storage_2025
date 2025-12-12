package com.MWS.service;

import com.MWS.model.File;
import com.MWS.model.UserEntity;
import com.MWS.repository.FileRepository;
import com.MWS.repository.UserRepository;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


/**
 * Сервис для работы с файлами.
 * Связывает PostgreSQL (метаданные) и S3 (сами файлы).
 */
public class FileService {
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    public FileService(
            FileRepository fileRepository,
            UserRepository userRepository
    ) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
    }

    /**
     * Генерирует ключ для хранения в S3.
     * Формат: user/{userId}/{timestamp}_{filename}
     */
    private String generateS3Key(UUID userId, String filename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        // Заменяем опасные символы в имени файла
        String safeName = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("user/%s/%s_%s", userId, timestamp, safeName);
    }

    /**
     * Проверяет доступ пользователя к файлу.
     */
    private void checkFileAccess(UUID userId, File file) {
        if (!file.getUser().getId().equals(userId)) {
            throw new RuntimeException("Доступ к файлу запрещён");
        }
    }

    public File uploadFile(
            UUID userId,
            String originalFilename,
            InputStream fileStream,
            long fileSize,
            String mimeType
//            boolean isPublic,
//            String description
    ){

        // 1. Находим пользователя
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 2. Генерируем уникальный ключ для S3
        String s3Key = generateS3Key(userId, originalFilename);

        // 4. Сохраняем файл в S3
        File file = new File(user, originalFilename, fileSize, mimeType);
        file.setS3Key(s3Key);

        return fileRepository.save(file);
    }

    /**
     * Скачивает файл.
     * 1. Находит метаданные в PostgreSQL
     * 2. Проверяет доступ
     * 3. Скачивает из S3
     */
    public InputStream downloadFile(UUID userId, UUID fileId) {
        // 1. Находим метаданные в PostgreSQL
        File file = userRepository.findFileById(fileId, userId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        // 2. Проверяем доступ
        checkFileAccess(userId, file);

        // 4. Скачиваем из S3
        return fileRepository.findByS3Key(file.getS3Key());
    }

    /**
     * Удаляет файл.
     * 1. Проверяет права
     * 2. Удаляет из S3
     * 3. Удаляет метаданные из PostgreSQL
     */
    public void deleteFile(UUID userId, UUID fileId) {
        // 1. Находим файл
        File file = userRepository.findFileById(fileId, userId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        // 2. Проверяем, что пользователь - владелец
        checkFileAccess(userId, file);

        // 3. Удаляем из S3
        fileRepository.deleteByS3Key(file.getS3Key());

        // 4. Удаляем метаданные из PostgreSQL
        userRepository.deleteFileById(fileId, userId);
    }
}
