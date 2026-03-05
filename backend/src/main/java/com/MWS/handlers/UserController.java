package com.MWS.handlers;

import com.MWS.dto.create_update.CreateUserDTO;
import com.MWS.dto.get.GetSimpleUserDto;
import com.MWS.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
