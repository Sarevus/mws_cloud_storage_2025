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
     * Находит файл по ключу в S3.
     */
    Optional<File> findByS3Key(String s3Key);

    /**
     * Удаляет файл по по ключу в S3.
     */
    void deleteByS3Key(String s3Key);
}