package com.MWS.service;

import com.MWS.model.File;
import com.MWS.model.UserEntity;
import com.MWS.repository.FileRepository;
import com.MWS.repository.UserRepository;
import com.MWS.storage.S3FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

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

    private String generateS3Key(UUID userId, String filename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        // Заменяем опасные символы в имени файла
        String safeName = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("user/%s/%s_%s", userId, timestamp, safeName);
    }


    private void checkFileAccess(UUID userId, File file) {
        if (!file.getUser().getId().equals(userId)) {
            logger.warn("Попытка доступа к файлу {} пользователем {}", file.getId(), userId);
            throw new SecurityException("Доступ к файлу запрещён");
        }
    }


    public File uploadFile(
            UUID userId,
            String originalFilename,
            InputStream fileStream,
            long fileSize,
            String mimeType
    ) {
        logger.info("Начало загрузки файла '{}' для пользователя {}", originalFilename, userId);

        try {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        logger.error("Пользователь {} не найден", userId);
                        return new RuntimeException("Пользователь не найден");
                    });

            String s3Key = generateS3Key(userId, originalFilename);
            logger.debug("Сгенерирован S3 ключ: {}", s3Key);

            String fileUrl = s3Storage.uploadFile(s3Key, fileStream, fileSize, mimeType);
            logger.info("Файл загружен в S3: {}", fileUrl);

            File file = new File(user, originalFilename, fileSize, mimeType);
            file.setS3Key(s3Key);

            File savedFile = fileRepository.save(file);
            logger.info("✅ Файл {} успешно загружен с ID {}", originalFilename, savedFile.getId());

            return savedFile;

        } catch (Exception e) {
            logger.error("❌ Ошибка при загрузке файла '{}'", originalFilename, e);
            throw new RuntimeException("Не удалось загрузить файл: " + e.getMessage(), e);
        }
    }


    public InputStream downloadFile(UUID userId, UUID fileId) {
        logger.info("Попытка скачивания файла {} пользователем {}", fileId, userId);

        try {
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> {
                        logger.error("Файл {} не найден", fileId);
                        return new RuntimeException("Файл не найден");
                    });

            checkFileAccess(userId, file);

            InputStream fileStream = s3Storage.downloadFile(file.getS3Key());
            logger.info("✅ Файл {} успешно скачан", fileId);

            return fileStream;

        } catch (SecurityException e) {
            logger.error("❌ Доступ запрещен к файлу {} для пользователя {}", fileId, userId);
            throw e;
        } catch (Exception e) {
            logger.error("❌ Ошибка при скачивании файла {}", fileId, e);
            throw new RuntimeException("Не удалось скачать файл: " + e.getMessage(), e);
        }
    }


    public File getFileMetadata(UUID userId, UUID fileId) {
        logger.debug("Получение метаданных файла {} для пользователя {}", fileId, userId);

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        checkFileAccess(userId, file);
        return file;
    }


    public List<File> getUserFiles(UUID userId) {
        logger.debug("Получение списка файлов для пользователя {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<File> files = fileRepository.findByUserId(userId);
        logger.info("Найдено {} файлов для пользователя {}", files.size(), userId);

        return files;
    }


    public void deleteFile(UUID userId, UUID fileId) {
        logger.info("Попытка удаления файла {} пользователем {}", fileId, userId);

        try {
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> {
                        logger.error("Файл {} не найден", fileId);
                        return new RuntimeException("Файл не найден");
                    });

            checkFileAccess(userId, file);

            s3Storage.deleteFile(file.getS3Key());
            logger.info("Файл удален из S3: {}", file.getS3Key());

            fileRepository.deleteById(fileId);

            logger.info("✅ Файл {} успешно удален", fileId);

        } catch (SecurityException e) {
            logger.error("❌ Доступ запрещен к файлу {} для пользователя {}", fileId, userId);
            throw e;
        } catch (Exception e) {
            logger.error("❌ Ошибка при удалении файла {}", fileId, e);
            throw new RuntimeException("Не удалось удалить файл: " + e.getMessage(), e);
        }
    }


    public File updateFileMetadata(UUID userId, UUID fileId, String newName) {
        logger.info("Обновление метаданных файла {} пользователем {}", fileId, userId);

        try {
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("Файл не найден"));

            checkFileAccess(userId, file);

            if (newName != null && !newName.trim().isEmpty()) {
                file.setOriginalName(newName);
            }

            File updatedFile = fileRepository.update(file);
            logger.info("✅ Метаданные файла {} обновлены", fileId);

            return updatedFile;

        } catch (SecurityException e) {
            logger.error("❌ Доступ запрещен к файлу {} для пользователя {}", fileId, userId);
            throw e;
        } catch (Exception e) {
            logger.error("❌ Ошибка при обновлении метаданных файла {}", fileId, e);
            throw new RuntimeException("Не удалось обновить метаданные: " + e.getMessage(), e);
        }
    }

    public File findByS3Key(UUID userId, String s3Key) {
        logger.debug("Поиск файла по S3 ключу: {}", s3Key);

        File file = fileRepository.findByS3Key(s3Key)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        checkFileAccess(userId, file);
        return file;
    }

    public boolean fileExistsInStorage(String s3Key) {
        return s3Storage.fileExists(s3Key);
    }
}