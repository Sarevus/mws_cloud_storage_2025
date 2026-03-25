package com.MWS.repository;

import com.MWS.model.File;
import com.MWS.model.FolderManager;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface FolderManagerRepository extends JpaRepository<FolderManager, FolderManager.FolderManagerId> {
    @Query("SELECT fm.id.fileId FROM FolderManager fm WHERE fm.id.folderId = :folderId")
    List<UUID> findFileIdsByFolderId(@Param("folderId") UUID folderId);

    @Query("SELECT fm.id.folderId FROM FolderManager fm WHERE fm.id.fileId = :fileId")
    List<UUID> findFolderIdsByFileId(@Param("fileId") UUID fileId);

    boolean existsByIdFolderIdAndIdFileId(UUID folderId, UUID fileID);

    @Modifying
    @Transactional
    void deleteByIdFolderId(UUID folderId);

    @Modifying
    @Transactional
    void deleteByIdFolderIdAndIdFileId(UUID folderId, UUID fileId);

    int countByIdFolderId(UUID folderId);

    @Query("SELECT COALESCE(SUM(f.size), 0) FROM File f " +
            "WHERE f.id IN (SELECT fm.id.fileId FROM FolderManager fm WHERE fm.id.folderId = :folderId)")
    long getTotalSizeInFolder(@Param("folderId") UUID folderId);

    @Query("SELECT f FROM File f " +
            "WHERE f.id IN (SELECT fm.id.fileId FROM FolderManager fm WHERE fm.id.folderId = :folderId) " +
            "ORDER BY f.createdAt DESC")
    List<File> findRecentFilesInFolder(@Param("folderId") UUID folderId, Pageable pageable);

}
