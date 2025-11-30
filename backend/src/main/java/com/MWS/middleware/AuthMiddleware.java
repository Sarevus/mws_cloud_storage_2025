package com.MWS.middleware;

import com.MWS.service.AuthService;
import spark.Request;
import spark.Response;

import java.util.UUID;

import static spark.Spark.halt;

public class AuthMiddleware {
    private final AuthService authService;

    public AuthMiddleware(AuthService authService) {
        this.authService = authService;
    }

    public void requireAuth(Request request, Response response) {
        // Пропускаем публичные маршруты
        if (request.pathInfo().equals("/") ||
                request.pathInfo().startsWith("/register") ||
                request.pathInfo().startsWith("/login") ||
                request.pathInfo().startsWith("/public") ||
                request.pathInfo().endsWith(".html") ||
                request.pathInfo().endsWith(".css") ||
                request.pathInfo().endsWith(".js")) {
            return;
        }

        String userIdStr = request.cookie("UserId");
        if (userIdStr == null) {
            response.status(401);
            if (isAjaxRequest(request)) {
                halt(401, "Требуется авторизация");
            } else {
                response.redirect("/loginIndex.html");
            }
            return;
        }

        try {
            // ПРОБЛЕМА: userIdStr = "1" (число), но мы пытаемся сделать UUID.fromString("1")
            // Используем Long вместо UUID для проверки
            long userId = Long.parseLong(userIdStr);

            // Временно отключаем проверку в БД чтобы протестировать
            System.out.println("User authorized with ID: " + userId);

            // Если нужно проверить в БД, используй:
            // UserEntity user = authService.getUserByLongId(userId);
            // if (user == null) { ... }

        } catch (NumberFormatException e) {
            response.status(401);
            if (isAjaxRequest(request)) {
                halt(401, "Некорректный ID пользователя");
            } else {
                response.redirect("/loginIndex.html");
            }
        }
    }

    private boolean isAjaxRequest(Request request) {
        return "XMLHttpRequest".equals(request.headers("X-Requested-With")) ||
                "application/json".equals(request.contentType());
    }
}