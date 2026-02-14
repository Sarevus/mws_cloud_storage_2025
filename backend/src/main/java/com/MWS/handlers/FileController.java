package com.MWS.handlers;

import com.MWS.model.File;
import com.MWS.service.FileService;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Контроллер для работы с файлами через HTTP API
 */
public class FileController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private final FileService fileService;
    private final long maxFileSize;
    private final Gson gson;

    public FileController(FileService fileService, long maxFileSize) {
        this.fileService = fileService;
        this.maxFileSize = maxFileSize;
        this.gson = new Gson();
    }

    /**
     * Получить список всех файлов пользователя
     * GET /api/files?userId={userId}
     */
    public String listFiles(Request req, Response res) {
        try {
            String userIdStr = req.queryParams("userId");
            String category = req.queryParams("category");

            if (userIdStr == null || userIdStr.isEmpty()) {
                res.status(400);
                return errorResponse("Параметр userId обязателен");
            }

            UUID userId = UUID.fromString(userIdStr);
            List<File> files = fileService.getUserFiles(userId);

            res.type("application/json");
            res.status(200);
            return gson.toJson(Map.of(
                    "success", true,
                    "files", files,
                    "count", files.size(),
                    "category", category != null ? category : "general"
            ));

        } catch (IllegalArgumentException e) {
            logger.error("Неверный формат UUID", e);
            res.status(400);
            return errorResponse("Неверный формат userId");
        } catch (Exception e) {
            logger.error("Ошибка при получении списка файлов", e);
            res.status(500);
            return errorResponse("Ошибка сервера: " + e.getMessage());
        }
    }

    /**
     * Загрузить файл
     * POST /api/files/upload
     * Content-Type: multipart/form-data
     */
    public String uploadFile(Request req, Response res) {
        try {
            // НАИБОЛЕЕ ПРОСТОЕ РЕШЕНИЕ: используем пустую строку для in-memory хранения
            // ИЛИ системную временную директорию
            String tempDir = System.getProperty("java.io.tmpdir");
            logger.info("Используем временную директорию: {}", tempDir);

            req.attribute("org.eclipse.jetty.multipartConfig",
                    new MultipartConfigElement(tempDir, maxFileSize, maxFileSize, 1024));

            // Получаем userId из query параметров
            String userIdStr = req.queryParams("userId");
            if (userIdStr == null || userIdStr.isEmpty()) {
                res.status(400);
                return errorResponse("Параметр userId обязателен");
            }
            UUID userId = UUID.fromString(userIdStr);

            // Получаем загруженный файл
            Part filePart = req.raw().getPart("file");
            if (filePart == null) {
                res.status(400);
                return errorResponse("Файл не найден в запросе");
            }

            // Проверяем размер файла
            long fileSize = filePart.getSize();
            if (fileSize > maxFileSize) {
                res.status(413);
                return errorResponse("Файл слишком большой. Максимальный размер: "
                        + (maxFileSize / 1024 / 1024) + " MB");
            }

            // Получаем данные файла
            String originalFilename = filePart.getSubmittedFileName();
            String mimeType = filePart.getContentType();
            InputStream fileStream = filePart.getInputStream();

            String category = req.queryParams("category");
            if (category == null) {
                category = req.queryParamOrDefault("category", "general");
            }

            logger.info("Загрузка файла в категорию: {}", category);

            logger.info("Загрузка файла: {}, размер: {} байт, тип: {}",
                    originalFilename, fileSize, mimeType, category);

            // Загружаем файл через сервис
            File uploadedFile = fileService.uploadFile(
                    userId,
                    originalFilename,
                    fileStream,
                    fileSize,
                    mimeType,
                    category
            );

            // Закрываем поток
            fileStream.close();

            res.type("application/json");
            res.status(201);
            return gson.toJson(Map.of(
                    "success", true,
                    "message", "Файл успешно загружен",
                    "file", uploadedFile
            ));

        } catch (IllegalArgumentException e) {
            logger.error("Неверные параметры", e);
            res.status(400);
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            logger.error("Ошибка при загрузке файла", e);
            res.status(500);
            return errorResponse("Ошибка загрузки: " + e.getMessage());
        }
    }

    /**
     * Получить метаданные файла
     * GET /api/files/:id?userId={userId}
     */
    public String getFileMetadata(Request req, Response res) {
        try {
            UUID fileId = UUID.fromString(req.params(":id"));
            String userIdStr = req.queryParams("userId");

            if (userIdStr == null || userIdStr.isEmpty()) {
                res.status(400);
                return errorResponse("Параметр userId обязателен");
            }
            UUID userId = UUID.fromString(userIdStr);

            File file = fileService.getFileMetadata(userId, fileId);

            res.type("application/json");
            res.status(200);
            return gson.toJson(Map.of(
                    "success", true,
                    "file", file
            ));

        } catch (IllegalArgumentException e) {
            res.status(400);
            return errorResponse("Неверный формат UUID");
        } catch (SecurityException e) {
            res.status(403);
            return errorResponse("Доступ запрещен");
        } catch (RuntimeException e) {
            res.status(404);
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            logger.error("Ошибка при получении метаданных файла", e);
            res.status(500);
            return errorResponse("Ошибка сервера: " + e.getMessage());
        }
    }

    /**
     * Скачать файл
     * GET /api/files/:id/download?userId={userId}
     */
    public Object downloadFile(Request req, Response res) {
        try {
            UUID fileId = UUID.fromString(req.params(":id"));
            String userIdStr = req.queryParams("userId");

            if (userIdStr == null || userIdStr.isEmpty()) {
                res.status(400);
                return errorResponse("Параметр userId обязателен");
            }
            UUID userId = UUID.fromString(userIdStr);

            // Получаем метаданные файла
            File file = fileService.getFileMetadata(userId, fileId);

            // Скачиваем файл из S3/Ceph
            InputStream fileStream = fileService.downloadFile(userId, fileId);

            // Устанавливаем заголовки для скачивания
            res.type(file.getMimeType());
            res.header("Content-Disposition",
                    "attachment; filename=\"" + file.getOriginalName() + "\"");
            res.header("Content-Length", String.valueOf(file.getSize()));

            // Возвращаем поток файла
            try {
                byte[] buffer = fileStream.readAllBytes();
                res.status(200);

                // Записываем данные в response
                javax.servlet.ServletOutputStream output = res.raw().getOutputStream();
                output.write(buffer);
                output.flush();
                output.close();

                logger.info("✅ Файл {} успешно отправлен пользователю {}", fileId, userId);
                return res.raw();

            } finally {
                fileStream.close();
            }

        } catch (IllegalArgumentException e) {
            res.status(400);
            return errorResponse("Неверный формат UUID");
        } catch (SecurityException e) {
            res.status(403);
            return errorResponse("Доступ запрещен");
        } catch (RuntimeException e) {
            res.status(404);
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            logger.error("Ошибка при скачивании файла", e);
            res.status(500);
            return errorResponse("Ошибка сервера: " + e.getMessage());
        }
    }

    /**
     * Удалить файл
     * DELETE /api/files/:id?userId={userId}
     */
    public String deleteFile(Request req, Response res) {
        try {
            UUID fileId = UUID.fromString(req.params(":id"));
            String userIdStr = req.queryParams("userId");

            if (userIdStr == null || userIdStr.isEmpty()) {
                res.status(400);
                return errorResponse("Параметр userId обязателен");
            }
            UUID userId = UUID.fromString(userIdStr);

            fileService.deleteFile(userId, fileId);

            res.type("application/json");
            res.status(200);
            return gson.toJson(Map.of(
                    "success", true,
                    "message", "Файл успешно удален"
            ));

        } catch (IllegalArgumentException e) {
            res.status(400);
            return errorResponse("Неверный формат UUID");
        } catch (SecurityException e) {
            res.status(403);
            return errorResponse("Доступ запрещен");
        } catch (RuntimeException e) {
            res.status(404);
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            logger.error("Ошибка при удалении файла", e);
            res.status(500);
            return errorResponse("Ошибка сервера: " + e.getMessage());
        }
    }

    /**
     * Удалить все файлы или файлы определённой категории
     * DELETE /api/files?userId={userId}&category={category}
     */
    public String deleteAllFiles(Request req, Response res) {
        try {
            String userIdStr = req.queryParams("userId");
            String category = req.queryParams("category");

            if (userIdStr == null || userIdStr.isEmpty()) {
                res.status(400);
                return errorResponse("Параметр userId обязателен");
            }

            UUID userId = UUID.fromString(userIdStr);

            if (category != null && !category.isBlank() && !category.equals("shared")) {
                fileService.deleteFilesByCategory(userId, category);
            } else {
                fileService.deleteAllFiles(userId);
            }

            res.type("application/json");
            res.status(200);
            return gson.toJson(Map.of(
                    "success", true,
                    "message", "Файлы успешно удалены"
            ));

        } catch (IllegalArgumentException e) {
            res.status(400);
            return errorResponse("Неверный формат UUID");
        } catch (SecurityException e) {
            res.status(403);
            return errorResponse("Доступ запрещен");
        } catch (RuntimeException e) {
            res.status(404);
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            logger.error("Ошибка при удалении файлов", e);
            res.status(500);
            return errorResponse("Ошибка сервера: " + e.getMessage());
        }
    }

    /**
     * Обновить метаданные файла
     * PUT /api/files/:id?userId={userId}
     * Body: {"newName": "new_filename.txt"}
     */
    public String updateFileMetadata(Request req, Response res) {
        try {
            UUID fileId = UUID.fromString(req.params(":id"));
            String userIdStr = req.queryParams("userId");

            if (userIdStr == null || userIdStr.isEmpty()) {
                res.status(400);
                return errorResponse("Параметр userId обязателен");
            }
            UUID userId = UUID.fromString(userIdStr);

            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String newName = body.get("newName");

            if (newName == null || newName.trim().isEmpty()) {
                res.status(400);
                return errorResponse("Параметр newName обязателен");
            }

            File updatedFile = fileService.updateFileMetadata(userId, fileId, newName);

            res.type("application/json");
            res.status(200);
            return gson.toJson(Map.of(
                    "success", true,
                    "message", "Метаданные файла обновлены",
                    "file", updatedFile
            ));

        } catch (IllegalArgumentException e) {
            res.status(400);
            return errorResponse("Неверный формат данных");
        } catch (SecurityException e) {
            res.status(403);
            return errorResponse("Доступ запрещен");
        } catch (RuntimeException e) {
            res.status(404);
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            logger.error("Ошибка при обновлении метаданных", e);
            res.status(500);
            return errorResponse("Ошибка сервера: " + e.getMessage());
        }
    }

    private String errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return gson.toJson(error);
    }

    public String getUserCategories(Request req, Response res) {
        try {
            String userIdStr = req.queryParams("userId");

            if (userIdStr == null || userIdStr.isEmpty()) {
                res.status(400);
                return errorResponse("Параметр userId обязателен");
            }

            UUID userId = UUID.fromString(userIdStr);
            List<String> categories = fileService.getUserCategories(userId);

            res.type("application/json");
            res.status(200);
            return gson.toJson(Map.of(
                    "success", true,
                    "categories", categories,
                    "count", categories.size()
            ));
        } catch (Exception e) {
            logger.error("Ошибка при получении категорий", e);
            res.status(500);
            return errorResponse("Ошибка сервера: " + e.getMessage());
        }
    }
}