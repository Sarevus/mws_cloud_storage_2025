package com.MWS.controller;
import com.MWS.model.User;
import com.MWS.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}")
    public User getUser(@PathVariable long id) {
        return userService.getUser(id);

    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable long id, @RequestBody User user) {
        return userService.updateUser(id, user);

    }

    @DeleteMapping("/{id}")
    public User deleteUser(@PathVariable long id) {
        userService.deleteUser(id);
        return null;
    }
}
