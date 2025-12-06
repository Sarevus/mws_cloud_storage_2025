package com.cloudstorage.controller;

import com.cloudstorage.model.User;
import com.cloudstorage.security.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder = new PasswordEncoder();


    //временное хранилище пользователей в памяти
    //ключ email значение user
    private final Map<String, User> users = new ConcurrentHashMap<>();


    /**
     * регистрация нового пользователя
     * @param request
     * @param res
     * @return статус создания
     */
    public Object register(Request request, Response res){
        try {
            /**
             * читаем данные из тела запроса для безопасности
             */
            String body = request.body();
            var data = gson.fromJson(body, Map.class);

            String name = (String) data.get("name");
            String email = (String) data.get("email");
            String rawPassword = (String) data.get("password");


            /**
             * проверка полей, тк они обязательны
             */
            // Проверяем обязательные поля
            if (name == null || name.trim().isEmpty()) {
                return error(res, 400, "Имя обязательно");
            }
            if (email == null || email.trim().isEmpty()) {
                return error(res, 400, "Email обязателен");
            }
            if (rawPassword == null || rawPassword.length() < 6) {
                return error(res, 400, "Пароль должен быть минимум 6 символов");
            }

            if (users.containsKey(email)) {
                return error(res, 400, "Пользователь с таким email уже существует");
            }



            //хэширование пароля
            String hashedPassword = passwordEncoder.encode(rawPassword);


            /**
             * создание пользователя,
             */
            User user = new User(name, email, hashedPassword);


            //сохранение пользователя
            users.put(email,user);

            /**
             * возвращаем ответ
             */
            System.out.println("пользователь зарегестрирован");
            res.type("application/json");
            res.status(201);

            //сам ответ
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "пользователь создан");
            response.put("userId", user.getId().toString());
            response.put("name", user.getName());
            response.put("email", user.getEmail());

            return gson.toJson(response);
        }
        //видимо всё пошло не по плану и какая-то странная ошибка
        catch (Exception e) {
            res.status(500);
            return "{\"error\": \"Ошибка сервера: " + e.getMessage() + "\"}";
        }
    }

    public Object login(Request req, Response res){
        try{
            /**
             * читаем данные из тела запроса для безопасности
             */
            String body = req.body();
            var data = gson.fromJson(body, Map.class);

            String email = (String) data.get("email");
            String rawPassword = (String) data.get("password");


            //проверка ввода
            if (email == null || rawPassword == null){

                return error(res, 400, "Email и пароль обязательны");
            }

            //проверка в бд
            User user = users.get(email);
            if
            (
                user == null ||
                !passwordEncoder.matches(rawPassword, user.getPassword())
            ) {
                return error(res, 401, "неверный email или пароль");
            }

            System.out.println("пользователь вошёл");

            //создаём сессию
            // пока только иммитация, поэтому
            //todo: создание сессий

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Вход выполнен");
            response.put("userId", user.getId().toString());
            response.put("name", user.getName());
            response.put("email", user.getEmail());

            //устанавливаем cookie в качестве сессии
            res.cookie("user_id", user.getId().toString(), 3600);

            return gson.toJson(response);
        } catch (Exception e){
            return error(res, 500, "Ошибка сервера");
        }
    }

    public Object logout(Request req, Response res){
        res.cookie("user_id", "", 0);

        res.type("application/json");
        return "{\"success\": true, \"message\": \"Выход выполнен\"}";
    }


    //возвращает всё кроме пароля текущего пользователя
    // todo возможно стоит урезать до возврата только id/email
    public Object getCurrentUser(Request req, Response res){
        String userId = req.cookie("user_id");

        if (userId == null){
            return error(res, 401, "не авторизован");
        }

        //поиск юзера по id
        User user = findUserById(UUID.fromString(userId));
        if (user == null){
            return error(res, 401, "пользователь не найден");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("userId", user.getId().toString());
        response.put("name", user.getName());
        response.put("email", user.getEmail());

        return gson.toJson(response);
    }


    /**
     * Вспомогательный метод для поиска пользователя по ID.
     */
    private User findUserById(UUID id) {
        for (User user : users.values()) {
            if (user.getId().equals(id)) {
                return user;
            }
        }
        return null;
    }


    //упрощённый метод для возврата ошибок
    private String error(Response res, int status, String message) {
        res.status(status);
        res.type("application/json");
        return "{\"success\": false, \"error\": \"" + message + "\"}";
    }
}
