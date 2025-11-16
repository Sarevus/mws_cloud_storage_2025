package com.MWS.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "metadata")
public class FileMetadata {
    @Id
    private UUID fileId;

    @OneToOne
    @JoinColumn(name = "file_id")
    @MapsId
    private FileEntity file;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "description")
    private String description;

    @Column(name = "tags")
    private String tags;

    @Column(name = "is_public")
    private Boolean isPublic = false;

    // Конструкторы
    public FileMetadata() {
    }

    public FileMetadata(String filename, Long fileSize, String description, String tags, Boolean isPublic) {
        this.filename = filename;
        this.fileSize = fileSize;
        this.description = description;
        this.tags = tags;
        this.isPublic = isPublic;
    }


    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }


    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void setFile(FileEntity file) {
        this.file = file;
    }

    public FileEntity getFile() {
        return file;
    }
}