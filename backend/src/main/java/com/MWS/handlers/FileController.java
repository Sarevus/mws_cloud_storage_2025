package com.MWS.handlers;

import com.MWS.model.File;
import com.MWS.service.FileService;
import com.google.gson.Gson;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Контроллер для работы с файлами через HTTP.
 * получает запрос и возвращает ответ
 *
 */
public class FileController {
    private final Gson gson = new Gson();
    private final FileService fileService;
    private final long maxFileSize;

    public FileController(FileService fileService, long maxFileSize) {
        this.fileService = fileService;
        this.maxFileSize = maxFileSize;
    }

    /**
     * Возвращает список файлов пользователя.
     * GET /files
     */
    public Object listFiles(Request req, Response res) {
        try {
            // 1. Получаем ID пользователя из url
            UUID userId = UUID.fromString(req.params(":id"));

            // Получаем файлы через сервис
            List<File> files = fileService.getUserFiles(userId);

            // Формируем ответ
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("files", files.stream()
                    .map(this::toFileMap)
                    .collect(Collectors.toList()));
            response.put("count", files.size());

            res.type("application/json");
            return gson.toJson(response);

        } catch (Exception e) {
            return error(res, 500, "Ошибка получения списка файлов: " + e.getMessage());
        }
    }

    /**
     * Загружает файл на сервер.
     * Формат запроса: multipart/form-data
     */
    public Object uploadFile(Request req, Response res) {
        try {
            // 1. Получаем ID пользователя из url
            UUID userId = UUID.fromString(req.params(":id"));

            // 2. Настраиваем загрузку файла
            req.attribute("org.eclipse.jetty.multipartConfig",
                    new MultipartConfigElement("/tmp"));

            // 3. Получаем файл из запроса
            Part filePart = req.raw().getPart("file");
            if (filePart == null) {
                return error(res, 400, "Файл не предоставлен");
            }

            // 4. Проверяем размер файла
            if (filePart.getSize() > maxFileSize) {
                return error(res, 413, "Файл слишком большой. Максимум: " +
                        (maxFileSize / (1024 * 1024)) + " MB");
            }

            // 5. Загружаем файл через сервис
            try (InputStream fileStream = filePart.getInputStream()) {
                File file = fileService.uploadFile(
                        userId,
                        filePart.getSubmittedFileName(),
                        fileStream,
                        filePart.getSize(),
                        filePart.getContentType()
                );

                // 7. Возвращаем успешный ответ
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Файл успешно загружен");
                response.put("file", toFileMap(file));

                res.status(201); // 201 Created
                res.type("application/json");
                return gson.toJson(response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return error(res, 500, "Ошибка загрузки файла: " + e.getMessage());
        }
    }

    /**
     * Скачивает файл.
     * GET /files/{id}/download
     */
    public Object downloadFile(Request req, Response res) {
        try {
            // 1. Получаем ID пользователя из url
            UUID userId = UUID.fromString(req.params(":id"));

            // 2. получаем ID файла из url
            UUID fileId = UUID.fromString(req.params(":fileId"));

            // Скачиваем файл через сервис
            InputStream fileStream = fileService.downloadFile(userId, fileId);

            // Получаем информацию о файле для заголовков
            File file = fileService.getUserFiles(userId).stream()
                    .filter(f -> f.getId().equals(fileId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Файл не найден"));

            // Устанавливаем правильные заголовки
            res.type(file.getMimeType() != null ? file.getMimeType() : "application/octet-stream");
            res.header("Content-Disposition",
                    "attachment; filename=\"" + file.getOriginalName() + "\"");
            res.header("Content-Length", String.valueOf(file.getSize()));

            // Возвращаем поток файла
            return fileStream;

        } catch (Exception e) {
            return error(res, 404, "Файл не найден или доступ запрещён");
        }
    }

    /**
     * Удаляет файл.
     * DELETE /files/{id}
     */
    public Object deleteFile(Request req, Response res) {
        try {
            // 1. Получаем ID пользователя из url
            UUID userId = UUID.fromString(req.params(":id"));

            UUID fileId = UUID.fromString(req.params(":fileId"));

            fileService.deleteFile(userId, fileId);

            res.status(204); // 204 No Content
            return "";

        } catch (Exception e) {
            return error(res, 500, "Ошибка удаления файла: " + e.getMessage());
        }
    }

    /**
     * Преобразует File в Map для JSON ответа.
     */
    private Map<String, Object> toFileMap(File file) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", file.getId());
        map.put("originalName", file.getOriginalName());
        map.put("size", file.getSize());
        map.put("formattedSize", file.getFormattedSize());
        map.put("mimeType", file.getMimeType());
//        map.put("isPublic", file.getIsPublic());
//        map.put("description", file.getDescription());
//        map.put("uploadedAt", file.getUploadedAt());
//        map.put("updatedAt", file.getUpdatedAt());
        map.put("downloadUrl", "/files/" + file.getId() + "/download");
        return map;
    }

    /**
     * Возвращает ошибку в JSON формате.
     */
    private String error(Response res, int status, String message) {
        res.status(status);
        res.type("application/json");
        return "{\"success\": false, \"error\": \"" + message + "\"}";
    }
}