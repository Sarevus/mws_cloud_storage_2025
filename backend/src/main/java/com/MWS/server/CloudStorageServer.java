package com.MWS.server;
import com.MWS.handlers.Files;
import com.MWS.handlers.Home;
import com.MWS.handlers.UserController;
import com.MWS.repository.UserRepository;
import com.MWS.repository.UserRepositoryJDBC;
import com.MWS.service.UserService;
import com.MWS.service.UserServiceRelease;


import static spark.Spark.*;

public class CloudStorageServer {
    public static void main(String[] args) {
        UserRepository userRepository = new UserRepositoryJDBC();
        UserService userService = new UserServiceRelease(userRepository);
        UserController userController = new UserController(userService);
        staticFiles.location("/public");

        /**
         * Запускаем сервер на порту 80
         */
        port(80);


        get("/register", (req, res) -> {
            res.type("text/html");
            res.redirect("/registerIndex.html");
            return null;
        });

        /**
         * на запрос / возвращаем домашнюю страницу
         */
        get("/", (request, response) -> Home.check(request, response));

        /**
         * на запрос /register/ открывается форма для регистрации пользователя.
         */
        post("/register", (req, res) -> userController.register(req, res));
        post("/register/", (req, res) -> userController.register(req, res));

        /**
         * Получить данные о пользователе по id
         */
        get("/user/:id", (req, res) -> userController.getUserById(req, res));
        get("/user/:id/", (req, res) -> userController.getUserById(req, res));


        /**
         * Удалить пользователя по id
         */
        delete("/user/:id", (req, res) -> userController.deleteUser(req, res));
        delete("/user/:id/", (req, res) -> userController.deleteUser(req, res));

        /**
         * Обновить данные пользователя по id
         */
        put("/user/:id", (req, res) -> userController.updateUser(req, res));
        put("/user/:id/", (req, res) -> userController.updateUser(req, res));

        /**
         * на запрос /register/ открывается форма для регистрации пользователя.
         */
//        post("/user/register/", (request, response) -> UserController.UserRegister(request, response));

        /**
         * на запрос /login/ открывается форма для входа пользователя.
         */
//        get("/login/", (request, response) -> UserController.login(request, response));

        /**
         * на запрос /files/user/ возвращаем список файлов.
         */
        get("/files/user/", (request, response) -> Files.getList(request, response));

        /**
         * на запрос /files/download/:id скачиваем файл по id.
         */
        get("/files/download/:id", (request, response) -> Files.downloadFile(request, response));

        /**
         * на запрос /files/upload/ загружаем передаваемый файл.
         */
        post("/files/upload/", (request, response) -> Files.uploadFile(request, response));

        /**
         * на запрос /files/delete/:id удаляем файл по id.
         */
        delete("/files/delete/:id", (request, response) -> Files.deleteFile(request, response));
    }
}