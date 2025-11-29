package com.MWS.service;

import com.MWS.Validator.ValidationResult;
import com.MWS.Validator.Validator;
import com.MWS.dto.create_update.CreateUserDTO;
import com.MWS.dto.get.GetSimpleUserDto;
import com.MWS.handlers.UserController;
import com.MWS.model.UserEntity;
import com.MWS.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.MWS.security.HashPassword;

import java.util.UUID;

public class UserServiceRelease implements UserService {
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceRelease.class);

    public UserServiceRelease(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public GetSimpleUserDto createUser(CreateUserDTO userDTO){

        ValidationResult validationResult = Validator.validate(userDTO);
        if (!validationResult.isValid() || !validationResult.getErrors().isEmpty()) {
            String message = String.join("; ", validationResult.getErrors());
            logger.warn("Ошибка валидации данных пользователя: {}", message);
            throw new IllegalArgumentException("Некорректные данные пользователя: " + message);
        }

        /**
         * Заполняем переменные значениями из dto. Пароль переводим в хэшированный
         */

        String email = userDTO.email();
        String name = userDTO.name();
        String phoneNumber = userDTO.phoneNumber();
        String password = HashPassword.createPasswordHash(userDTO.password());

        /**
         * Проверяем есть ли в бд пользователь с таким же email
         */
        userRepository.findByEmail(email).ifPresent(existingUser -> {
            logger.warn("Пользователь с email {} уже существует.", email);
            throw new IllegalArgumentException("Email " + email + " уже занят.");
        });
        UserEntity user = new UserEntity();
        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPassword(password);


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
        return null;
    }

    @Override
    public GetSimpleUserDto getUser(UUID id) {
        return null;
    }

    @Override
    public void deleteUser(UUID id) {

    }

}