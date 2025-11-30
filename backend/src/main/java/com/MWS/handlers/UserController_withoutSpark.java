package com.MWS.handlers;

import com.MWS.dto.create_update.CreateUserDTO;
import com.MWS.dto.get.GetSimpleUserDto;
import com.MWS.service.UserService;
import com.google.gson.Gson;
import jakarta.persistence.EntityNotFoundException;
//import spark.Request;
//import spark.Response;

import java.util.UUID;

public class UserController_withoutSpark {

    private final UserService userService;
    private final Gson gson = new Gson();

    public UserController_withoutSpark(UserService userService) {
        this.userService = userService;
    }

    /**
     * Обрабатывает запрос на создание нового пользователя.
     */
    public ControllerResult register(String requestBody) {
        try {
            CreateUserDTO dto = gson.fromJson(requestBody, CreateUserDTO.class);
            GetSimpleUserDto createdUser = userService.createUser(dto);

            String json = gson.toJson(createdUser);
            return new ControllerResult(201, json);
        } catch (Exception e) {
            String json = gson.toJson(new ErrorResponse("Некорректные данные: " + e.getMessage()));
            return new ControllerResult(400, json);
        }
    }

    /**
     * Обрабатывает запрос на получение пользователя по ID.
     */
    public ControllerResult getUserById(String requestBody) {
        try {
            UUID id = UUID.fromString(requestBody);
            GetSimpleUserDto user = userService.getUser(id);
            String json = gson.toJson(user);
            return new ControllerResult(200, json);
        } catch (EntityNotFoundException e) {
            String json = gson.toJson(new ErrorResponse(e.getMessage()));
            return new ControllerResult(204, json);
        } catch (IllegalArgumentException e) {
            String json = gson.toJson(new ErrorResponse("Некорректный формат UUID"));
            return new ControllerResult(400, json);
        }
    }

    /**
     * Обрабатывает запрос на обновление данных пользователя.
     */
    public ControllerResult updateUser(String idParam, String requestBody) {
        try {
            UUID id = UUID.fromString(idParam);
            CreateUserDTO dto = gson.fromJson(requestBody, CreateUserDTO.class);
            GetSimpleUserDto updatedUser = userService.updateUser(id, dto);
            String json = gson.toJson(updatedUser);
            return new ControllerResult(200, json);
        } catch (EntityNotFoundException e) {
            String json = gson.toJson(new ErrorResponse(e.getMessage()));
            return new ControllerResult(404, json);
        } catch (IllegalArgumentException e) {
            String json = gson.toJson(new ErrorResponse("Некорректный формат UUID"));
            return new ControllerResult(400, json);
        }
    }

    /**
     * Обрабатывает запрос на удаление пользователя.
     */
    public ControllerResult deleteUser(String idParam) {
        try {
            UUID id = UUID.fromString(idParam);
            userService.deleteUser(id);
            // 204 No Content – без тела
            return new ControllerResult(204, "");
        } catch (EntityNotFoundException e) {
            String json = gson.toJson(new ErrorResponse(e.getMessage()));
            return new ControllerResult(404, json);
        } catch (IllegalArgumentException e) {
            String json = gson.toJson(new ErrorResponse("Некорректный формат UUID"));
            return new ControllerResult(400, json);
        }
    }

    private static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}