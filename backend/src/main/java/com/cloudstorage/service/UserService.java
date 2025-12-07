package com.cloudstorage.service;

import com.cloudstorage.model.User;
import com.cloudstorage.repository.UserRepository;
import com.cloudstorage.security.PasswordEncoder;

import java.util.UUID;

/**
 * Сервис для работы с пользователями.
 * Отдельно от AuthService, чтобы разделить ответственность:
 * - AuthService: аутентификация (вход/выход/сессии)
 * - UserService: управление данными пользователей
 */
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Создаёт нового пользователя.
     * Используется администратором или при импорте данных.
     */
    public User createUser(String name, String email, String phoneNumber, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPassword(passwordEncoder.encode(password));

        return userRepository.save(user);
    }

    /**
     * Получает пользователя по ID.
     */
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    /**
     * Получает пользователя по email.
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    /**
     * Обновляет основные данные пользователя.
     */
    public User updateUser(UUID userId, String name, String email, String phoneNumber) {
        User user = getUserById(userId);

        // Если email меняется, проверяем уникальность
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }

        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);

        return userRepository.update(user);
    }

    /**
     * Обновляет пароль пользователя.
     */
    public void updatePassword(UUID userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);

        // Проверяем старый пароль
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Неверный текущий пароль");
        }

        // Устанавливаем новый пароль
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.update(user);
    }

    /**
     * Удаляет пользователя.
     */
    public void deleteUser(UUID userId) {
        // Проверяем, что пользователь существует
        getUserById(userId);

        // Удаляем пользователя
        userRepository.deleteById(userId);
    }

    /**
     * Проверяет, существует ли пользователь с таким email.
     */
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }
}