package com.MWS.service;

import com.MWS.model.User;

public interface UserService {

    void save(String userName, String email, String phoneNumber, String password);


    User createUser(User user);

    User updateUser(long id, User user);

    User getUser(long id);

    void deleteUser(long id);
}
