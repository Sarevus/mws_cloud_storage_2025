package com.cloudstorage.repository;

import com.cloudstorage.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Интерфейс для работы с пользователями в PostgreSQL.
 * Реализацию делает другой разработчик.
 */
public interface UserRepository {

    /**
     * Сохраняет пользователя в БД.
     * Возвращает сохранённого пользователя с присвоенным ID.
     */
    User save(User user);

    /**
     * Находит пользователя по его ID в БД.
     * Используется для получения информации о пользователе.
     */
    Optional<User> findById(UUID id);

    /**
     * Находит пользователя по email.
     * Используется при входе и проверке уникальности email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Проверяет, существует ли пользователь с таким email.
     * Используется при регистрации.
     */
    boolean existsByEmail(String email);

    /**
     * Удаляет пользователя по его ID.
     */
    void deleteById(UUID id);

    /**
     * Обновляет информацию о пользователе в БД.
     */
    User update(User user);
}