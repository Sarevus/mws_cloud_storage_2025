package com.MWS.handlers;

import com.MWS.dto.create_update.CreateUserDTO;
import com.MWS.dto.get.GetSimpleUserDto;
import com.MWS.dto.login.LoginUserDTO;
import com.MWS.service.UserService;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
//import spark.Request;
//import spark.Response;


import java.util.UUID;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Обрабатывает запрос на создание нового пользователя.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public GetSimpleUserDto register(@RequestBody CreateUserDTO body) {
        if (body != null) {
            return userService.createUser(body);
        }
        throw new IllegalArgumentException("не заполнены поля");
    }

    /**
     * Обрабатывает запрос на вход в страничку пользователя
     */
    @PostMapping("/login")
    public GetSimpleUserDto login(@RequestBody LoginUserDTO body) {
        UUID userId = userService.loginUser(body.email(), body.password());
        return new GetSimpleUserDto(
                userId, null, body.email(), null
        );
    }


    /**
     * Обрабатывает запрос на получение пользователя по ID.
     */
    @GetMapping("/{id}")
    public GetSimpleUserDto getUserById(@PathVariable("id") String id) {
        try {
            UUID uuid = UUID.fromString(id);
            return userService.getUser(uuid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Некорректный формат UUID");
        }
    }


    /**
     * Обрабатывает запрос на обновление данных пользователя.
     */
    @PutMapping("/{id}")
    public GetSimpleUserDto updateUser(@PathVariable("id") String id,
                                       @RequestBody CreateUserDTO body) {
        try {
            UUID uuid = UUID.fromString(id);
            return userService.updateUser(uuid, body);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Некорректный формат UUID");
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable("id") String id) {
        try {
            UUID uuid = UUID.fromString(id);
            userService.deleteUser(uuid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Некорректный формат UUID");
        }
    }


}
