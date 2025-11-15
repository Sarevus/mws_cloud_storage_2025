package com.MWS.server;
import com.MWS.handlers.Files;
import com.MWS.handlers.Home;
import com.MWS.handlers.UserController;

import static spark.Spark.*;

public class CloudStorageServer {
    public static void main(String[] args) {
        /**
         * Запускаем сервер на порту 80
         */
        port(80);

        /**
         * на запрос / возвращаем домашнюю страницу
         */
        get("/", (request, response) -> Home.check(request, response));

        /**
         * на запрос /register/ регистрация пользователя.
         */
        get("/register/", (request, response) -> UserController.register(request, response));

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