package com.MWS.server;
import com.MWS.handlers.Files;
import com.MWS.handlers.Home;
import com.MWS.handlers.UserController;
import com.MWS.repository.UserRepository;
import com.MWS.repository.UserRepositoryPostgre;
import com.MWS.service.UserService;
import com.MWS.service.UserServiceRelease;

import static spark.Spark.*;

public class CloudStorageServer {
    public static void main(String[] args) {
        UserRepository userRepository = new UserRepositoryPostgre();
        UserService userService = new UserServiceRelease(userRepository);
        UserController userController = new UserController(userService);
        /**
         * Запускаем сервер на порту 80
         */
        port(80);

        /**
         * на запрос / возвращаем домашнюю страницу
         */
        get("/", (request, response) -> Home.check(request, response));

        /**
         * на запрос /register/ открывается форма для регистрации пользователя.
         */
        post("/register", userController::register);
        post("/register/", userController::register);

        /**
         * Получить данные о пользователе по id
         */
        get("/user/:id", userController::getUserById);
        get("/user/:id/", userController::getUserById);

        /**
         * Удалить пользователя по id
         */
        delete("/user/:id", userController::deleteUser);
        delete("/user/:id/", userController::deleteUser);

        /**
         * на запрос /register/ открывается форма для регистрации пользователя.
         */
        post("/user/register/", (request, response) -> UserController.UserRegister(request, response));

        /**
         * на запрос /login/ открывается форма для входа пользователя.
         */
        get("/login/", (request, response) -> UserController.login(request, response));

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