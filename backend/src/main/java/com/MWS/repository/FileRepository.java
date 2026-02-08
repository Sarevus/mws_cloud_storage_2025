package com.MWS.repository;

import com.MWS.model.File;

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
     * Находит файл по ключу в S3.
     */
    Optional<File> findByS3Key(String s3Key);

    /**
     * Находит все файлы пользователя.
     */
    List<File> findByUserId(UUID userId);

    /**
     * Удаляет файл по ID.
     */
    void deleteById(UUID id);

    /**
     * Удаляет файл по ключу в S3.
     */
    void deleteByS3Key(String s3Key);

    /**
     * Обновляет информацию о файле.
     */
    File update(File file);

    List<File> findByUserIdAndCategory(UUID userId, String category);

    List<String> findDistinctCategoriesByUser(UUID userId);
}