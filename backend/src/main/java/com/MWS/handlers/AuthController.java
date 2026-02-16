package com.MWS.handlers;

import com.MWS.dto.create_update.CreateUserDTO;
import com.MWS.dto.get.GetSimpleUserDto;
import com.MWS.dto.login.LoginUserDTO;
import com.MWS.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


@RestController
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
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

}
