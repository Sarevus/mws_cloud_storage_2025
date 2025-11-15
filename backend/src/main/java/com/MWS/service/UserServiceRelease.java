package com.MWS.service;

import com.MWS.dto.create_update.CreateUserDTO;
import com.MWS.dto.get.GetSimpleUserDto;
import com.MWS.model.UserEntity;

import java.util.UUID;

public class UserServiceRelease implements UserService {
    @Override
    public void save(String userName, String email, String phoneNumber, String password) {
    } // Тут надо дописать метод, позволяющий регать нового пользователя. поработать с UserRepository (CRUD)

    @Override
    public GetSimpleUserDto createUser(CreateUserDTO userDTO) {
        return null;
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