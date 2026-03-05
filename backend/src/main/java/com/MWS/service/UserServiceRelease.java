package com.MWS.service;

import com.MWS.Validator.ValidationResult;
import com.MWS.Validator.Validator;
import com.MWS.db.postgresql.repository.UserRepository;
import com.MWS.dto.create_update.CreateUserDTO;
import com.MWS.dto.get.GetSimpleUserDto;
import com.MWS.model.UserEntity;
import com.MWS.security.HashPassword;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class UserServiceRelease implements UserService {
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceRelease.class);

    public UserServiceRelease(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    /**
     * Регистрация пользователя
     *
     * @param userDTO - данные о пользователе
     */
    @Override
    public GetSimpleUserDto createUser(CreateUserDTO userDTO) {
        // проверка на валидность вводимых данных
        ValidationResult validationResult = Validator.validate(userDTO);
        if (!validationResult.isValid() || !validationResult.getErrors().isEmpty()) {
            String message = String.join("; ", validationResult.getErrors());
            logger.warn("Ошибка валидации данных пользователя: {}", message);
            throw new IllegalArgumentException("Некорректные данные пользователя: " + message);
        }


        // Заполняем переменные значениями из dto. Пароль переводим в хэшированный
        String email = userDTO.email();
        String name = userDTO.name();
        String phoneNumber = userDTO.phoneNumber();
        String password = HashPassword.createPasswordHash(userDTO.password());


        // Проверяем есть ли в бд пользователь с таким же email
        userRepository.findByEmail(email).ifPresent(existingUser -> {
            logger.warn("Пользователь с email {} уже существует.", email);
            throw new IllegalArgumentException("Email " + email + " уже занят.");
        });


        // заполняем UserEntity
        UserEntity user = new UserEntity();
        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPassword(password);

        // сохраняем user
        UserEntity savedUser = userRepository.save(user);

        return new GetSimpleUserDto(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getPhoneNumber()
        );
    }


    @Override
    public GetSimpleUserDto updateUser(UUID id, CreateUserDTO userDTO) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Такого пользователя не существует: " + id));


        ValidationResult validationResult = Validator.validate(userDTO);
        if (!validationResult.isValid() || !validationResult.getErrors().isEmpty()) {
            String message = String.join("; ", validationResult.getErrors());
            logger.warn("Ошибка валидации данных пользователя: {}", message);
            throw new IllegalArgumentException("Некорректные данные пользователя: " + message);
        }


        // Заполняем переменные значениями из dto. Пароль переводим в хэшированный
        String email = userDTO.email();
        String name = userDTO.name();
        String phoneNumber = userDTO.phoneNumber();
        String password = HashPassword.createPasswordHash(userDTO.password());

        // Проверяем есть ли в бд пользователь с таким же email
        userRepository.findByEmail(email).ifPresent(existingUser -> {
            if (!(existingUser.getId().equals(id))) {
                logger.warn("Пользователь с email {} уже существует.", email);
                throw new IllegalArgumentException("Email " + email + " уже занят.");
            }
        });


        // заполняем UserEntity

        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPassword(password);


        // сохраняем user
        UserEntity savedUser = userRepository.update(user);

        return new GetSimpleUserDto(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getPhoneNumber()
        );
//        return null;
    }


    /**
     * Возвращает данные о пользователе по его id
     *
     * @param id - id пользователя
     */
    @Override
    public GetSimpleUserDto getUser(UUID id) {
        // Если такого пользователя нет возвращаем exception
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + id));

        // Если есть, то возвращаем его данные
        return new GetSimpleUserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber()
        );
    }

    /**
     * Удаление пользователя по id
     *
     * @param id
     */
    @Override
    public void deleteUser(UUID id) {
        // Если такого пользователя нет возвращаем exception
        userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + id));

        userRepository.deleteById(id);

        logger.info("Пользователь {} удалён", id);
    }

    @Override
    public UUID loginUser(String email, String rawPassword) {

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        if (!HashPassword.verifyPassword(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Неверный пароль");
        }

        return user.getId();


    }

}