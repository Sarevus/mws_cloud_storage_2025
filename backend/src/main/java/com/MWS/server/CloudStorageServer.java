package com.MWS.server;

import com.MWS.config.Config;
import com.MWS.handlers.FileController;
import com.MWS.handlers.Home;
import com.MWS.handlers.UserController;
import com.MWS.repository.FileRepository;
import com.MWS.repository.FileRepositoryJDBC;
import com.MWS.repository.UserRepository;
import com.MWS.repository.UserRepositoryJDBC;
import com.MWS.service.FileService;
import com.MWS.service.UserService;
import com.MWS.service.UserServiceRelease;
import com.MWS.storage.S3FileStorage;
import spark.Spark;


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


        get("/login", (req, res) -> {
            res.type("text/html");
            res.redirect("/loginIndex.html");
            return null;
        });

        post("/login", (req, res) -> userController.login(req, res));

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
        get("/user/:id", (req, res) -> {
            res.type("text/html");
            res.redirect("/myProfile.html?id=" + req.params(":id"));
            return null;
        });

        get("/user/:id/edit", (req, res) -> {
            res.type("text/html");
            res.redirect("/editProfile.html?id=" + req.params(":id"));
            return null;
        });


        get("/api/user/:id", (req, res) -> userController.getUserById(req, res));
        get("/api/user/:id/", (req, res) -> userController.getUserById(req, res));


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





        FileRepository fileRepository = new FileRepositoryJDBC();
        S3FileStorage s3Storage = new S3FileStorage(
                Config.getCephEndpoint(),
                Config.getCephAccessKey(),
                Config.getCephSecretKey(),
                Config.getCephBucketName()
        );


        FileService fileService = new FileService(fileRepository, userRepository, s3Storage);
        FileController fileController = new FileController(
                fileService,
                100 * 1024 * 1024
        );

        Spark.get("/files", fileController::listFiles);
        Spark.post("/files/upload", fileController::uploadFile);
        Spark.get("/files/:id/download", fileController::downloadFile);
        Spark.delete("/files/:id", fileController::deleteFile);
    }
}