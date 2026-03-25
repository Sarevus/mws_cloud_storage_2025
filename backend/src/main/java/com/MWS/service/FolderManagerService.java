package com.MWS.service;

import com.MWS.dto.get.GetFolderStatisticsDto;
import com.MWS.exception.DuplicateFileException;
import com.MWS.model.*;
import com.MWS.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FolderManagerService {
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final FolderManagerRepository folderManagerRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(FolderManagerService.class);

    @Autowired
    public FolderManagerService(
            FileRepository fileRepository,
            FolderRepository folderRepository,
            FolderManagerRepository folderManagerRepository,
            PermissionRepository permissionRepository,
            UserRepository userRepository) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.folderManagerRepository = folderManagerRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void addFilesToFolder(UUID folderId, List<UUID> fileIds, String userEmail) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Папки не существует"));

        UUID userId = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"))
                .getId();

        if (!folder.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("Добавлять файлы может только владелец");
        }

        addFilesToFolderLogics(folderId, fileIds, userEmail, userId);
    }

    @Transactional
    public void removeFilesFromFolder(UUID folderId, List<UUID> fileIds, UUID userId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Папки не существует"));

        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        if (!folder.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("Удалять файлы может только владелец");
        }

        removeFilesFromFolderLogics(folderId, fileIds);
    }

    @Transactional
    public void moveFiles(List<UUID> fileIds, UUID sourceFolderId, UUID targetFolderId, String movedBy) {
        if (!folderRepository.existsById(sourceFolderId)) {
            throw new IllegalArgumentException("Исходная папка не найдена");
        }

        if (!folderRepository.existsById(targetFolderId)) {
            throw new IllegalArgumentException("Целевая папка не найдена");
        }

        UUID userId = userRepository.findByEmail(movedBy)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"))
                .getId();

        removeFilesFromFolderLogics(sourceFolderId, fileIds);
        addFilesToFolderLogics(targetFolderId, fileIds, movedBy, userId);
    }

    @Transactional
    public void copyFilesToFolder(List<UUID> fileIds, UUID targetFolderId, String copiedBy) {
        if (!folderRepository.existsById(targetFolderId)) {
            throw new IllegalArgumentException("Целевая папка не найдена");
        }

        UUID userId = userRepository.findByEmail(copiedBy)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"))
                .getId();

        addFilesToFolderLogics(targetFolderId, fileIds, copiedBy, userId);
    }

    public List<File> getFilesInFolder(UUID folderId) {
        if (!folderRepository.existsById(folderId)) {
            throw new IllegalArgumentException("Папка не найдена");
        }
        return fileRepository.findAllById(folderManagerRepository.findFileIdsByFolderId(folderId));
    }

    public List<Folder> getFoldersWithFile(UUID fileId) {
        if (!fileRepository.existsById(fileId)) {
            throw new IllegalArgumentException("Файл не найден");
        }
        return folderRepository.findAllById(folderManagerRepository.findFolderIdsByFileId(fileId));
    }

    public boolean isFileInFolder(UUID fileId, UUID folderId) {
        return folderManagerRepository.existsByIdFolderIdAndIdFileId(folderId, fileId);
    }

    public GetFolderStatisticsDto getFolderStatistics(UUID folderId) {
        if (!folderRepository.existsById(folderId)) {
            throw new IllegalArgumentException("Папка не найдена");
        }

        int filesQuantity = folderManagerRepository.countByIdFolderId(folderId);
        long totalSize = folderManagerRepository.getTotalSizeInFolder(folderId);

        Pageable limit = PageRequest.of(0, 5);
        List<File> recentFiles = folderManagerRepository.findRecentFilesInFolder(folderId, limit);

        return new GetFolderStatisticsDto(
                filesQuantity,
                totalSize,
                recentFiles
        );
    }

    private void incrementFoldersSize(UUID folderId, long fileSize) {
        while (folderId != null) {
            folderRepository.incrementFilesQuantity(folderId, fileSize);
            folderId = folderRepository.findById(folderId)
                    .orElseThrow(() -> new IllegalArgumentException("Папка не найдена"))
                    .getParentId();
        }
    }

    private void decrementFoldersSize(UUID folderId, long fileSize) {
        while (folderId != null) {
            folderRepository.decrementFilesQuantity(folderId, fileSize);
            folderId = folderRepository.findById(folderId)
                    .orElseThrow(() -> new IllegalArgumentException("Папка не найдена"))
                    .getParentId();
        }
    }

    private void addFilesToFolderLogics(UUID folderId, List<UUID> fileIds, String addedBy, UUID userId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Папка не найдена"));

        for (UUID fileId : fileIds) {
            try {
                File file = fileRepository.findById(fileId)
                        .orElseThrow(() -> new IllegalArgumentException("Файл не найден"));

                if (isFileInFolder(folderId, fileId)) {
                    throw new DuplicateFileException(file.getOriginalName());
                }

                FolderManager.FolderManagerId complexId = new FolderManager.FolderManagerId(folderId, fileId);
                FolderManager folderManager = new FolderManager();
                folderManager.setId(complexId);
                folderManager.setAddedBy(addedBy);
                folderManagerRepository.save(folderManager);

                folderRepository.incrementFilesQuantity(folderId, file.getSize());

                if (folder.getParentId() != null) {
                    incrementFoldersSize(folder.getParentId(), file.getSize());
                }
            } catch (IllegalArgumentException e) {
                logger.error(e.getMessage());
            }
        }
    }

    private void removeFilesFromFolderLogics(UUID folderId, List<UUID> fileIds) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Папка не найдена"));

        for (UUID fileId : fileIds) {
            try {
                File file = fileRepository.findById(fileId)
                        .orElseThrow(() -> new IllegalArgumentException("Файл не найден"));

                if (!isFileInFolder(folderId, fileId)) {
                    throw new IllegalArgumentException("Файла " + file.getOriginalName() + " нет в папке");
                }

                FolderManager.FolderManagerId complexId = new FolderManager.FolderManagerId(folderId, fileId);
                folderManagerRepository.deleteById(complexId);
                folderRepository.decrementFilesQuantity(folderId, file.getSize());

                if (folder.getParentId() != null) {
                    decrementFoldersSize(folder.getParentId(), file.getSize());
                }
            } catch (IllegalArgumentException e) {
                logger.error(e.getMessage());
            }
        }
    }
}
