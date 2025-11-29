package com.MWS.repository;

import com.MWS.model.UserEntity;

import java.util.UUID;
import java.util.Optional; // Используем Optional для методов, которые могут ничего не найти

public interface UserRepository {

    /**
     * Регает нового пользователя или обновляет существующего
     * @param user пользователь для сохранения
     */
    UserEntity save(UserEntity user);

    /**
     * Ищет пользователя по его ID.
     * @param id id пользователя
     * @return Optional инфа про пользователя или пустой если не найден
     */
    Optional<UserEntity> findById(UUID id);

    /**
     * Удаляет пользователя по его ID.
     * @param id id пользователя
     */
    void deleteById(UUID id);

    UserEntity update(UserEntity user);

    Optional<UserEntity> findByEmail(String email);
}
