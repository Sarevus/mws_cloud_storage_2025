package com.MWS.handlers;

import com.MWS.dto.create_update.CreateUserDTO;
import com.MWS.dto.get.GetSimpleUserDto;
import com.MWS.dto.login.LoginUserDTO;
import com.MWS.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Обрабатывает запрос на создание нового пользователя.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED) // возвращает код 201 - пользователь создан
    public GetSimpleUserDto register(@RequestBody CreateUserDTO body, HttpSession session) {
        GetSimpleUserDto user = userService.createUser(body);

        session.setAttribute("userId", user.id());
        session.setAttribute("email", user.email());

        logger.info("Пользователь с email {} зарегистрирован и вошёл", user.email());

        return user;

    }

    /**
     * Обрабатывает запрос на вход в страничку пользователя
     */
    @PostMapping("/login")
    public GetSimpleUserDto login(@RequestBody LoginUserDTO body, HttpSession session) {
        UUID userId = userService.loginUser(body.email(), body.password());
        GetSimpleUserDto user = userService.getUser(userId);

        session.setAttribute("userId", userId);
        session.setAttribute("email", body.email());

        logger.info("Пользователь с email {} вошёл", body.email());

        return user;
    }

    @PostMapping("/logout")
    public void logout(HttpSession session) {
        session.invalidate();

        logger.info("Пользователь вышёл");
    }

    @GetMapping("/me")
    public GetSimpleUserDto getCurrentUser(HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");

        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не зарегистрирован");
        }

        return userService.getUser(userId);
    }
}
