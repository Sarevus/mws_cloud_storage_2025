package com.MWS.service;

import com.MWS.model.File;
import com.MWS.model.UserEntity;
import com.MWS.repository.FileRepository;
import com.MWS.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Сервис для работы с файлами.
 * Связывает PostgreSQL (метаданные) и S3 (сами файлы).
 */
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

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
            logger.warn("Попытка доступа к файлу {} пользователем {}", file.getId(), userId);
            throw new SecurityException("Доступ к файлу запрещён");
        }
    }

    /**
     * Загружает файл в систему.
     * 1. Проверяет существование пользователя
     * 2. Генерирует уникальный S3 ключ
     * 3. Сохраняет метаданные в PostgreSQL
     * 4. (В будущем) Загружает файл в S3
     *
     * @param userId ID пользователя
     * @param originalFilename Оригинальное имя файла
     * @param fileStream Поток данных файла
     * @param fileSize Размер файла в байтах
     * @param mimeType MIME-тип файла
     * @return Сохраненный объект File с метаданными
     */
    public File uploadFile(
            UUID userId,
            String originalFilename,
            InputStream fileStream,
            long fileSize,
            String mimeType
    ) {
        logger.info("Начало загрузки файла '{}' для пользователя {}", originalFilename, userId);

        // 1. Находим пользователя
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("Пользователь {} не найден", userId);
                    return new RuntimeException("Пользователь не найден");
                });

        // 2. Генерируем уникальный ключ для S3
        String s3Key = generateS3Key(userId, originalFilename);
        logger.debug("Сгенерирован S3 ключ: {}", s3Key);

        // 3. Создаем объект файла с метаданными
        File file = new File(user, originalFilename, fileSize, mimeType);
        file.setS3Key(s3Key);

        // 4. TODO: Загрузить файл в S3/Ceph используя fileStream
        // cephService.uploadFile(s3Key, fileStream, fileSize, mimeType);

        // 5. Сохраняем метаданные в PostgreSQL
        File savedFile = fileRepository.save(file);
        logger.info("Файл {} успешно загружен с ID {}", originalFilename, savedFile.getId());

        return savedFile;
    }

    /**
     * Скачивает файл.
     * 1. Находит метаданные в PostgreSQL
     * 2. Проверяет доступ
     * 3. (В будущем) Скачивает из S3
     *
     * @param userId ID пользователя
     * @param fileId ID файла
     * @return Поток данных файла
     */
    public InputStream downloadFile(UUID userId, UUID fileId) {
        logger.info("Попытка скачивания файла {} пользователем {}", fileId, userId);

        // 1. Находим метаданные в PostgreSQL
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> {
                    logger.error("Файл {} не найден", fileId);
                    return new RuntimeException("Файл не найден");
                });

        // 2. Проверяем доступ
        checkFileAccess(userId, file);

        // 3. TODO: Скачать файл из S3/Ceph
        // return cephService.downloadFile(file.getS3Key());

        logger.warn("Скачивание из S3 еще не реализовано для файла {}", fileId);
        throw new UnsupportedOperationException("Скачивание из S3 пока не реализовано");
    }

    /**
     * Получает метаданные файла.
     *
     * @param userId ID пользователя
     * @param fileId ID файла
     * @return Объект File с метаданными
     */
    public File getFileMetadata(UUID userId, UUID fileId) {
        logger.debug("Получение метаданных файла {} для пользователя {}", fileId, userId);

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        checkFileAccess(userId, file);
        return file;
    }

    /**
     * Получает список всех файлов пользователя.
     *
     * @param userId ID пользователя
     * @return Список файлов
     */
    public List<File> getUserFiles(UUID userId) {
        logger.debug("Получение списка файлов для пользователя {}", userId);

        // Проверяем существование пользователя
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<File> files = fileRepository.findByUserId(userId);
        logger.info("Найдено {} файлов для пользователя {}", files.size(), userId);

        return files;
    }

    /**
     * Удаляет файл.
     * 1. Проверяет права
     * 2. (В будущем) Удаляет из S3
     * 3. Удаляет метаданные из PostgreSQL
     *
     * @param userId ID пользователя
     * @param fileId ID файла
     */
    public void deleteFile(UUID userId, UUID fileId) {
        logger.info("Попытка удаления файла {} пользователем {}", fileId, userId);

        // 1. Находим файл
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> {
                    logger.error("Файл {} не найден", fileId);
                    return new RuntimeException("Файл не найден");
                });

        // 2. Проверяем, что пользователь - владелец
        checkFileAccess(userId, file);

        // 3. TODO: Удалить из S3/Ceph
        // cephService.deleteFile(file.getS3Key());

        // 4. Удаляем метаданные из PostgreSQL
        fileRepository.deleteById(fileId);

        logger.info("Файл {} успешно удален", fileId);
    }

    /**
     * Обновляет метаданные файла (имя, описание).
     *
     * @param userId ID пользователя
     * @param fileId ID файла
     * @param newName Новое имя файла (может быть null)
     * @return Обновленный объект File
     */
    public File updateFileMetadata(UUID userId, UUID fileId, String newName) {
        logger.info("Обновление метаданных файла {} пользователем {}", fileId, userId);

        // 1. Находим файл
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        // 2. Проверяем доступ
        checkFileAccess(userId, file);

        // 3. Обновляем метаданные
        if (newName != null && !newName.trim().isEmpty()) {
            file.setOriginalName(newName);
        }

        // 4. Сохраняем изменения
        File updatedFile = fileRepository.update(file);
        logger.info("Метаданные файла {} обновлены", fileId);

        return updatedFile;
    }

    /**
     * Поиск файла по S3 ключу.
     *
     * @param userId ID пользователя
     * @param s3Key S3 ключ файла
     * @return Объект File
     */
    public File findByS3Key(UUID userId, String s3Key) {
        logger.debug("Поиск файла по S3 ключу: {}", s3Key);

        File file = fileRepository.findByS3Key(s3Key)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        checkFileAccess(userId, file);
        return file;
    }
}