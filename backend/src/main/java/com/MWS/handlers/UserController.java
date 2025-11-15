package com.MWS.handlers;

import com.MWS.model.UserEntity;
import com.MWS.service.UserService;
import com.MWS.Validator.ValidationResult;
import com.MWS.Validator.Validator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserEntity user) {

        ValidationResult result = Validator.validate(user);

        if (!result.isValid()) {
            return ResponseEntity.badRequest().body(result.getErrors());
        }

        String userName = user.getName();
        String email = user.getEmail();
        String phoneNumber = user.getPhoneNumber();
        String password = user.getPassword();

        userService.save(userName, email, phoneNumber, password);

        return ResponseEntity.ok("Пользователь успешно зарегистрирован!");
    }

}
