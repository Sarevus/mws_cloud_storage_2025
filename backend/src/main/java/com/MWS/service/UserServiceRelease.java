package com.MWS.service;

import com.MWS.model.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceRelease implements UserService {
    @Override
    public void save(String userName, String email, String phoneNumber, String password) {
    } // тут надо дописать метод, позволяющий регать нового пользователя. поработать с UserRepository (CRUD)

    @Override
    public User createUser(User user) {
        return null;
    }

    @Override
    public User updateUser(UUID id, User user) {
        return null;
    }

    @Override
    public User getUser(UUID id) {
        return null;
    }

    @Override
    public void deleteUser(UUID id) {

    }

}