package com.MWS.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "folder_manager")
@Data  // автоматически генерирует геттеры/сеттеры
@NoArgsConstructor
public class FolderManager {
    @EmbeddedId  // поле является составным первичным ключом
    private FolderManagerId id;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @Column(name = "added_by", length = 50)
    private String addedBy;

    @PrePersist
    public void onCreate() {
        addedAt = LocalDateTime.now();
    }

    /**
     * Помечает класс как встраиваемый (может быть частью другой сущности)
     * Можем использовать класс как составной ключ
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    public static class FolderManagerId implements Serializable {
        @Column(name = "folder_id")
        private UUID folderId;

        @Column(name = "file_id")
        private UUID fileId;

        public FolderManagerId(UUID folderId, UUID fileId) {
            this.folderId = folderId;
            this.fileId = fileId;
        }
    }
}
