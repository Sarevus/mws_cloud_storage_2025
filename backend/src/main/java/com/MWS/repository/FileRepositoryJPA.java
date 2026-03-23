package com.MWS.repository;

import com.MWS.model.File;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class FileRepositoryJPA implements FileRepository {

    private static final Logger logger = LoggerFactory.getLogger(FileRepositoryJPA.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public File save(File file) {
        if (file.getId() == null) {
            entityManager.persist(file);
            logger.info("Файл сохранён: {}", file.getId());
        } else {
            file = entityManager.merge(file);
            logger.info("Файл обновлён: {}", file.getId());
        }
        return file;
    }

    @Override
    public Optional<File> findById(UUID id) {
        File file = entityManager.find(File.class, id);
        return Optional.ofNullable(file);
    }

    @Override
    public List<File> findByUserId(UUID userId) {
        List<File> files = entityManager.createQuery(
                        "SELECT f FROM File f WHERE f.user.id = :userId ORDER BY f.id",  // Убрали originalName
                        File.class)
                .setParameter("userId", userId)
                .getResultList();

        logger.info("Найдено {} файлов для пользователя {}", files.size(), userId);
        return files;
    }

    @Override
    public Optional<File> findByS3Key(String s3Key) {
        try {
            File file = entityManager.createQuery(
                            "SELECT f FROM File f WHERE f.s3Key = :s3Key",
                            File.class)
                    .setParameter("s3Key", s3Key)
                    .getSingleResult();
            return Optional.of(file);
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public void deleteById(UUID id) {
        File file = entityManager.find(File.class, id);
        if (file != null) {
            entityManager.remove(file);
            logger.info("Файл удалён: {}", id);
        } else {
            logger.warn("Файл с id {} не найден для удаления", id);
        }
    }

    @Override
    public void deleteAll() {
        int deleted = entityManager.createQuery("DELETE FROM File").executeUpdate();
        logger.info("Удалено {} файлов", deleted);
    }

    @Override
    public void deleteByS3Key(String s3Key) {
        int deleted = entityManager.createQuery(
                        "DELETE FROM File f WHERE f.s3Key = :s3Key")
                .setParameter("s3Key", s3Key)
                .executeUpdate();

        if (deleted > 0) {
            logger.info("Файл удалён по S3 ключу: {}", s3Key);
        } else {
            logger.warn("Файл с S3 ключом {} не найден", s3Key);
        }
    }

    @Override
    public File update(File file) {
        return save(file);  // JPA merge сделает update
    }

    @Override
    public List<File> findByUserIdAndCategory(UUID userId, String category) {
        return entityManager.createQuery(
                        "SELECT f FROM File f WHERE f.user.id = :userId AND f.category = :category ORDER BY f.id",  // Убрали originalName
                        File.class)
                .setParameter("userId", userId)
                .setParameter("category", category)
                .getResultList();
    }

    @Override
    public List<String> findDistinctCategoriesByUser(UUID userId) {
        return entityManager.createQuery(
                        "SELECT DISTINCT f.category FROM File f WHERE f.user.id = :userId ORDER BY f.category",
                        String.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}