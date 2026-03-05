package com.MWS.model;

import jakarta.persistence.*;

import java.io.InputStream;
import java.util.UUID;

/**
 * Модель файла в облачном хранилище.
 */
@Entity
@Table(name = "files")
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToOne(mappedBy = "file", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private MetadataEntity metadata;

    @Column(name = "s3_key")
    private String s3Key;

    private String category;

    @Transient
    private InputStream contentStream;

    public File() {
    }

    public File(UserEntity user, String originalName, Long size, String mimeType, String category) {
        this.user = user;
        this.category = (category != null) ? category : "general";

        MetadataEntity m = new MetadataEntity();
        m.setFile(this);
        m.setOriginalName(originalName);
        m.setSize(size);
        m.setMimeType(mimeType);
        // m.setIsPublic(false); // если есть
        this.metadata = m;
    }

    public UUID getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public String getS3Key() {
        return s3Key;
    }


    public String getCategory() {
        return category;
    }

    public MetadataEntity getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataEntity metadata) {
        this.metadata = metadata;
        if (metadata != null) {
            metadata.setFile(this);
        }
    }

    public InputStream getContentStream() {
        return contentStream;
    }
//    public Boolean getIsPublic() { return isPublic; }
//    public String getDescription() { return description; }
//    public LocalDateTime getUploadedAt() { return uploadedAt; }
//    public LocalDateTime getUpdatedAt() { return updatedAt; }


//    public void setId(UUID id) {
//        this.id = id;
//    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }


    public void setCategory(String category) {
        this.category = category != null ? category : "general";
    }

    public void setContentStream(InputStream contentStream) {
        this.contentStream = contentStream;
    }
//    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
//    public void setDescription(String description) { this.description = description; }
//    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
//    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }


    /**
     * Возвращает удобочитаемый размер файла.
     * нужно, чтобы во фронте не ебаться
     */
    public String getFormattedSize() {
        Long size = (metadata == null) ? null : metadata.getSize();
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
        String originalName = (metadata == null) ? null : metadata.getOriginalName();
        if (originalName == null) return "";
        int lastDot = originalName.lastIndexOf('.');
        return lastDot > 0 ? originalName.substring(lastDot + 1).toLowerCase() : "";
    }

}
