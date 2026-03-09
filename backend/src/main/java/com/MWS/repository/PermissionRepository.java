package com.MWS.repository;

import com.MWS.model.FilePermission;
import com.MWS.model.Roles;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<FilePermission, UUID> {
    List<FilePermission> findByFileId(UUID fileId);

    List<FilePermission> findByUserId(UUID userId);

    List<FilePermission> findByUserIdAndFileId(UUID userId, UUID fileId);

    List<FilePermission> findByFileIdAndRole(UUID fileId, Roles role);

    Optional<FilePermission> findOwnerByFileId(UUID fileId);

    @Query("SELECT CASE WHEN COUNT(fp) > 0 THEN true ELSE false END FROM FilePermission fp " +
            "WHERE fp.file.id = :fileId AND fp.userId = :userId " +
            "AND fp.role = :requiredRole AND fp.takenBackAt IS NULL")
    boolean hasPermission(@Param("fileId") UUID fileId, @Param("userId") UUID userId, @Param("requiredRole") Roles requiredRole);

    @Modifying
    @Transactional
    @Query("UPDATE FilePermission fp SET fp.takenBackAt = CURRENT_TIMESTAMP WHERE fp.id = :id")
    void blockPermission(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE FilePermission fp SET fp.takenBackAt = CURRENT_TIMESTAMP WHERE fp.userId = :userId")
    void blockAllPermissions(@Param("userId") UUID userId);

    @Query("SELECT CASE WHEN COUNT(fp) > 0 THEN true ELSE false END FROM FilePermission fp " +
            "WHERE fp.file.id = :fileId AND fp.userId = :userId AND fp.role = 'OWNER'")
    boolean isOwner(@Param("fileId") UUID fileId, @Param("userId") UUID userId);
}
