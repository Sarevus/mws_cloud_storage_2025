package com.MWS.model;

import java.io.InputStream;
import java.util.UUID;

/**
 * Модель файла в облачном хранилище.
 */
public class File {
    private UUID id;                // Уникальный ID файла в БД
    private UserEntity user;              // Владелец файла
    private String s3Key;           // Ключ в S3 (путь: user/{userId}/{timestamp}_{filename})
    private String originalName;    // Оригинальное имя файла
    private Long size;              // Размер в байтах
    private String mimeType;        // MIME-тип (image/jpeg, application/pdf и т.д.)

    /**
     * Контент файла.
     *
     * В БД это НЕ хранится — тело файла лежит в S3/Ceph.
     * Поле нужно, чтобы можно было:
     *  - при upload передать поток в репозиторий (repo сам загрузит в S3),
     *  - при download вернуть поток наружу.
     */
    private transient InputStream contentStream;
    //private Boolean isPublic;       // Публичный ли файл
    //private String description;     // Описание файла
    //private LocalDateTime uploadedAt;    // Дата загрузки
    //private LocalDateTime updatedAt;     // Дата обновления

    public File() {}

    public File(UserEntity user, String originalName, Long size, String mimeType) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.originalName = originalName;
        this.size = size;
        this.mimeType = mimeType;
        //this.isPublic = false;
        //this.uploadedAt = LocalDateTime.now();
        //this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UserEntity getUser() { return user; }
    public String getS3Key() { return s3Key; }
    public String getOriginalName() { return originalName; }
    public Long getSize() { return size; }
    public String getMimeType() { return mimeType; }

    public InputStream getContentStream() { return contentStream; }
//    public Boolean getIsPublic() { return isPublic; }
//    public String getDescription() { return description; }
//    public LocalDateTime getUploadedAt() { return uploadedAt; }
//    public LocalDateTime getUpdatedAt() { return updatedAt; }


    public void setId(UUID id) { this.id = id; }
    public void setUser(UserEntity user) { this.user = user; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public void setSize(Long size) { this.size = size; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public void setContentStream(InputStream contentStream) { this.contentStream = contentStream; }
//    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
//    public void setDescription(String description) { this.description = description; }
//    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
//    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }


    /**
     * Возвращает удобочитаемый размер файла.
     * нужно, чтобы во фронте не ебаться
     */
    public String getFormattedSize() {
        if (size == null) return "0 B";

        long bytes = size;
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
