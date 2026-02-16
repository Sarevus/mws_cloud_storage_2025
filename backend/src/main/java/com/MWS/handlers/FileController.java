package com.MWS.handlers;

import com.MWS.model.File;
import com.MWS.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Контроллер для работы с файлами через HTTP API
 */

@RestController
@RequestMapping("/api/files") // определяем базовый url для всех методов
public class FileController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;
    private final long maxFileSize;

    @Autowired
    public FileController(FileService fileService, @Value("${file.max-size:10485760}") long maxFileSize) {
        this.fileService = fileService;
        this.maxFileSize = maxFileSize;
    }

    /**
     * Получить список всех файлов пользователя
     * GET /api/files?userId={userId}
     */
    @GetMapping
    public ResponseEntity<?> listFiles(@RequestParam UUID userId, @RequestParam(required = false) String category) {
        try {
            logger.debug("Получение списка файлов для пользователя: {}", userId);

            List<File> files = fileService.getUserFiles(userId);

            Map<String, Object> response = new HashMap<>();

            response.put("success", true);
            response.put("files", files);
            response.put("count", files.size());
            response.put("category", category != null ? category : "general");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Неверный формат UUID", e);
            return ResponseEntity.badRequest().body(errorResponse("Неверный формат userId"));
        } catch (Exception e) {
            logger.error("Ошибка при получении списка файлов", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse("Ошибка сервера: " + e.getMessage()));
        }
    }

    /**
     * Загрузить файл
     * POST /api/files/upload
     * Content-Type: multipart/form-data
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam UUID userId,
                                        @RequestParam(required = false) String category,
                                        @RequestParam("file") MultipartFile file) {
        try {
            logger.info("Загрузка файла: {}, размер: {} байт, тип: {}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            // Проверяем, пустой ли файл
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(errorResponse("Файл не найден в запросе"));
            }

            // Проверяем размер файла
            long fileSize = file.getSize();
            if (fileSize > maxFileSize) {
                return ResponseEntity.badRequest().body(errorResponse(
                        "Файл слишком большой. Максимальный размер: "
                                + (maxFileSize / 1024 / 1024) + " MB"
                ));
            }

            // Получаем данные файла
            String originalFilename = file.getOriginalFilename();
            String mimeType = file.getContentType();
            InputStream fileStream = file.getInputStream();

            String finalCategory = category != null ? category : "general";
            logger.info("Загрузка файла в категорию: {}", finalCategory);

            // Загружаем файл через сервис
            File uploadedFile = fileService.uploadFile(
                    userId,
                    originalFilename,
                    fileStream,
                    fileSize,
                    mimeType,
                    finalCategory
            );

            // Закрываем поток
            fileStream.close();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Файл успешно загружен");
            response.put("file", uploadedFile);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Неверные параметры", e);
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        } catch (IOException e) {
            logger.error("Ошибка при чтении файла", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse("Ошибка чтения файла: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Ошибка при загрузке файла", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse("Ошибка загрузки: " + e.getMessage()));
        }
    }

    /**
     * Получить метаданные файла
     * GET /api/files/:id?userId={userId}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getFileMetadata(@PathVariable UUID id, @RequestParam UUID userId) {
        try {
            logger.debug("Получение метаданных файла {} для пользователя {}", id, userId);

            File file = fileService.getFileMetadata(userId, id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("file", file);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorResponse("Неверный формат UUID"));
        } catch (SecurityException e) {
            // 403 - доступ запрещен
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse("Доступ запрещен"));
        } catch (RuntimeException e) {
            // 404 - файл не найден
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Ошибка при получении метаданных файла", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse("Ошибка сервера: " + e.getMessage()));
        }
    }

    /**
     * Скачать файл
     * GET /api/files/:id/download?userId={userId}
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadFile(@PathVariable UUID id, @RequestParam UUID userId) {
        try {
            logger.info("Скачивание файла {} пользователем {}", id, userId);

            // Получаем метаданные файла
            File file = fileService.getFileMetadata(userId, id);

            // Скачиваем файл из S3/Ceph
            InputStream fileStream = fileService.downloadFile(userId, id);

            byte[] buffer = fileStream.readAllBytes();
            fileStream.close();

            logger.info("✅ Файл {} успешно отправлен пользователю {}", id, userId);

            return ResponseEntity.ok()
                    .header("Content-Type", file.getMimeType())
                    .header("Content-Disposition", "attachment; filename=\"" + file.getOriginalName() + "\"")
                    .header("Content-Length", String.valueOf(file.getSize()))
                    .body(buffer);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorResponse("Неверный формат UUID"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse("Доступ запрещен"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Ошибка при скачивании файла", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse("Ошибка сервера: " + e.getMessage()));
        }
    }

    /**
     * Удалить файл
     * DELETE /api/files/:id?userId={userId}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable UUID id, @RequestParam UUID userId) {
        try {
            logger.info("Удаление файла {} пользователем {}", id, userId);

            fileService.deleteFile(userId, id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Файл успешно удален");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorResponse("Неверный формат UUID"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse("Доступ запрещен"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Ошибка при удалении файла", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse("Ошибка сервера: " + e.getMessage()));
        }
    }

    /**
     * Удалить все файлы или файлы определённой категории
     * DELETE /api/files?userId={userId}&category={category}
     */
    @DeleteMapping
    public ResponseEntity<?> deleteAllFiles(@RequestParam UUID userId, @RequestParam(required = false) String category) {
        try {
            logger.info("Массовое удаление файлов пользователя {} в категории {}",
                    userId, category != null ? category : "все");

            if (category != null && !category.isBlank() && !category.equals("shared")) {
                fileService.deleteFilesByCategory(userId, category);
            } else {
                fileService.deleteAllFiles(userId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Файлы успешно удалены");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorResponse("Неверный формат UUID"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse("Доступ запрещен"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Ошибка при удалении файлов", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse("Ошибка сервера: " + e.getMessage()));
        }
    }

    /**
     * Обновить метаданные файла
     * PUT /api/files/:id?userId={userId}
     * Body: {"newName": "new_filename.txt"}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFileMetadata(
            @PathVariable UUID id, @RequestParam UUID userId,
            @RequestBody Map<String, String> body
    ) {
        try {
            String newName = body.get("newName");

            File updatedFile = fileService.updateFileMetadata(userId, id, newName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Метаданные файла обновлены");
            response.put("file", updatedFile);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorResponse("Неверный формат данных"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse("Доступ запрещен"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Ошибка при обновлении метаданных", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse("Ошибка сервера: " + e.getMessage()));
        }
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return error;
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getUserCategories(@RequestParam UUID userId) {
        try {
            logger.debug("Получение категорий пользователя {}", userId);

            List<String> categories = fileService.getUserCategories(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("categories", categories);
            response.put("count", categories.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Ошибка при получении категорий", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse("Ошибка сервера: " + e.getMessage()));
        }
    }
}