package com.MWS.repository;

import com.MWS.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FolderRepository extends JpaRepository<Folder, UUID> {
    List<Folder> findByOwnerIdAndParentIdOrderByName(UUID ownerId, UUID parentId);

    boolean existsByOwnerIdAndParentIdAndName(UUID ownerId, UUID parentId, String name);

    Optional<Folder> findByOwnerIdAndParentIdAndName(UUID ownerId, UUID parentId, String name);

    @Modifying
    @Transactional
    @Query("UPDATE Folder f SET f.filesQuantity = f.filesQuantity + 1, " +
            "f.size = f.size + :fileSize," +
            "f.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE f.id = :folderId")
    void incrementFilesQuantity(@Param("folderId") UUID folderId, @Param("fileSize") long fileSize);

    @Modifying
    @Transactional
    @Query("UPDATE Folder f SET f.foldersQuantity = f.foldersQuantity + 1, " +
            "f.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE f.id = :folderId")
    void incrementFoldersQuantity(@Param("folderId") UUID folderId);

    @Modifying
    @Transactional
    @Query("UPDATE Folder f SET f.filesQuantity = f.filesQuantity - 1, " +
            "f.size = f.size - :fileSize," +
            "f.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE f.id = :folderId AND f.filesQuantity > 0")
    void decrementFilesQuantity(@Param("folderId") UUID folderId, @Param("fileSize") long fileSize);

    @Modifying
    @Transactional
    @Query("UPDATE Folder f SET f.foldersQuantity = f.foldersQuantity - 1, " +
            "f.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE f.id = :folderId AND f.foldersQuantity > 0")
    void decrementFoldersQuantity(@Param("folderId") UUID folderId);

    @Query("SELECT f FROM Folder f WHERE f.path LIKE :path AND f.ownerId = :ownerId")
    List<Folder> findSubfolders(@Param("path") String path, @Param("ownerId") UUID ownerId);

    @Query("SELECT f FROM Folder f WHERE f.parentId = :parentId")
    List<Folder> findByParentId(@Param("parentId") UUID parentId);
}
