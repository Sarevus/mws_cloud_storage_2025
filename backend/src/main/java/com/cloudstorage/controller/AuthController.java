package com.cloudstorage.controller;

import com.cloudstorage.dto.request.CreateUserDTO;
import com.cloudstorage.dto.request.GetSimpleUserDto;
import com.cloudstorage.dto.request.LoginRequest;
import com.cloudstorage.model.User;
import com.cloudstorage.security.PasswordEncoder;
import com.cloudstorage.service.AuthService;
import com.google.gson.Gson;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Контроллер для регистрации и входа.
 * Пока работает без БД, только в памяти.
 */
public class AuthController {
    private final Gson gson = new Gson();
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    /**
     * регистрация нового пользователя
     * @param req
     * @param res
     * @return статус создания
     */
    public Object register(Request req, Response res){
        try {
            CreateUserDTO request = gson.fromJson(req.body(), CreateUserDTO.class);

            /**
             * проверка полей, тк они обязательны
             */
            // Проверяем обязательные поля
            if (request.name() == null || request.name().trim().isEmpty()) {
                return error(res, 400, "Имя обязательно");
            }
            if (request.email() == null || request.email().trim().isEmpty()) {
                return error(res, 400, "Email обязателен");
            }
            if (request.phoneNumber() == null || request.phoneNumber().trim().isEmpty()) {
                return error(res, 400, "Телефон обязателен");
            }
            if (request.password() == null || request.password().length() < 6) {
                return error(res, 400, "Пароль должен быть минимум 6 символов");
            }

            GetSimpleUserDto userDto = authService.register(request);
            String sessionId = authService.login(request.email(), request.password());

            // Устанавливаем куку
            res.cookie("session_id", sessionId, 3600);

            /**
             * возвращаем ответ
             */
            System.out.println("пользователь зарегестрирован");
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Пользователь создан");
            response.put("user", userDto);

            res.type("application/json");
            res.status(201);
            return gson.toJson(response);

        } catch (RuntimeException e) {
            return error(res, 400, e.getMessage());
        } catch (Exception e) {
            return error(res, 500, "Ошибка сервера");
        }
    }

    public Object login(Request req, Response res){
        try{
            // Используем LoginRequest DTO
            LoginRequest request = gson.fromJson(req.body(), LoginRequest.class);

            if (request.email() == null || request.email().trim().isEmpty()) {
                return error(res, 400, "Email обязателен");
            }
            if (request.password() == null) {
                return error(res, 400, "Пароль обязателен");
            }


            String sessionId = authService.login(request.email(), request.password());
            User user = authService.getUserFromSession(sessionId);

            if (user == null) {
                return error(res, 500, "Ошибка создания сессии");
            }

            GetSimpleUserDto userDto = new GetSimpleUserDto(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getPhoneNumber()
            );

            res.cookie("session_id", sessionId, 3600);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Вход выполнен");
            response.put("user", userDto);

            res.type("application/json");
            return gson.toJson(response);

        } catch (RuntimeException e) {
            return error(res, 401, e.getMessage());
        } catch (Exception e) {
            return error(res, 500, "Ошибка сервера");
        }
    }

    public Object logout(Request req, Response res){
        String sessionId = req.cookie("session_id");

        if (sessionId != null) {
            authService.logout(sessionId);
        }

        res.cookie("session_id", "", 0);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Выход выполнен");

        res.type("application/json");
        return gson.toJson(response);
    }


    //возвращает всё кроме пароля текущего пользователя
    // todo возможно стоит урезать до возврата только id/email
    public Object getCurrentUser(Request req, Response res) {
        String sessionId = req.cookie("session_id");

        if (sessionId == null) {
            return error(res, 401, "Не авторизован");
        }

        User user = authService.getUserFromSession(sessionId);
        if (user == null) {
            return error(res, 401, "Сессия недействительна");
        }

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
    }

    //упрощённый метод для возврата ошибок
    private String error(Response res, int status, String message) {
        res.status(status);
        res.type("application/json");
        return "{\"success\": false, \"error\": \"" + message + "\"}";
    }
}
