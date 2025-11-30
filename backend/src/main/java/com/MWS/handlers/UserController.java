package com.MWS.handlers;

import com.MWS.dto.create_update.CreateUserDTO;
import com.MWS.dto.get.GetSimpleUserDto;
import com.MWS.model.UserEntity;
import com.MWS.service.AuthService;
import com.MWS.service.UserService;
import com.google.gson.Gson;
import jakarta.persistence.EntityNotFoundException;
import spark.Request;
import spark.Response;

import java.util.UUID;

public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final Gson gson = new Gson();

    // Старый конструктор (оставляем для обратной совместимости)
    public UserController(UserService userService) {
        this.userService = userService;
        this.authService = null;
    }


    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    /**
     * Обрабатывает запрос на создание нового пользователя.
     */
    public Object register(Request request, Response response) {
        response.type("application/json");

        CreateUserDTO dto = gson.fromJson(request.body(), CreateUserDTO.class);
        GetSimpleUserDto createdUser = userService.createUser(dto);

        response.status(201);
        return gson.toJson(createdUser);
    }

    public Object login(Request request, Response response) {
        response.type("application/json");

        try {
            LoginRequest loginData = gson.fromJson(request.body(), LoginRequest.class);

            // НЕПРАВИЛЬНО: AuthService.authenticate(...) - статический вызов
            // ПРАВИЛЬНО: authService.authenticate(...) - через экземпляр

            UserEntity user = authService.authenticate(loginData.email, loginData.password);

            if (user == null) {
                response.status(401);
                return gson.toJson(new ErrorResponse("Неверный email или пароль"));
            }

            // Создаем сессию в cookie
            String sessionId = UUID.randomUUID().toString();
            response.cookie("SESSION_ID", sessionId, 3600);
            response.cookie("UserId", user.getId().toString(), 3600);

            return gson.toJson(new GetSimpleUserDto(
                    user.getId(), user.getName(), user.getEmail(), user.getPhoneNumber()
            ));

        } catch (Exception e) {
            response.status(400);
            return gson.toJson(new ErrorResponse("Ошибка входа: " + e.getMessage()));
        }
    }

    private static class LoginRequest {
        String email;
        String password;
    }

    /**
     * Обрабатывает запрос на получение пользователя по ID.
     */
    public Object getUserById(Request request, Response response) {
        response.type("application/json");
        try {
            UUID id = UUID.fromString(request.params(":id"));
            GetSimpleUserDto user = userService.getUser(id);
            return gson.toJson(user);
        } catch (EntityNotFoundException e) {
            response.status(404);
            return gson.toJson(new ErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            response.status(400);
            return gson.toJson(new ErrorResponse("Некорректный формат UUID"));
        }
    }

    /**
     * Обрабатывает запрос на обновление данных пользователя.
     */
    public Object updateUser(Request request, Response response) {
        response.type("application/json");
        try {
            UUID id = UUID.fromString(request.params(":id"));
            CreateUserDTO dto = gson.fromJson(request.body(), CreateUserDTO.class);
            GetSimpleUserDto updatedUser = userService.updateUser(id, dto);
            return gson.toJson(updatedUser);
        } catch (EntityNotFoundException e) {
            response.status(404);
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Обрабатывает запрос на удаление пользователя.
     */
    public Object deleteUser(Request request, Response response) {
        UUID id = UUID.fromString(request.params(":id"));
        userService.deleteUser(id);
        response.status(204);
        return "";
    }

    private static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}