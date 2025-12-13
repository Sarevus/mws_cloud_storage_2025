package com.MWS.repository;

import com.MWS.model.File;

import java.io.InputStream;
import java.util.Optional;

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
    InputStream findByS3Key(String s3Key);

    /**
     * Удаляет файл по по ключу в S3.
     */
    void deleteByS3Key(String s3Key);
}