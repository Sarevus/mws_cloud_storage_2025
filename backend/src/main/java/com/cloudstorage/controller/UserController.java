package com.cloudstorage.controller;

import com.cloudstorage.dto.request.GetSimpleUserDto;
import com.cloudstorage.dto.request.PutUserDto;
import com.cloudstorage.dto.request.PutUserPasswordDto;
import com.cloudstorage.model.User;
import com.cloudstorage.service.UserService;
import com.google.gson.Gson;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Контроллер для управления пользователями.
 */
public class UserController {
    private final Gson gson = new Gson();
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Получает текущего пользователя.
     * GET /user
     */
    public Object getCurrentUser(Request req, Response res) {
        try {
            // Получаем user_id из куки (AuthMiddleware должен установить)
            String userIdStr = req.cookie("user_id");
            if (userIdStr == null) {
                return error(res, 401, "Пользователь не авторизован");
            }

            UUID userId = UUID.fromString(userIdStr);
            User user = userService.getUserById(userId);

            GetSimpleUserDto userDto = new GetSimpleUserDto(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getPhoneNumber()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", userDto);

            res.type("application/json");
            return gson.toJson(response);

        } catch (Exception e) {
            return error(res, 401, "Пользователь не найден: " + e.getMessage());
        }
    }

    /**
     * Обновляет данные пользователя.
     * PUT /user
     */
    public Object updateUser(Request req, Response res) {
        try {
            String userIdStr = req.cookie("user_id");
            if (userIdStr == null) {
                return error(res, 401, "Пользователь не авторизован");
            }

            UUID userId = UUID.fromString(userIdStr);
            PutUserDto request = gson.fromJson(req.body(), PutUserDto.class);

            // Валидация
            if (request.name() == null || request.name().trim().isEmpty()) {
                return error(res, 400, "Имя обязательно");
            }
            if (request.email() == null || request.email().trim().isEmpty()) {
                return error(res, 400, "Email обязателен");
            }
            if (request.phoneNumber() == null || request.phoneNumber().trim().isEmpty()) {
                return error(res, 400, "Телефон обязателен");
            }

            User user = userService.updateUser(
                    userId,
                    request.name(),
                    request.email(),
                    request.phoneNumber()
            );

            GetSimpleUserDto userDto = new GetSimpleUserDto(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getPhoneNumber()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Данные пользователя обновлены");
            response.put("user", userDto);

            res.type("application/json");
            return gson.toJson(response);

        } catch (RuntimeException e) {
            return error(res, 400, e.getMessage());
        } catch (Exception e) {
            return error(res, 500, "Ошибка обновления пользователя: " + e.getMessage());
        }
    }

    /**
     * Обновляет пароль пользователя.
     * PUT /user/password
     */
    public Object updatePassword(Request req, Response res) {
        try {
            String userIdStr = req.cookie("user_id");
            if (userIdStr == null) {
                return error(res, 401, "Пользователь не авторизован");
            }

            UUID userId = UUID.fromString(userIdStr);
            PutUserPasswordDto request = gson.fromJson(req.body(), PutUserPasswordDto.class);

            // Валидация
            if (request.oldPassword() == null || request.oldPassword().trim().isEmpty()) {
                return error(res, 400, "Старый пароль обязателен");
            }
            if (request.newPassword() == null || request.newPassword().length() < 6) {
                return error(res, 400, "Новый пароль должен быть минимум 6 символов");
            }

            userService.updatePassword(userId, request.oldPassword(), request.newPassword());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Пароль успешно обновлён");

            res.type("application/json");
            return gson.toJson(response);

        } catch (RuntimeException e) {
            return error(res, 400, e.getMessage());
        } catch (Exception e) {
            return error(res, 500, "Ошибка обновления пароля: " + e.getMessage());
        }
    }

    /**
     * Удаляет пользователя.
     * DELETE /user
     */
    public Object deleteUser(Request req, Response res) {
        try {
            String userIdStr = req.cookie("user_id");
            if (userIdStr == null) {
                return error(res, 401, "Пользователь не авторизован");
            }

            UUID userId = UUID.fromString(userIdStr);

            userService.deleteUser(userId);

            // Очищаем куки
            res.cookie("session_id", "", 0);
            res.cookie("user_id", "", 0);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Пользователь удалён");

            res.type("application/json");
            return gson.toJson(response);

        } catch (Exception e) {
            return error(res, 500, "Ошибка удаления пользователя: " + e.getMessage());
        }
    }

    /**
     * Вспомогательный метод для возврата ошибок.
     */
    private String error(Response res, int status, String message) {
        res.status(status);
        res.type("application/json");
        return "{\"success\": false, \"error\": \"" + message + "\"}";
    }
}