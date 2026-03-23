package com.MWS.repository;

import com.MWS.model.UserEntity;
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
public class UserRepositoryPostgres implements UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepositoryPostgres.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public UserEntity save(UserEntity user) {
        if (user.getId() == null) {
            entityManager.persist(user);
        } else {
            user = entityManager.merge(user);
        }
        logger.info("Пользователь сохранён или обновлён: {}", user.getId());
        return user;
    }

    @Override
    public Optional<UserEntity> findById(UUID id) {
        UserEntity user = entityManager.find(UserEntity.class, id);
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        try {
            UserEntity user = entityManager.createQuery(
                            "SELECT u FROM UserEntity u WHERE u.email = :email", UserEntity.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return Optional.ofNullable(user);
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<UserEntity> findAll() {
        return entityManager.createQuery("SELECT u FROM UserEntity u", UserEntity.class)
                .getResultList();
    }

    @Override
    public void deleteById(UUID id) {
        UserEntity user = entityManager.find(UserEntity.class, id);
        if (user != null) {
            entityManager.remove(user);
            logger.info("Пользователь удалён: {}", id);
        }
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM UserEntity").executeUpdate();
        logger.info("Все пользователи удалены");
    }

    @Override
    public UserEntity update(UserEntity user) {
        return save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(u) FROM UserEntity u WHERE u.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }
}