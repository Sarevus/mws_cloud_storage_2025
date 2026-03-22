package com.MWS.repository;

import com.MWS.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Интерфейс для работы с файлами в хранилище.
 * Реализацию сделает другой разработчик (БД, файловая система и т.д.)
 */
@Repository
public interface FileRepository extends JpaRepository<File, UUID> {
    /**
     * Находит файл по ключу в S3.
     */
    Optional<File> findByS3Key(String s3Key);

    /**
     * Находит все файлы пользователя.
     */
    List<File> findByUserId(UUID userId);

    /**
     * Удаляет файл по ключу в S3.
     */
    void deleteByS3Key(String s3Key);

    /**
     * Обновляет информацию о файле.
     */
    List<File> findByUserIdAndCategory(UUID userId, String category);

    @Query("SELECT DISTINCT f.category FROM File f WHERE f.user.id = :userId ORDER BY f.category")
    List<String> findDistinctCategoriesByUser(UUID userId);
}