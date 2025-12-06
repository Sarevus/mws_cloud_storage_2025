package com.MWS.handlers;

import com.MWS.dto.create_update.CreateUserDTO;
import com.MWS.dto.get.GetSimpleUserDto;
import com.MWS.dto.login.LoginUserDTO;
import com.MWS.service.UserService;
import com.google.gson.Gson;
import jakarta.persistence.EntityNotFoundException;
import spark.Request;
import spark.Response;

import java.util.UUID;

public class UserController {

    private final UserService userService;
    private final Gson gson = new Gson();

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Обрабатывает запрос на создание нового пользователя.
     */
    public Object register(Request request, Response response) {
        response.type("application/json");
        try {
            CreateUserDTO dto = gson.fromJson(request.body(), CreateUserDTO.class);
            GetSimpleUserDto createdUser = userService.createUser(dto);
            response.status(201);
            return gson.toJson(createdUser);
        } catch (IllegalArgumentException ex) {
            response.status(400);
            return gson.toJson(new ErrorResponse(ex.getMessage()));
        }
    }

    public Object login(Request request, Response response) {
        response.type("application/json");
        try {
            LoginUserDTO dto = gson.fromJson(request.body(), LoginUserDTO.class);
            UUID userId = userService.loginUser(dto.email(), dto.password());

            response.status(200);
            return gson.toJson(new GetSimpleUserDto(
                    userId, null, dto.email(), null
            ));
        } catch (IllegalArgumentException ex) {
            response.status(400);
            return gson.toJson(new ErrorResponse(ex.getMessage()));
        }
    }


    /**
     * Обрабатывает запрос на получение пользователя по ID.
     */
    public Object getUserById(Request request, Response response) {
        response.type("application/json");
        try {
            UUID id = UUID.fromString(request.params(":id"));
            GetSimpleUserDto user = userService.getUser(id);
            response.status(200);
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
        } catch (IllegalArgumentException ex) {
            response.status(404);
            return gson.toJson(new ErrorResponse(ex.getMessage()));
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