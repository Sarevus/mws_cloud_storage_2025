package com.MWS.service;

import com.MWS.model.User;

import java.util.UUID;

public interface UserService {

    void save(String userName, String email, String phoneNumber, String password);

    User createUser(User user);

    User updateUser(UUID id, User user);

    User getUser(UUID id);

    void deleteUser(UUID id);
}
