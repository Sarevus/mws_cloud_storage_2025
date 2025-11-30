package com.MWS.handlers;

import com.MWS.service.FileService;
import com.MWS.service.FileServiceRelease;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class Files {
    private static final FileService fileService = new FileServiceRelease();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * GET /files/user/?userId=123
     * Возвращает JSON-массив ключей файлов Ceph для указанного пользователя.
     *
     * Пример ответа:
     * ["user_123/1700123456789_hui.txt", "user_123/1700123460000_resume.pdf"]
     */
    public static Object getList(spark.Request request, spark.Response response) throws JsonProcessingException {
        try {
            // Добавь эту проверку
            response.type("application/json; charset=utf-8");

            String userIdStr = request.cookie("UserId");
            System.out.println("DEBUG: UserId from cookie: " + userIdStr);

            long userId = Long.parseLong(userIdStr);
            List<String> files = fileService.getFileLinksByUserId(userId);

            String result = objectMapper.writeValueAsString(files);
            System.out.println("DEBUG: Returning JSON: " + result);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            response.status(500);
            response.type("application/json; charset=utf-8");
            return "{\"error\": \"Server error: " + e.getMessage() + "\"}";
        }
    }

    /**
     * GET /files/download/:id
     *
     * :id – это base64url от objectKey (например, "user_123/hui.txt").
     * Пример:
     *   objectKey = "user_123/hui.txt"
     *   id       = "dXNlcl8xMjMvaHVpLnR4dA"
     */
    public static Object downloadFile(spark.Request request, spark.Response response) {
        String userIdStr = request.cookie("UserId");
        if (userIdStr == null) {
            response.status(401);
            return "Требуется авторизация";
        }

        String encodedId = request.params("id");
        if (encodedId == null) {
            response.status(400);
            return "id в URL обязателен";
        }

        String objectKey = decodeFileId(encodedId);

        try {
            long userId = Long.parseLong(userIdStr);
            if (!objectKey.startsWith("user_" + userId + "/")) {
                response.status(403);
                return "Доступ запрещен";
            }
        } catch (NumberFormatException e) {
            response.status(400);
            return "Некорректный ID пользователя";
        }

        try (InputStream is = fileService.downloadFile(objectKey)) {
            if (is == null) {
                response.status(404);
                return "файл не найден";
            }

            byte[] data = toByteArray(is);

            // Вытащим имя файла из objectKey
            String filename = objectKey;
            int slashIdx = objectKey.lastIndexOf('/');
            if (slashIdx >= 0 && slashIdx + 1 < objectKey.length()) {
                filename = objectKey.substring(slashIdx + 1);
            }

            response.type("application/octet-stream");
            response.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            response.status(500);
            return "ошибка при скачивании файла: " + e.getMessage();
        }
    }

    /**
     * POST /files/upload/?userId=123
     *
     * Тело запроса – multipart/form-data с полем file.
     */
    public static Object uploadFile(spark.Request request, spark.Response response) {
        // Берем userId из cookies
        String userIdStr = request.cookie("UserId");

        if (userIdStr == null) {
            response.status(401);
            return "Требуется авторизация";
        }

        try {
            long userId = Long.parseLong(userIdStr);

            // Остальной код загрузки файла без изменений...
            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/tmp"));

            Part filePart = request.raw().getPart("file");
            if (filePart == null) {
                response.status(400);
                return "поле file не найдено";
            }

            String originalFilename = filePart.getSubmittedFileName();
            long fileSize = filePart.getSize();

            try (InputStream input = filePart.getInputStream()) {
                String objectKey = fileService.saveUserFile(userId, originalFilename, input, fileSize);

                response.status(201);
                response.type("application/json");
                String downloadId = encodeFileId(objectKey);
                String json = String.format("{\"objectKey\":\"%s\",\"downloadId\":\"%s\"}", objectKey, downloadId);
                return json;
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.status(500);
            return "ошибка при загрузке файла: " + e.getMessage();
        }
    }

    /**
     * DELETE /files/delete/:id
     *
     * :id – тот же base64url от objectKey.
     */
    public static Object deleteFile(spark.Request request, spark.Response response) {
        String userIdStr = request.cookie("UserId");
        if (userIdStr == null) {
            response.status(401);
            return "Требуется авторизация";
        }

        String encodedId = request.params("id");
        if (encodedId == null) {
            response.status(400);
            return "id в URL обязателен";
        }

        String objectKey = decodeFileId(encodedId);

        try {
            long userId = Long.parseLong(userIdStr);
            if (!objectKey.startsWith("user_" + userId + "/")) {
                response.status(403);
                return "Доступ запрещен";
            }
        } catch (NumberFormatException e) {
            response.status(400);
            return "Некорректный ID пользователя";
        }

        try {
            // userId сейчас не используем, реализация FileServiceRelease его игнорирует
            fileService.deleteUserFile(null, objectKey);

            response.status(204); // No Content
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            response.status(500);
            return "ошибка при удалении файла: " + e.getMessage();
        }
    }

    // ===== Вспомогательные методы =====

    // base64url encode
    private static String encodeFileId(String objectKey) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(objectKey.getBytes(StandardCharsets.UTF_8));
    }

    // base64url decode, если не получилось — считаем, что нам передали уже готовый objectKey
    private static String decodeFileId(String id) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(id);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            // id не в base64, используем как есть
            return id;
        }
    }

    private static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
