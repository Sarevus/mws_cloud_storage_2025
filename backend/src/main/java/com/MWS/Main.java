package com.MWS;

import com.MWS.handlers.UserController;
import com.MWS.repository.UserRepositoryJDBC;
import com.MWS.service.UserServiceRelease;
import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {
        port(4567);

        UserRepositoryJDBC userRepository = new UserRepositoryJDBC();
        UserServiceRelease userService = new UserServiceRelease(userRepository);
        UserController userController = new UserController(userService);

        post("/api/users/register", userController::register);
        get("/api/users/:id", userController::getUserById);
        put("/api/users/:id", userController::updateUser);
        delete("/api/users/:id", userController::deleteUser);

        get("/health", (req, res) -> {
            res.type("application/json");
            return "{\"status\":\"ok\"}";
        });

        after((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
        });

        options("/*", (req, res) -> {
            return "OK";
        });

        exception(IllegalArgumentException.class, (e, req, res) -> {
            res.status(400);
            res.type("application/json");
            res.body("{\"error\": \"" + e.getMessage() + "\"}");
        });

        exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.type("application/json");
            res.body("{\"error\": \"Internal server error\"}");
        });

        System.out.println("Сервер запущен");
    }
}