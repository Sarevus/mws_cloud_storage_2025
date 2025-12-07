package com.cloudstorage;
import com.cloudstorage.controller.*;
import com.cloudstorage.repository.FileRepository;
import com.cloudstorage.repository.UserRepository;
import com.cloudstorage.security.*;
import com.cloudstorage.storage.*;
import com.cloudstorage.service.*;
import spark.Spark;

import static spark.Spark.post;
import static spark.Spark.get;


/**
 * Главный класс приложения. Собирает все компоненты вместе.
 */
public class Application{
    public static void main(String[] args){
        System.out.println("Запуск сервера");

        Spark.port(8080);
        Spark.staticFiles.location("/public");

        // 1. Создаём основные утилиты
        PasswordEncoder passwordEncoder = new PasswordEncoder();

        // 2. Создаём репозитории todo ждём реализацию
        UserRepository userRepository = createUserRepository();
        FileRepository fileRepository = createFileRepository();

        // 3. Создаём S3 клиент (готовый компонент)
        S3FileStorage s3Storage = new S3FileStorage(
                "http://localhost:9000",  // MinIO endpoint
                "admin",                   // accessKey
                "password123",             // secretKey
                "cloud-storage"            // bucketName
        );

        // 4. Создаём сервисы
        AuthService authService = new AuthService(userRepository, passwordEncoder);
        FileService fileService = new FileService(fileRepository, userRepository, s3Storage);

        /**
         * контроллер для регистрации пользователя
         *
         * содержит методы register, который создаёт нового пользователя
         */
        AuthController authController = new AuthController();


        /**
         * контроллер для управления файлами
         */
        FileController fileController = new FileController(fileService, 100 * 1024 * 1024); // 100MB макс

        // Middleware
        AuthMiddleware authMiddleware = new AuthMiddleware();


        System.out.println("✅ Все компоненты созданы");


        // ===== MIDDLEWARE =====
        // Проверяем авторизацию перед каждым запросом
        Spark.before(authMiddleware::checkAllRoutes);

        // ====== Маршруты ======

        //домашняя страница
        get("/", (req,res) -> "Cloud Storage работает!");

        //  Аутентификация
        post("/auth/register", authController::register);
        post("/auth/login", authController::login);
        post("/auth/logout", authController::logout);

        // Пользователь (требуют авторизации)
        Spark.get("/user/:id", userController::getUserById);     // ← БУДЕТ ПОЗЖЕ
        Spark.put("/user/:id", userController::updateUser);      // ← БУДЕТ ПОЗЖЕ
        Spark.delete("/user/:id", userController::deleteUser);   // ← БУДЕТ ПОЗЖЕ


        // Файлы (требуют авторизации)
        Spark.get("/files", fileController::listFiles);
        Spark.post("/files/upload", fileController::uploadFile);
        Spark.get("/files/:id/download", fileController::downloadFile);
        Spark.delete("/files/:id", fileController::deleteFile);


        // ====== CORS ======
        //хз зачем пока
        Spark.before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "*");
            res.header("Access-Control-Allow-Headers", "*");
            res.header("Access-Control-Allow-Credentials", "true");
        });

        // OPTIONS для CORS (хз)
        Spark.options("/*", (req, res) -> {
            res.status(200);
            return "OK";
        });

        // ====== ИНФОРМАЦИЯ ======
        System.out.println("✅ Сервер запущен: http://localhost:8080");
        System.out.println("   GET  /             - Домашняя страница");
        System.out.println("   POST /register     - Регистрация (JSON: name, email, password)");
    }
}