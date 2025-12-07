package com.cloudstorage;
import com.cloudstorage.config.Config;
import com.cloudstorage.controller.*;
import com.cloudstorage.repository.FileRepository;
import com.cloudstorage.repository.FileRepositoryJDBC;
import com.cloudstorage.repository.UserRepository;
import com.cloudstorage.repository.UserRepositoryJDBC;
import com.cloudstorage.security.*;
import com.cloudstorage.storage.*;
import com.cloudstorage.service.*;
import spark.Spark;

import static spark.Spark.*;


/**
 * Главный класс приложения. Собирает все компоненты вместе.
 */
public class Application{
    public static void main(String[] args){
        System.out.println("Запуск сервера");

        Spark.port(Config.getServerPort());
        Spark.staticFiles.location("/public");

        // 1. Создаём основные утилиты
        PasswordEncoder passwordEncoder = new PasswordEncoder();

        // 2. Создаём репозитории
        UserRepository userRepository = new UserRepositoryJDBC();
        FileRepository fileRepository = new FileRepositoryJDBC();

        // 3. Создаём S3 клиент (готовый компонент)
        S3FileStorage s3Storage = new S3FileStorage(
                Config.getCephEndpoint(),
                Config.getCephAccessKey(),
                Config.getCephSecretKey(),
                Config.getCephBucketName()
        );

        // 4. Создаём сервисы
        AuthService authService = new AuthService(userRepository, passwordEncoder);
        UserService userService = new UserService(userRepository, passwordEncoder);
        FileService fileService = new FileService(fileRepository, userRepository, s3Storage);


        /**
         * контроллер для регистрации пользователя
         *
         * содержит методы register, который создаёт нового пользователя
         */
        AuthController authController = new AuthController(authService);


        /**
         * контроллер для управления файлами
         */
        FileController fileController = new FileController(
                fileService,
                Config.getMaxFileSize()
        );


        /**
         * контроллер для управления юзерами
         */
        UserController userController = new UserController(userService);


        // Middleware
        AuthMiddleware authMiddleware = new AuthMiddleware(authService);


        System.out.println("Все компоненты созданы");


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
        get("/auth/me", authController::getCurrentUser);

        // Пользователь (требуют авторизации)
        get("/user", userController::getCurrentUser);
        put("/user", userController::updateUser);
        put("/user/password", userController::updatePassword);
        delete("/user", userController::deleteUser);


        // Файлы (требуют авторизации)
        Spark.get("/files", fileController::listFiles);
        Spark.post("/files/upload", fileController::uploadFile);
        Spark.get("/files/:id/download", fileController::downloadFile);
        Spark.delete("/files/:id", fileController::deleteFile);






        get("/", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });

        get("/login", (req, res) -> {
            res.redirect("/login.html");
            return null;
        });

        get("/register", (req, res) -> {
            res.redirect("/register.html");
            return null;
        });

        get("/profile", (req, res) -> {
            res.redirect("/profile.html");
            return null;
        });

        get("/edit-profile", (req, res) -> {
            res.redirect("/edit-profile.html");
            return null;
        });

        get("/file-exchange", (req, res) -> {
            res.redirect("/file-exchange.html");
            return null;
        });







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
        System.out.println("\n==================================");
        System.out.println("✅ Сервер запущен: http://localhost:" + Config.getServerPort());
        System.out.println("📁 Ceph S3: " + Config.getCephEndpoint());
        System.out.println("🐘 PostgreSQL: " + Config.getDatabaseUrl());
        System.out.println("==================================");
        System.out.println("\nДоступные endpoints:");
        System.out.println("   GET  /               - Домашняя страница");
        System.out.println("   POST /auth/register  - Регистрация");
        System.out.println("   POST /auth/login     - Вход");
        System.out.println("   POST /auth/logout    - Выход");
        System.out.println("   GET  /auth/me        - Текущий пользователь");
        System.out.println("   GET  /user           - Инфо о пользователе");
        System.out.println("   PUT  /user           - Обновить данные");
        System.out.println("   GET  /files          - Список файлов");
        System.out.println("   POST /files/upload   - Загрузить файл");
        System.out.println("\n==================================\n");
    }
}