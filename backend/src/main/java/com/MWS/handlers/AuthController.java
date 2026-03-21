package com.MWS.handlers;

import com.MWS.dto.create_update.CreateUserDTO;
import com.MWS.dto.get.GetSimpleUserDto;
import com.MWS.dto.login.LoginUserDTO;
import com.MWS.service.EmailService;
import com.MWS.service.UserService;
import com.MWS.service.VerificationCodeService;
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
    private final EmailService emailService;
    private final VerificationCodeService verificationCodeService;

    @Autowired
    public AuthController(UserService userService, EmailService emailService, VerificationCodeService verificationCodeService) {
        this.userService = userService;
        this.emailService = emailService;
        this.verificationCodeService = verificationCodeService;
    }


    @PostMapping("/register/request")
    public void requestRegister(@RequestBody CreateUserDTO body) {
        int code = (int) (Math.random() * 900000) + 100000;

        emailService.sendTextEmail(body.email(), "Код подтверждения", "Ваш код: " + code);

        verificationCodeService.savePendingUser(body, code);

        logger.info("Код подтверждения отправлен на email: {}", body.email());
    }

    /**
     * Проверяем код и только теперь создаем аккаунт.
     */
    @PostMapping("/register/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public GetSimpleUserDto confirm(@RequestParam String email, @RequestParam int code, HttpSession session) {
        if (verificationCodeService.verifyCode(email, code)) {
            CreateUserDTO body = verificationCodeService.getPendingUser(email);
            GetSimpleUserDto user = userService.createUser(body);
            verificationCodeService.removeCode(email);
            session.setAttribute("userId", user.id());
            session.setAttribute("email", user.email());
            return user;
        }
        throw new RuntimeException("Invalid code");
    }


    /**
     * Обрабатывает запрос на создание нового пользователя.
     */

//    @PostMapping("/register")
//    @ResponseStatus(HttpStatus.CREATED) // возвращает код 201 - пользователь создан
//    public GetSimpleUserDto register(@RequestBody CreateUserDTO body, HttpSession session) {
//        GetSimpleUserDto user = userService.createUser(body);
//
//        session.setAttribute("userId", user.id());
//        session.setAttribute("email", user.email());
//
//        logger.info("Пользователь с email {} зарегистрирован и вошёл", user.email());
//
//        return user;
//
//    }

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
