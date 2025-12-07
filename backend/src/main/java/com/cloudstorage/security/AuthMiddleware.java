package com.cloudstorage.security;

import com.cloudstorage.service.AuthService;
import spark.Request;
import spark.Response;

import java.util.UUID;

import static spark.Spark.halt;

/**
 * Middleware для проверки авторизации.
 * Если пользователь не авторизован - редирект на /login.html
 */
public class AuthMiddleware {

    private final AuthService authService;

    public AuthMiddleware(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Проверяет, публичный ли маршрут.
     */
    private boolean isPublicRoute(String path) {
        // Точные совпадения с публичными маршрутами
        String[] publicExactPaths = {
                "/",
        };

        for (String publicPath : publicExactPaths) {
            if (path.equals(publicPath)) {
                return true;
            }
        }

        // Маршруты начинающиеся с /auth/ - публичные
        if (path.startsWith("/auth/")) {
            return true;
        }

        // Статические файлы - публичные
        if (path.startsWith("/public/") ||
                path.endsWith(".html") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.endsWith(".png") ||
                path.endsWith(".jpg") ||
                path.endsWith(".ico") ||
                path.endsWith(".svg")) {
            return true;
        }

        // Всё остальное - защищённое
        return false;
    }


    /**
     * Проверяет авторизацию пользователя.
     */
    private void checkAuth(Request req, Response res) {
        // 1. Получаем sessionId из куки
        String sessionId = req.cookie("session_id");

        if (sessionId == null || sessionId.isEmpty()) {
            haltUnauthorized(req, res, "Требуется авторизация");
        }

        // 2. Проверяем валидность сессии через AuthService
        if (!authService.isValidSession(sessionId)) {
            haltUnauthorized(req, res, "Сессия истекла или недействительна");
        }

        // 3. Проверяем, что sessionId - валидный UUID
        try {
            UUID.fromString(sessionId);
        } catch (IllegalArgumentException e) {
            haltUnauthorized(req, res, "Неверный формат сессии");
        }
    }

    /**
     * Прерывает запрос с ошибкой 401.
     */
    private void haltUnauthorized(Request req,Response res, String message) {
        // Определяем тип запроса
        boolean isAjax = "XMLHttpRequest".equals(req.headers("X-Requested-With")) ||
                "application/json".equals(req.contentType());

        if (isAjax) {
            // AJAX/API запросы получают JSON
            halt(401, "{\"error\": \"" + message + "\"}");
        } else {
            // Браузерные запросы - редирект на страницу входа
            res.redirect("/login.html");
            halt();
        }
    }

    /**
     * Проверяет все маршруты. Решает сам, нужно ли требовать авторизацию.
     */
    public void checkAllRoutes(Request req, Response res) {
        String path = req.pathInfo();

        // 1. Проверяем, публичный ли маршрут
        if (isPublicRoute(path)) {
            return; // Пропускаем без проверки
        }

        // 2. Для защищённых маршрутов проверяем авторизацию
        checkAuth(req, res);
    }
}