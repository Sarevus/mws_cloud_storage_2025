package com.MWS.service;

import com.MWS.dto.create_update.CreateUserDTO;
import com.MWS.dto.get.GetSimpleUserDto;
import com.MWS.handlers.UserController;
import com.MWS.model.UserEntity;
import com.MWS.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class UserServiceRelease implements UserService {
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceRelease.class);

    public UserServiceRelease(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public GetSimpleUserDto createUser(CreateUserDTO userDTO) {
        // Проверяем, существует ли пользователь с таким email
        String email = userDTO.email();
        String name = userDTO.name();
        String phoneNumber = userDTO.phoneNumber();
        String password = userDTO.password();
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