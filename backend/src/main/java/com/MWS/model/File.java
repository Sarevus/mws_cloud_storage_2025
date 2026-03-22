package com.MWS.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Модель файла в облачном хранилище.
 */
@Getter
@Entity
@Table(name = "files")
public class File {
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;                // Уникальный ID файла в БД

    @Setter
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;              // Владелец файла

    @Setter
    @Column(name = "link", nullable = false)
    private String s3Key;           // Ключ в S3 (путь: user/{userId}/{timestamp}_{filename})

    @Setter
    private String originalName;    // Оригинальное имя файла

    @Setter
    private Long size;              // Размер в байтах

    @Setter
    private String mimeType;        // MIME-тип (image/jpeg, application/pdf и т.д.)

    @Column(name = "category")
    private String category;

    @Getter
    @Setter
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Контент файла.
     * <p>
     * В БД это НЕ хранится — тело файла лежит в S3/Ceph.
     * Поле нужно, чтобы можно было:
     * - при upload передать поток в репозиторий (repo сам загрузит в S3),
     * - при download вернуть поток наружу.
     */

    @Setter
    @Transient
    private transient InputStream contentStream;

    public File() {
    }

    public File(UserEntity user, String originalName, Long size, String mimeType, String category) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.originalName = originalName;
        this.size = size;
        this.mimeType = mimeType;
        this.category = category != null ? category : "general";
        this.createdAt = LocalDateTime.now();
    }

    public void setCategory(String category) {
        this.category = category != null ? category : "general";
    }

    /**
     * Возвращает удобочитаемый размер файла.
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
     * <p>
     * то что мы получаем от геттера возвращает браузер
     */
    public String getExtension() {
        if (originalName == null) return "";
        int lastDot = originalName.lastIndexOf('.');
        return lastDot > 0 ? originalName.substring(lastDot + 1).toLowerCase() : "";
    }
}
