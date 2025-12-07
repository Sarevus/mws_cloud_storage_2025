package com.cloudstorage.service;

import com.cloudstorage.dto.request.CreateUserDTO;
import com.cloudstorage.dto.request.GetSimpleUserDto;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.UserRepository;
import com.cloudstorage.security.PasswordEncoder;
import com.cloudstorage.security.SessionManager;

import java.util.UUID;

/**
 * Сервис для аутентификации и управления пользователями.
 */
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionManager sessionManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionManager = new SessionManager();
    }

    /**
     * Регистрирует нового пользователя.
     * Регистрация с DTO.
     */
    public GetSimpleUserDto register(CreateUserDTO request){
        // Проверяем уникальность email
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }

        // Создаём пользователя
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setPassword(passwordEncoder.encode(request.password()));

        // Сохраняем
        User savedUser = userRepository.save(user);

        // Возвращаем DTO ответа
        return new GetSimpleUserDto(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getPhoneNumber()
        );
    }

    /**
     * Выполняет вход пользователя.
     * Возвращает sessionId для установки в куки.
     */
    public String login(String email, String password) {
        // Находим пользователя по email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Неверный email или пароль"));

        // Проверяем пароль
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Неверный email или пароль");
        }

        // Создаём сессию
        return sessionManager.createSession(user.getId());
    }

    /**
     * Выполняет выход пользователя.
     */
    public void logout(String sessionId) {
        sessionManager.invalidateSession(sessionId);
    }

    /**
     * Получает пользователя по sessionId.
     */
    public User getUserFromSession(String sessionId) {
        UUID userId = sessionManager.getUserId(sessionId);
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    /**
     * Проверяет валидность сессии.
     */
    public boolean isValidSession(String sessionId) {
        return sessionManager.isValidSession(sessionId);
    }

    /**
     * Получает пользователя по ID.
     */
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    /**
     * Обновляет информацию о пользователе.
     */
    public User updateUser(UUID userId, String name, String email, String phoneNumber) {
        User user = getUserById(userId);

        // Если меняем email, проверяем уникальность
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
        // Удаляем все сессии пользователя
        sessionManager.invalidateAllUserSessions(userId);

        // Удаляем пользователя из БД
        userRepository.deleteById(userId);
    }
}