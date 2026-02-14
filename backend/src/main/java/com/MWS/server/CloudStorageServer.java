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
import com.MWS.storage.Database;
import com.MWS.storage.S3FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import static spark.Spark.*;

public class CloudStorageServer {

    private static final Logger logger = LoggerFactory.getLogger(CloudStorageServer.class);

    public static void main(String[] args) {
        try {
            // Выводим конфигурацию при запуске
            logger.info("Запуск Cloud Storage Server...");
            Config.printConfiguration();

            // Проверяем подключение к БД
            if (!Database.testConnection()) {
                logger.error("Не удалось подключиться к базе данных!");
                System.exit(1);
            }

            // Инициализация репозиториев
            UserRepository userRepository = new UserRepositoryJDBC();
            FileRepository fileRepository = new FileRepositoryJDBC();

            // Инициализация S3/Ceph хранилища
            S3FileStorage s3Storage = new S3FileStorage();
            if (!s3Storage.testConnection()) {
                logger.error("Не удалось подключиться к S3/Ceph!");
                System.exit(1);
            }

            // Инициализация сервисов
            UserService userService = new UserServiceRelease(userRepository);
            FileService fileService = new FileService(fileRepository, userRepository, s3Storage);

            // Инициализация контроллеров
            UserController userController = new UserController(userService);
            FileController fileController = new FileController(
                    fileService,
                    Config.getMaxFileSize()  // Используем конфигурацию из Config
            );

            // Настройка порта сервера
            int serverPort = Config.getServerPort();
            port(serverPort);
            logger.info("Сервер запускается на порту {}", serverPort);

            // Настройка статических файлов
            staticFiles.location("/public");

            // Включаем CORS для API запросов
            enableCORS();

            // ==================== USER ROUTES ====================

            // Страницы регистрации и логина
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

            // API для аутентификации
            post("/login", (req, res) -> userController.login(req, res));
            post("/register", (req, res) -> userController.register(req, res));
            post("/register/", (req, res) -> userController.register(req, res));

            // Домашняя страница
            get("/", (request, response) -> Home.check(request, response));

            // Страницы профиля
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

            // API для работы с пользователями
            get("/api/user/:id", (req, res) -> userController.getUserById(req, res));
            get("/api/user/:id/", (req, res) -> userController.getUserById(req, res));
            delete("/user/:id", (req, res) -> userController.deleteUser(req, res));
            delete("/user/:id/", (req, res) -> userController.deleteUser(req, res));
            put("/user/:id", (req, res) -> userController.updateUser(req, res));
            put("/user/:id/", (req, res) -> userController.updateUser(req, res));

            // ==================== FILE ROUTES ====================

            // тестовый маршрут для проверки
            get("/api/test", (req, res) -> {
                res.type("application/json");
                return "{\"status\": \"API работает\", \"timestamp\": \"" + new java.util.Date() + "\", \"port\": " + serverPort + "}";
            });

            // Страница файлов
            get("/files", (req, res) -> {
                res.type("text/html");
                res.redirect("/files.html");
                return null;
            });

            // ==================== FILE ROUTES ====================

            // Основные маршруты (поддерживают параметр category в query)
            get("/api/files", fileController::listFiles);
            post("/api/files/upload", fileController::uploadFile);

            // Новые маршруты для категорий
            get("/api/files/categories", fileController::getUserCategories); // Список категорий

            // Остальные маршруты (оставить как есть)
            get("/api/files/:id", fileController::getFileMetadata);
            get("/api/files/:id/download", fileController::downloadFile);
            delete("/api/files/:id", fileController::deleteFile);
            put("/api/files/:id", fileController::updateFileMetadata);

            // API для работы с файлами
            get("/api/files", fileController::listFiles);                      // Получить список файлов
            post("/api/files/upload", fileController::uploadFile);             // Загрузить файл
            get("/api/files/:id", fileController::getFileMetadata);            // Получить метаданные файла
            get("/api/files/:id/download", fileController::downloadFile);      // Скачать файл
            delete("/api/files/:id", fileController::deleteFile);              // Удалить файл
            put("/api/files/:id", fileController::updateFileMetadata);         // Обновить метаданные
            delete("/api/files", fileController::deleteAllFiles);


            // ==================== ERROR HANDLING ====================

            // Обработка 404
            notFound((req, res) -> {
                res.type("application/json");
                return "{\"error\": \"Страница не найдена\", \"path\": \"" + req.pathInfo() + "\"}";
            });

            // Обработка внутренних ошибок
            internalServerError((req, res) -> {
                res.type("application/json");
                return "{\"error\": \"Внутренняя ошибка сервера\"}";
            });

            // Глобальный обработчик исключений
            exception(Exception.class, (e, req, res) -> {
                logger.error("Необработанное исключение: ", e);
                res.status(500);
                res.type("application/json");
                res.body("{\"error\": \"" + e.getMessage() + "\"}");
            });

            // ==================== GRACEFUL SHUTDOWN ====================

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Остановка сервера...");
                Spark.stop();
                logger.info("Сервер остановлен");
            }));

            // Ждем инициализации Spark
            awaitInitialization();
            logger.info("✅ Cloud Storage Server успешно запущен на http://localhost:{}", serverPort);
            logger.info("Swagger UI (если настроен): http://localhost:{}/swagger-ui", serverPort);

        } catch (Exception e) {
            logger.error("Критическая ошибка при запуске сервера", e);
            System.exit(1);
        }
    }

    /**
     * Включает CORS для всех API запросов
     */
    private static void enableCORS() {
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
            response.header("Access-Control-Allow-Credentials", "true");
        });
    }
}