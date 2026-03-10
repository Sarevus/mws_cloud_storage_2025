package com.MWS.db.postgresql.repository;

import com.MWS.model.File;
import com.MWS.model.MetadataEntity;
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
public class FileRepositoryImpl implements FileRepository {

    private static final Logger logger = LoggerFactory.getLogger(FileRepositoryImpl.class);

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public File save(File file) {
        try {

            if (file.getId() == null) {
                em.persist(file);
                logger.info("Файл сохранён: {}", file.getId());
                return file;
            }

            File merged = em.merge(file);
            logger.info("Файл обновлён: {}", merged.getId());
            return merged;

        } catch (Exception e) {
            logger.error("Ошибка сохранения файла {}", file.getId(), e);
            throw new RuntimeException("Ошибка сохранения файла", e);
        }
    }

    @Override
    public List<File> findByUserId(UUID userId) {
        try {

            List<File> files = em.createQuery("""
                    select f
                    from File f
                    join fetch f.user u
                    left join fetch f.metadata m
                    where u.id = :userId
                    order by m.originalName asc
                    """, File.class)
                    .setParameter("userId", userId)
                    .getResultList();

            logger.info("Найдено {} файлов для пользователя {}", files.size(), userId);
            return files;

        } catch (Exception e) {
            logger.error("Ошибка поиска файлов пользователя {}", userId, e);
            throw new RuntimeException("Ошибка поиска файлов пользователя", e);
        }
    }

    @Override
    public Optional<File> findById(UUID id) {
        try {

            return em.createQuery("""
                    select f
                    from File f
                    join fetch f.user
                    left join fetch f.metadata
                    where f.id = :id
                    """, File.class)
                    .setParameter("id", id)
                    .getResultStream()
                    .findFirst();

        } catch (Exception e) {
            logger.error("Ошибка поиска файла по ID {}", id, e);
            throw new RuntimeException("Ошибка поиска файла", e);
        }
    }

    @Override
    public Optional<File> findByS3Key(String link) {
        try {

            return em.createQuery("""
                    select f
                    from File f
                    join fetch f.user
                    left join fetch f.metadata
                    where f.link = :link
                    """, File.class)
                    .setParameter("link", link)
                    .getResultStream()
                    .findFirst();

        } catch (Exception e) {
            logger.error("Ошибка поиска файла по link {}", link, e);
            throw new RuntimeException("Ошибка поиска файла по link", e);
        }
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {

        try {

            File file = em.find(File.class, id);

            if (file == null) {
                logger.warn("Файл с id {} не найден для удаления", id);
                return;
            }

            em.remove(file);

            logger.info("Файл удалён: {}", id);

        } catch (Exception e) {
            logger.error("Ошибка удаления файла {}", id, e);
            throw new RuntimeException("Ошибка удаления файла", e);
        }
    }

    @Override
    @Transactional
    public void deleteByS3Key(String link) {

        try {

            File file = em.createQuery("""
                    select f
                    from File f
                    where f.link = :link
                    """, File.class)
                    .setParameter("link", link)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);

            if (file == null) {
                logger.warn("Файл с link {} не найден", link);
                return;
            }

            em.remove(file);

            logger.info("Файл удалён по link: {}", link);

        } catch (Exception e) {
            logger.error("Ошибка удаления файла по link {}", link, e);
            throw new RuntimeException("Ошибка удаления файла", e);
        }
    }

    @Override
    @Transactional
    public File update(File file) {

        try {

            File managed = em.find(File.class, file.getId());

            if (managed == null) {
                throw new RuntimeException("Файл с id " + file.getId() + " не найден");
            }

            managed.setLink(file.getLink());
            managed.setCategory(file.getCategory());

            if (managed.getMetadata() == null) {

                MetadataEntity metadata = new MetadataEntity();
                metadata.setFile(managed);
                metadata.setIsPublic(false);

                managed.setMetadata(metadata);
            }

            managed.getMetadata().setOriginalName(file.getOriginalName());
            managed.getMetadata().setSize(file.getSize());
            managed.getMetadata().setMimeType(file.getMimeType());

            logger.info("Файл обновлён: {}", managed.getId());

            return managed;

        } catch (Exception e) {
            logger.error("Ошибка обновления файла {}", file.getId(), e);
            throw new RuntimeException("Ошибка обновления файла", e);
        }
    }

    @Override
    public List<File> findByUserIdAndCategory(UUID userId, String category) {

        try {

            List<File> files = em.createQuery("""
                    select f
                    from File f
                    join fetch f.user u
                    left join fetch f.metadata m
                    where u.id = :userId
                      and f.category = :category
                    order by m.originalName asc
                    """, File.class)
                    .setParameter("userId", userId)
                    .setParameter("category", category)
                    .getResultList();

            logger.info("Найдено {} файлов пользователя {} в категории {}",
                    files.size(), userId, category);

            return files;

        } catch (Exception e) {
            logger.error("Ошибка поиска файлов пользователя {} по категории {}",
                    userId, category, e);

            throw new RuntimeException("Ошибка поиска файлов", e);
        }
    }

    @Override
    public List<String> findDistinctCategoriesByUser(UUID userId) {

        try {

            return em.createQuery("""
                    select distinct f.category
                    from File f
                    where f.user.id = :userId
                    order by f.category
                    """, String.class)
                    .setParameter("userId", userId)
                    .getResultList();

        } catch (Exception e) {
            logger.error("Ошибка поиска категорий пользователя {}", userId, e);
            throw new RuntimeException("Ошибка поиска категорий", e);
        }
    }
}