package com.MWS.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "metadata")
public class MetadataEntity {

    @Id
    @Column(name = "file_id")
    private UUID fileId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "file_id")
    @JsonIgnore
    private File file;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "size")
    private Long size;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "is_public")
    private Boolean isPublic;

    public MetadataEntity() {}

    public MetadataEntity(File file, String originalName, Long size, String mimeType, Boolean isPublic) {
        this.file = file;
        this.originalName = originalName;
        this.size = size;
        this.mimeType = mimeType;
        this.isPublic = isPublic;
    }

    public UUID getFileId() {
        return fileId;
    }

    public File getFile() {
        return file;
    }

    public String getOriginalName() {
        return originalName;
    }

    public Long getSize() {
        return size;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
}