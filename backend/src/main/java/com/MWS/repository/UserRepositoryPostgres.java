package com.MWS.repository;

import com.MWS.model.User;
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
    public User save(User user) {
        if (user.getId() == null) {
            entityManager.persist(user);
        } else {
            user = entityManager.merge(user);
        }
        logger.info("Пользователь сохранён или обновлён: {}", user.getId());
        return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
        User user = entityManager.find(User.class, id);
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try {
            User user = entityManager.createQuery(
                            "SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return Optional.ofNullable(user);
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<User> findAll() {
        return entityManager.createQuery("SELECT u FROM User u", User.class)
                .getResultList();
    }

    @Override
    public void deleteById(UUID id) {
        User user = entityManager.find(User.class, id);
        if (user != null) {
            entityManager.remove(user);
            logger.info("Пользователь удалён: {}", id);
        }
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM User").executeUpdate();
        logger.info("Все пользователи удалены");
    }

    @Override
    public User update(User user) {
        return save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public User updateSubscription(User user) {
        User managed = entityManager.merge(user);

        // Обновляем storage_limit при изменении подписки
        if (user.getSubscriptionId() != null) {
            entityManager.createNativeQuery(
                            "UPDATE users SET storage_limit = (SELECT storage_limit_bytes FROM subscription_plans WHERE id = ?) " +
                                    "WHERE id = ?"
                    )
                    .setParameter(1, user.getSubscriptionId())
                    .setParameter(2, user.getId())
                    .executeUpdate();
        }

        logger.info("Пользователь обновлён: {}", managed.getId());
        return managed;
    }
}