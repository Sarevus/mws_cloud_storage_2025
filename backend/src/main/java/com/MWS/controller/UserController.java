package com.MWS.controller;
import com.MWS.model.User;
import com.MWS.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/")
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);

    }

    @GetMapping("/{ID}")
    public User getUser(@PathVariable UUID ID) {
        return userService.getUser(ID);

    }

    @PutMapping("/{ID}")
    public User updateUser(@PathVariable UUID ID, @RequestBody User user) {
        return userService.updateUser(ID, user);

    }

    @DeleteMapping("/{ID}")
    public ResponseEntity<Object> deleteUser(@PathVariable UUID ID) {
        userService.deleteUser(ID);
        return ResponseEntity.noContent().build();
    }
}
