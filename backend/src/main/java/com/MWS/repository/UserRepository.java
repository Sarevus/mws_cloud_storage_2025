package com.MWS.repository;

import com.MWS.model.User;

import java.util.List;
import java.util.UUID;
import java.util.Optional; // Используем Optional для методов, которые могут ничего не найти

public interface UserRepository {

    /**
     * Регает нового пользователя или обновляет существующего
     * @param user пользователь для сохранения
     */
    User save(User user);

    /**
     * Ищет пользователя по его ID.
     * @param id id пользователя
     * @return Optional инфа про пользователя или пустой если не найден
     */
    Optional<User> findById(UUID id);

    List<User> findAll();

    /**
     * Удаляет пользователя по его ID.
     * @param id id пользователя
     */
    void deleteById(UUID id);

    void deleteAll();

    User update(User user);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    User updateSubscription(User user);
}
