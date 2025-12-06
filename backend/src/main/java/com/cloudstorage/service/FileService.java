package com.cloudstorage.service;

import com.cloudstorage.model.File;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.FileRepository;
import com.cloudstorage.repository.UserRepository;
import com.cloudstorage.storage.S3FileStorage;

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
    private final S3FileStorage s3Storage;

    public FileService(
        FileRepository fileRepository,
        UserRepository userRepository,
        S3FileStorage s3Storage
    ) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.s3Storage = s3Storage;
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
        // Публичные файлы доступны всем
        if (Boolean.TRUE.equals(file.getIsPublic())) {
            return;
        }

        // Приватные файлы - только владельцу
        if (!file.getUser().getId().equals(userId)) {
            throw new RuntimeException("Доступ к файлу запрещён");
        }
    }

    public File uploadFile(
        UUID userId,
        String originalFilename,
        InputStream fileStream,
        long fileSize,
        String mimeType,
        boolean isPublic,
        String description
    ){

        // 1. Находим пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 2. Генерируем уникальный ключ для S3
        String s3Key = generateS3Key(userId, originalFilename);

        // 3. Загружаем файл в S3
        s3Storage.uploadFile(s3Key, fileStream, fileSize, mimeType);

        // 4. Сохраняем метаданные в PostgreSQL
        File file = new File(user, originalFilename, fileSize, mimeType);
        file.setS3Key(s3Key);
        file.setIsPublic(isPublic);
        file.setDescription(description);

        return fileRepository.save(file);
    }

    /**
     * Возвращает все файлы пользователя.
     * Берёт данные ТОЛЬКО из PostgreSQL (метаданные).
     */
    public List<File> getUserFiles(UUID userId) {
        // Проверяем, что пользователь существует
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Получаем файлы из PostgreSQL
        return fileRepository.findByUserId(userId);
    }

    /**
     * Скачивает файл.
     * 1. Находит метаданные в PostgreSQL
     * 2. Проверяет доступ
     * 3. Скачивает из S3
     */
    public InputStream downloadFile(UUID userId, UUID fileId) {
        // 1. Находим метаданные в PostgreSQL
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        // 2. Проверяем доступ
        checkFileAccess(userId, file);

        // 3. Обновляем время последнего доступа
        file.setUpdatedAt(LocalDateTime.now());
        fileRepository.update(file);

        // 4. Скачиваем из S3
        return s3Storage.downloadFile(file.getS3Key());
    }

    /**
     * Удаляет файл.
     * 1. Проверяет права
     * 2. Удаляет из S3
     * 3. Удаляет метаданные из PostgreSQL
     */
    public void deleteFile(UUID userId, UUID fileId) {
        // 1. Находим файл
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        // 2. Проверяем, что пользователь - владелец
        if (!file.getUser().getId().equals(userId)) {
            throw new RuntimeException("Недостаточно прав для удаления файла");
        }

        // 3. Удаляем из S3
        s3Storage.deleteFile(file.getS3Key());

        // 4. Удаляем метаданные из PostgreSQL
        fileRepository.deleteById(fileId);
    }
}
