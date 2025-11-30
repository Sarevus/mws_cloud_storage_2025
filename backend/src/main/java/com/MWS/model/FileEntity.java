package com.MWS.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "files")
public class FileEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", type = org.hibernate.id.UUIDGenerator.class)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "link", nullable = false)
    private String link;

    @OneToOne(mappedBy = "file", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private FileMetadata metadata;

    public FileEntity() {
    }

    public FileEntity(String link) {
        this.link = link;
    }

    public UUID getId() {
        return id;
    }

    public String getLink() {
        return link;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public FileMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(FileMetadata metadata) {
        this.metadata = metadata;
    }
}