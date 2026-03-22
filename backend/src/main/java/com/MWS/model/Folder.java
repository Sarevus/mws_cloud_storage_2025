package com.MWS.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "folder")
public class Folder {
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @Getter
    @Column(nullable = false, length = 255)
    private String name;

    @Setter
    @Getter
    @Column(nullable = false, length = 2000)
    private String path;

    @Setter
    @Getter
    @Column(name = "parent_id")
    private UUID parentId;

    @Setter
    @Getter
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Getter
    @Setter
    private long size;

    @Getter
    @Setter
    @Column(name = "files_quantity")
    private int filesQuantity;

    @Setter
    @Getter
    @Column(name = "folders_quantity")
    private int foldersQuantity;

    @Setter
    @Getter
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Setter
    @Getter
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Folder() {
    }

    public Folder(String name, UUID parentId, UUID ownerId) {
        this.name = name;
        this.parentId = parentId;
        this.ownerId = ownerId;
        this.size = 0L;
        this.filesQuantity = 0;
        this.foldersQuantity = 0;
    }
}
