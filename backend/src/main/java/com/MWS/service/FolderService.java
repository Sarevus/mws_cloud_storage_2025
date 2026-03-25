package com.MWS.service;

import com.MWS.dto.FolderContextDto;
import com.MWS.dto.FolderDto;
import com.MWS.dto.create_update.FileDto;
import com.MWS.exception.DuplicateFolderException;
import com.MWS.model.File;
import com.MWS.model.Folder;
import com.MWS.repository.FileRepository;
import com.MWS.repository.FolderManagerRepository;
import com.MWS.repository.FolderRepository;
import com.MWS.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class FolderService {
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final FolderManagerRepository folderManagerRepository;
    private final FileService fileService;
    private final FolderManagerService folderManagerService;

    @Autowired
    public FolderService(FileRepository fileRepository,
                         UserRepository userRepository,
                         FolderRepository folderRepository,
                         FolderManagerRepository folderManagerRepository, FileService fileService, FolderManagerService folderManagerService) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.folderRepository = folderRepository;
        this.folderManagerRepository = folderManagerRepository;
        this.fileService = fileService;
        this.folderManagerService = folderManagerService;
    }

    @Transactional
    public FolderDto createFolder(String name, UUID parentId, UUID ownerId) {
        if (folderRepository.existsByOwnerIdAndParentIdAndName(ownerId, parentId, name)) {
            throw new DuplicateFolderException(name);
        }

        userRepository.findById(ownerId).orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        if (parentId != null && !folderRepository.existsById(parentId)) {
            throw new IllegalArgumentException("Папки не существует");
        }

        Folder newFolder = new Folder(name, parentId, ownerId);
        String newPath = pathBuilder(name, parentId);

        newFolder.setPath(newPath);
        newFolder.setCreatedAt(LocalDateTime.now());
        newFolder.setUpdatedAt(LocalDateTime.now());

        Folder savedFolder = folderRepository.save(newFolder);

        if (parentId != null) {
            folderRepository.incrementFoldersQuantity(savedFolder.getParentId());
        }

        return toFolderDto(savedFolder);
    }

    public FolderContextDto getFolderContext(UUID folderId) {
        Folder folder = folderRepository.findById(folderId).
                orElseThrow(() -> new IllegalArgumentException("Родительской папки не существует"));

        List<FolderDto> allSubfolders = folderRepository.findSubfolders(folder.getPath() + "%", folder.getOwnerId())
                .stream()
                .map(this::toFolderDto)
                .toList();

        List<FileDto> allFiles = fileRepository.findAllById(folderManagerRepository.findFileIdsByFolderId(folderId))
                .stream()
                .map(this::toFileDto)
                .toList();

        return new FolderContextDto(allSubfolders, allFiles);
    }

    @Transactional
    public FolderDto renameFolder(UUID folderId, String newName, UUID ownerId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Папки не существует"));

        if (folderRepository.existsByOwnerIdAndParentIdAndName(ownerId, folder.getParentId(), newName)) {
            throw new DuplicateFolderException(newName);
        }

        folder.setName(newName);
        folder.setPath(pathBuilder(newName, folder.getParentId()));
        folder.setUpdatedAt(LocalDateTime.now());

        return toFolderDto(folderRepository.save(folder));
    }

    @Transactional
    public void moveFolder(UUID folderId, UUID ownerId, UUID newParentId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Папки не существует"));

        String originalPath = folder.getPath();
        UUID oldParent = folder.getParentId();

        if (newParentId != null) {
            folderRepository.findById(newParentId)
                    .orElseThrow(() -> new IllegalArgumentException("Родительской папки не существует"));
        }

        if (newParentId != null && folder.getParentId().equals(newParentId)) {
            throw new IllegalArgumentException("Нельзя переместить папку саму в себя");
        }

        folder.setPath(pathBuilder(folder.getName(), newParentId));
        folder.setParentId(newParentId);
        folder.setUpdatedAt(LocalDateTime.now());
        folderRepository.save(folder);

        updatedSubfoldersPaths(originalPath, folder.getPath(), ownerId);

        if (oldParent != null) {
            folderRepository.decrementFoldersQuantity(oldParent);
        }

        if (newParentId != null) {
            folderRepository.incrementFoldersQuantity(newParentId);
        }
    }

    @Transactional
    public void deleteFolder(UUID folderId, UUID ownerId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Папки не существует"));

        UUID parentId = folder.getParentId();

        if (!folder.getOwnerId().equals(ownerId)) {
            throw new SecurityException("Только владелец может удалить папку");
        }

        // Проверка на пустоту (если нужно удалять только пустые папки)
        if (folder.getFoldersQuantity() > 0 || folder.getFilesQuantity() > 0) {
            throw new IllegalArgumentException("Папка " + folder.getName() + " не пуста. Используйте рекурсивное удаление");
        }

        folderRepository.deleteById(folderId);

        if (parentId != null) {
            folderRepository.decrementFoldersQuantity(parentId);
        }
    }

    @Transactional
    public void deleteFolderRecursively(UUID folderId, UUID ownerId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Папки не существует"));

        UUID parentId = folder.getParentId();

        if (!folder.getOwnerId().equals(ownerId)) {
            throw new SecurityException("Только владелец может удалить папку");
        }

        // Удаляем файлы из хранилища!
        List<FileDto> files = getAllFilesInFolder(folderId);

        for (FileDto file : files) {
            try {
                fileService.deleteFile(ownerId, file.id());
            } catch (Exception ignored) {
            }
        }

        // Удаляем все связи с файлами в этой папке
        folderManagerRepository.deleteByIdFolderId(folderId);

        // Рекурсивно удаляем подпапки
        List<Folder> subfolders = folderRepository.findByParentId(folderId);
        for (Folder subfolder : subfolders) {
            deleteFolderRecursively(subfolder.getId(), subfolder.getOwnerId());
        }

        // Удаляем саму папку
        folderRepository.deleteById(folderId);

        // Обновляем количество папок в родительской папке
        if (parentId != null) {
            folderRepository.decrementFoldersQuantity(parentId);
        }
    }

    public List<FileDto> getAllFilesInFolder(UUID folderId) {
        List<File> files = fileRepository.findAllById(
                folderManagerRepository.findFileIdsByFolderId(folderId)
        );

        return files.stream()
                .map(this::toFileDto)
                .toList();
    }

    public List<Folder> getUserRootFolders(UUID userId) {
        return folderRepository.findByOwnerIdAndParentIdOrderByName(userId, null);
    }

    private String pathBuilder(String name, UUID parentId) {
        return getParentPath(parentId) + "/" + name;
    }

    private String getParentPath(UUID parentId) {
        if (parentId != null) {
            return folderRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Путь к папке не найден"))
                    .getPath();
        }
        return "";
    }

    public void updatedSubfoldersPaths(String oldPath, String newPath, UUID ownerId) {
        List<Folder> subfolders = folderRepository.findSubfolders(oldPath + "%", ownerId);

        for (Folder subfolder : subfolders) {
            String updatedPath = subfolder.getPath().replace(oldPath, newPath);
            subfolder.setPath(updatedPath);
            subfolder.setUpdatedAt(LocalDateTime.now());
        }

        folderRepository.saveAll(subfolders);
    }

    private FolderDto toFolderDto(Folder folder) {
        if (folder == null) {
            return null;
        }

        return new FolderDto(
                folder.getId(),
                folder.getName(),
                folder.getPath(),
                folder.getParentId(),
                folder.getOwnerId(),
                folder.getSize(),
                folder.getFilesQuantity(),
                folder.getFoldersQuantity(),
                folder.getCreatedAt(),
                folder.getUpdatedAt()
        );
    }

    private FileDto toFileDto(File file) {
        if (file == null) return null;

        return new FileDto(
                file.getId(),
                file.getOriginalName(),
                file.getSize(),
                file.getMimeType(),
                file.getExtension(),
                file.getCategory(),
                file.getUser().getId(),
                file.getS3Key()
        );
    }
}