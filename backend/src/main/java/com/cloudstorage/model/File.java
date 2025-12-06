package com.cloudstorage.model;


import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Модель файла в облачном хранилище.
 */
public class File {
    private UUID id;
    private User user;              // Владелец файла
    private String s3Key;           // Ключ в S3 (например: "user/123/filename.jpg")
    private String originalName;    // Оригинальное имя файла
    private Long fileSize;          // Размер в байтах
    private String mimeType;        // Тип файла
    private Boolean isPublic;       // Публичный ли файл
    private String description;     // Описание файла
    private String tags;            // Теги (через запятую)
    private LocalDateTime uploadedAt;    // Когда загружен
    private LocalDateTime lastAccessedAt; // Когда последний раз скачивали

    public File() {
        this.id = UUID.randomUUID();
        this.uploadedAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
        this.isPublic = false;
    }

    public File(User user, String originalName, Long fileSize, String mimeType) {
        this();
        this.user = user;
        this.originalName = originalName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getS3Key() { return s3Key; }
    public String getOriginalName() { return originalName; }
    public Long getFileSize() { return fileSize; }
    public String getMimeType() { return mimeType; }
    public Boolean getIsPublic() { return isPublic; }
    public String getDescription() { return description; }
    public String getTags() { return tags; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }

    public void setId(UUID id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    public void setDescription(String description) { this.description = description; }
    public void setTags(String tags) { this.tags = tags; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }


    /**
     * Возвращает удобочитаемый размер файла.
     * нужно, чтобы во фронте не ебаться
     */
    public String getFormattedSize() {
        if (fileSize == null) return "0 B";

        long bytes = fileSize;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }



    /**
     * Возвращает расширение файла.
     * более безопасная тк проверяет фактическое расширение
     * плюс удобно иконки рисовать
     *
     * то что мы получаем от геттера возвращает браузер
     */
    public String getExtension() {
        if (originalName == null) return "";
        int lastDot = originalName.lastIndexOf('.');
        return lastDot > 0 ? originalName.substring(lastDot + 1).toLowerCase() : "";
    }
}
