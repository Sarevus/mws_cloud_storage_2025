package com.MWS.server;

import com.MWS.handlers.Files;
import com.MWS.handlers.Home;
import com.MWS.handlers.UserController;
import com.MWS.middleware.AuthMiddleware;
import com.MWS.repository.UserRepository;
import com.MWS.repository.UserRepositoryJDBC;
import com.MWS.service.AuthService;
import com.MWS.service.UserService;
import com.MWS.service.UserServiceRelease;

import static spark.Spark.*;

public class CloudStorageServer {
    public static void main(String[] args) {
        UserRepository userRepository = new UserRepositoryJDBC();
        UserService userService = new UserServiceRelease(userRepository);
        AuthService authService = new AuthService(userRepository);
        UserController userController = new UserController(userService, authService); // Обновленный конструктор
        AuthMiddleware authMiddleware = new AuthMiddleware(authService);

        staticFiles.location("/public");

        port(80);

        // Middleware проверки аутентификации
        before(authMiddleware::requireAuth);

        // Публичные маршруты
        get("/", (request, response) -> Home.check(request, response));

        get("/register", (req, res) -> {
            res.type("text/html");
            res.redirect("/registerIndex.html");
            return null;
        });

        // API маршруты
        post("/register", (req, res) -> userController.register(req, res));
        post("/login", (req, res) -> userController.login(req, res)); // Добавляем вход

        get("/user/:id", (req, res) -> userController.getUserById(req, res));
        delete("/user/:id", (req, res) -> userController.deleteUser(req, res));
        put("/user/:id", (req, res) -> userController.updateUser(req, res));

        // Файлы
        get("/files/user/", (request, response) -> Files.getList(request, response));
        get("/files/download/:id", (request, response) -> Files.downloadFile(request, response));
        post("/files/upload/", (request, response) -> Files.uploadFile(request, response));
        delete("/files/delete/:id", (request, response) -> Files.deleteFile(request, response));
    }
}