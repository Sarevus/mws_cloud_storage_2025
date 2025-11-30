package com.MWS.service;

import com.MWS.dto.create_update.CreateUserDTO;
import com.MWS.dto.get.GetSimpleUserDto;
import com.MWS.model.UserEntity;

import java.util.UUID;

public interface UserService {

    GetSimpleUserDto createUser(CreateUserDTO userDTO);

    GetSimpleUserDto updateUser(UUID id, CreateUserDTO userDTO);

    GetSimpleUserDto getUser(UUID id);

    void deleteUser(UUID id);
}
