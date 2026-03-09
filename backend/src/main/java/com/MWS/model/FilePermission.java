package com.MWS.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_permissions")
public class FilePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Roles role;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "taken_back_at")
    private LocalDateTime takenBackAt;

    public FilePermission() {
        this.id = UUID.randomUUID();
        this.grantedAt = LocalDateTime.now();
    }

    public FilePermission(File file, Roles role, UUID ownerId, UUID userId) {
        this.file = file;
        this.ownerId = ownerId;
        this.userId = userId;
        this.role = role;
        this.grantedAt = LocalDateTime.now();
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setRole(Roles role) {
        this.role = role;
    }

    public void setTakenBackAt() {
        this.takenBackAt = LocalDateTime.now();
    }

    public UUID getOwnerId() { return ownerId; }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public void setTakenBackAt(LocalDateTime takenBackAt) {
        this.takenBackAt = takenBackAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public File getFile() {
        return file;
    }

    public Roles getRole() {
        return role;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public LocalDateTime getTakenBackAt() {
        return takenBackAt;
    }
}
