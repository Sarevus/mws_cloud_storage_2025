package com.cloudstorage.repository;

import com.cloudstorage.model.File;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Интерфейс для работы с файлами в хранилище.
 * Реализацию сделает другой разработчик (БД, файловая система и т.д.)
 */
public interface FileRepository {

    /**
     * Сохраняет файл.
     */
    File save(File file);

    /**
     * Находит файл по ID.
     */
    Optional<File> findById(UUID id);

    /**
     * Находит все файлы пользователя.
     */
    List<File> findByUserId(UUID userId);

    /**
     * Находит файл по ключу в S3.
     */
    Optional<File> findByS3Key(String s3Key);

    /**
     * Удаляет файл по ID.
     */
    void deleteById(UUID id);

    /**
     * Обновляет информацию о файле.
     */
    File update(File file);
}