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


public class Application{
    public static void main(String[] args){
        System.out.println("Запуск сервера");

        Spark.port(8080);
        Spark.staticFiles.location("/public");

        /**
         * контроллер для регистрации пользователя
         *
         * содержит методы register, который создаёт нового пользователя
         */
        AuthController authController = new AuthController();

        // Middleware
        AuthMiddleware authMiddleware = new AuthMiddleware();


        // ===== MIDDLEWARE =====
        // Проверяем авторизацию перед каждым запросом
        Spark.before(authMiddleware::checkAuthAndRedirect);

        // ====== Маршруты ======

        //домашняя страница
        get("/", (req,res) -> "Cloud Storage работает!");

        //  Аутентификация
        post("/auth/register", authController::register);
        post("/auth/login", authController::login);
        post("/auth/logout", authController::logout);


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












//
//
///**
// * Главный класс для запуска приложения
// */
//public class Application {
//    public static void main(String[] args){
//        System.out.println("Starting application");
//
//        try{
//            /**
//             * инициализация конфигурации приложения
//             */
//            Spark.port(8080);
//            Spark.staticFiles.location("/public");
//
//
//            /**
//             * Хэширование паролей
//             */
//            PasswordEncoder passwordEncoder = new SimplePasswordEncoder();
//
//            /**
//             * Хранение сессий в памяти
//             */
//            SessionManager sessionManager = new SimpleSessionManager();
//
//            /**
//             * подключение к S3/MinIO
//             */
//            S3FileStorage s3 = new S3FileStorage(
//                    "http://localhost:9000",  // адрес MinIO
//                    "admin",                  // логин (MINIO_ROOT_USER)
//                    "password123",            // пароль (MINIO_ROOT_PASSWORD)
//                    "userdata"           // название бакета
//            );
//
//            //todo реализовать репезитории, пока заглущки
//            UserRepository userRepo = new StubUserRepository();
//            FileRepository fileRepo = new StubFileRepository();
//
//            /**
//             * Сервисы - это главная логика приложения
//             */
//            AuthService authService = new AuthService(
//                    userRepo,
//                    passwordEncoder,
//                    sessionManager
//            );
//            UserService userService = new UserService(
//                    userRepo,
//                    passwordEncoder
//            );
//            FileService fileService = new FileService(fileRepo,
//                    userRepo,
//                    s3,
//                    "http://localhost:8080"
//            );
//
//
//            /**
//             * Контроллеры (обработчики запросов)
//             */
//            AuthController authController = new AuthController(authService);
//            UserController userController = new UserController(userService);
//            FileController fileController = new FileController(
//                    authService,
//                    fileService,
//                    100*1024*1024
//            );
//
//
//            /**
//             * настройка маршрутов
//             * что на какой запрос отвечает
//             */
//            setupRoutes(authController,
//                    userController,
//                    fileController
//            );
//
//            Spark.init();
//
//            System.out.println("Сервер запущен на порту 8080 \n http://localhost:8080");
//            System.out.println("Доступные пути:");
//            System.out.println("  POST /register    - Регистрация");
//            System.out.println("  POST /login       - Вход");
//            System.out.println("  GET  /files       - Мои файлы");
//            System.out.println("  POST /files       - Загрузить файл");
//        } catch (Exception e){
//            System.err.println("ВСЁ ПОШЛО ПО ПИЗДЕ");
//            System.err.println(e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private static void setupRoutes(AuthController auth,
//                                    UserController user,
//                                    FileController file) {
//
//        System.out.println("пошла возня с путями (/, /user, /file)");
//
//
//        // ====== Сначала публичные ======
//        /**
//         * главная страница
//         * todo подключить фронт
//         */
//        Spark.get("/", (req,res)->{
//            res.type("text/html");
//
//            return "<h1>☁️ Cloud Storage</h1>" +
//                    "<p>Сервер работает! Используйте API:</p>" +
//                    "<ul>" +
//                    "<li>POST /register - регистрация</li>" +
//                    "<li>POST /login - вход</li>" +
//                    "<li>GET /files - список файлов (требует входа)</li>" +
//                    "</ul>";
//        });
//
//
//        // Регистрация
//        Spark.post("/register", auth::register);
//
//        // Вход
//        Spark.post("/login", auth::login);
//
//        // Выход
//        Spark.post("/logout", auth::logout);
//
//
//
//        // ====== То что требует входа для доступа ======
//
//
//
//        //пути пользователя
//        Spark.get("/user/:id", user::getUserById);
//        Spark.put("/user/:id", user::updateUser);
//        Spark.delete("/user/:id", user::deleteUser);
//
//        //пути для работы с файлами
//        Spark.get("/files", file::listFiles);
//        Spark.post("/files", file::uploadFile);
//        Spark.get("/files/:id", file::downloadFile);
//        Spark.delete("/files/:id", file::deleteFile);
//
//
//
//
//
//        // ====== для фронта, todo: надо разобраться ======
//
//
//
//        Spark.before((req, res) -> {
//            res.header("Access-Control-Allow-Origin", "*");
//            res.header("Access-Control-Allow-Methods", "*");
//            res.header("Access-Control-Allow-Headers", "*");
//        });
//
//
//
//
//        System.out.println("мы живы, пути настроены");
//
//    }
//}
