package com.MWS.service;

import com.MWS.model.User;
import org.springframework.stereotype.Service;

@Service
public class UserServiceRelease implements UserService {
    @Override
    public void save(String userName, String email, String phoneNumber, String password) {
    } // тут надо дописать метод, позволяющий регать нового пользователя. поработать с UserRepository (CRUD)

}
