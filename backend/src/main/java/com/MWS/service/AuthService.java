package com.MWS.service;

import com.MWS.model.UserEntity;
import com.MWS.repository.UserRepository;
import com.MWS.security.HashPassword;

import java.util.UUID;

public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity authenticate(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> HashPassword.verifyPassword(password, user.getPassword()))
                .orElse(null);
    }

    public UserEntity getUserById(UUID userId) {
        return userRepository.findById(userId).orElse(null);
    }
}