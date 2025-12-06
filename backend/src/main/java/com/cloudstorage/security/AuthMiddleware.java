package com.cloudstorage.security;

import spark.Request;
import spark.Response;

import java.util.UUID;

import static spark.Spark.halt;

/**
 * Middleware для проверки авторизации.
 * Если пользователь не авторизован - редирект на /login.html
 */
public class AuthMiddleware {

    /**
     * Проверяет авторизацию и делает редирект если нужно.
     */
    public void checkAuthAndRedirect(Request req, Response res) {
        String path = req.pathInfo();

        // Публичные маршруты (не требуют входа)
        if (isPublicRoute(path)) {
            return;
        }

        // Проверяем авторизацию
        if (!isAuthenticated(req)) {
            // AJAX запросы (например, от фронтенда) получают JSON ошибку
            if (isAjaxRequest(req)) {
                halt(401, "{\"error\": \"Требуется авторизация\"}");
            }

            // Обычные запросы - редирект на страницу входа
            res.redirect("/login.html");
            halt(); // Останавливаем дальнейшую обработку
        }
    }

    /**
     * Проверяет, авторизован ли пользователь.
     */
    private boolean isAuthenticated(Request req) {
        String userId = req.cookie("user_id");

        if (userId == null || userId.isEmpty()) {
            return false;
        }

        try {
            // Проверяем, что это валидный UUID
            UUID.fromString(userId);
            //todo проверка в базе данных userId
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Проверяет, публичный ли маршрут.
     */
    private boolean isPublicRoute(String path) {
        // Точное совпадение с публичными маршрутами
        String[] exactMatches = {
                "/",
                "/auth/register",
                "/auth/login",
                "/auth/logout",
                "/login.html",
                "/register.html",
                "/index.html"
        };

        for (String route : exactMatches) {
            if (path.equals(route)) {
                return true;
            }
        }

        // Статические файлы всегда публичные
        if (path.startsWith("/public/") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.endsWith(".png") ||
                path.endsWith(".jpg") ||
                path.endsWith(".ico") ||
                path.endsWith(".svg")) {
            return true;
        }

        // Маршруты начинающиеся с /auth/ тоже публичные
        if (path.startsWith("/auth/")) {
            return true;
        }

        return false;
    }

    /**
     * Проверяет, AJAX ли это запрос.
     */
    private boolean isAjaxRequest(Request req) {
        return "XMLHttpRequest".equals(req.headers("X-Requested-With")) ||
                "application/json".equals(req.contentType());
    }
}