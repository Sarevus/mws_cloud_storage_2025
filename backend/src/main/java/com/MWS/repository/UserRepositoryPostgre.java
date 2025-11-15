package com.MWS.repository;

import com.MWS.model.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Optional;
import java.util.UUID;

public class UserRepositoryPostgre implements UserRepository {

    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("postgresql");
    private static final Logger logger = LoggerFactory.getLogger(UserRepositoryPostgre.class);

    @Override
    public void save(UserEntity user) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(user);
            em.getTransaction().commit();
            logger.info("Пользователь сохранен или обновлен: {}", user.getId());
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            logger.error("Ошибка при сохранении пользователя с id {}", user.getId(), e);
            throw new RuntimeException("Не удалось сохранить или обновить пользователя", e);
        } finally {
            em.close();
        }
    }

    @Override
    public Optional<UserEntity> findById(UUID id) {
        EntityManager em = emf.createEntityManager();
        try {
            UserEntity user = em.find(UserEntity.class, id); // ищем по запросу
            return Optional.ofNullable(user); // если null то будет пустым иначе отдаем результат запроса
        } finally {
            em.close();
        }
    }

    @Override
    public void deleteById(UUID id) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            UserEntity userToDelete = em.find(UserEntity.class, id);
            if (userToDelete != null) {
                em.remove(userToDelete);
                logger.info("Пользователь удален: {}", id);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            logger.error("Ошибка при удалении пользователя с id {}", id, e);
            throw new RuntimeException("Не удалось удалить пользователя с id: " + id, e);
        } finally {
            em.close();
        }
    }
}
