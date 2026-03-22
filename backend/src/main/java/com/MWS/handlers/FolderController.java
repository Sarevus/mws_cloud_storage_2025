package com.MWS.handlers;

import com.MWS.dto.FolderContextDto;
import com.MWS.dto.FolderDto;
import com.MWS.dto.create_update.FileDto;
import com.MWS.dto.folder.CreateFolderRequest;
import com.MWS.dto.folder.MoveFolderRequest;
import com.MWS.dto.folder.RenameFolderRequest;
import com.MWS.dto.get.GetFolderStatisticsDto;
import com.MWS.model.File;
import com.MWS.model.Folder;
import com.MWS.service.FileService;
import com.MWS.service.FolderManagerService;
import com.MWS.service.FolderService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/folders")
public class FolderController {
    private static final Logger logger = LoggerFactory.getLogger(FolderController.class);

    private final FolderService folderService;
    private final FolderManagerService folderManagerService;
    private final FileService fileService;

    @Autowired
    public FolderController(FolderService folderService,
                            FolderManagerService folderManagerService,
                            FileService fileService) {
        this.folderService = folderService;
        this.folderManagerService = folderManagerService;
        this.fileService = fileService;
    }

    @PostMapping
    public FolderDto createFolder(@RequestBody CreateFolderRequest request, HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} создаёт папку '{}' с parentId={}", userId, request.folderName(), request.parentId());
        return folderService.createFolder(request.folderName(), request.parentId(), userId);
    }

    @GetMapping("/{folderId}")
    public GetFolderStatisticsDto getFolderInfo(@PathVariable UUID folderId, HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} запрашивает статистику папки {}", userId, folderId);
        return folderManagerService.getFolderStatistics(folderId);
    }

    @GetMapping("/{folderId}/context")
    public FolderContextDto getFolderContext(@PathVariable UUID folderId, HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} запрашивает содержимое папки {}", userId, folderId);
        return folderService.getFolderContext(folderId);
    }

    @PatchMapping("/{folderId}")
    public FolderDto renameFolder(@PathVariable UUID folderId,
                                  @RequestBody RenameFolderRequest request,
                                  HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} переименовывает папку {} в '{}'", userId, folderId, request.newName());
        return folderService.renameFolder(folderId, request.newName(), userId);
    }

    @PutMapping("/{folderId}/move")
    public void moveFolder(@PathVariable UUID folderId,
                           @RequestBody MoveFolderRequest request,
                           HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} перемещает папку {} в папку {}", userId, folderId, request.newParentId());
        folderService.moveFolder(folderId, userId, request.newParentId());
    }

    @DeleteMapping("/{folderId}")
    public void deleteFolder(@PathVariable UUID folderId, HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} удаляет папку {}", userId, folderId);
        folderService.deleteFolder(folderId, userId);
    }

    @DeleteMapping("/{folderId}/recursive")
    public void deleteFolderRecursively(@PathVariable UUID folderId, HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} рекурсивно удаляет папку {}", userId, folderId);
        folderService.deleteFolderRecursively(folderId, userId);
    }

    @GetMapping
    public List<FolderDto> getFolders(@RequestParam UUID userId, HttpSession session) {
        UUID sessionUserId = (UUID) session.getAttribute("userId");
        if (sessionUserId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        if (!sessionUserId.equals(userId)) {
            throw new SecurityException("Доступ запрещён");
        }

        List<Folder> folders = folderService.getUserRootFolders(userId);

        return folders.stream()
                .map(this::toFolderDto)
                .toList();
    }

    @GetMapping("/{folderId}/download")
    public void downloadFolder(@PathVariable UUID folderId,
                               HttpSession session,
                               HttpServletResponse response) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} скачивает папку {}", userId, folderId);

        try {
            List<File> files = folderManagerService.getFilesInFolder(folderId);

            if (files.isEmpty()) {
                throw new IllegalArgumentException("Папка пуста");
            }

            // Создаем ZIP архив
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            for (File file : files) {
                try {
                    // Получаем InputStream файла из S3
                    InputStream fileStream = fileService.downloadFile(userId, file.getId());

                    // Создаем запись в ZIP
                    ZipEntry zipEntry = new ZipEntry(file.getOriginalName());
                    zos.putNextEntry(zipEntry);

                    // Копируем содержимое
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = fileStream.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }

                    zos.closeEntry();
                    fileStream.close();

                    logger.debug("Файл {} добавлен в архив", file.getOriginalName());
                } catch (Exception e) {
                    logger.error("Ошибка добавления файла {} в архив: {}", file.getOriginalName(), e.getMessage());
                }
            }

            zos.close();

            // Отправляем архив
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=folder_" + folderId + ".zip");
            response.getOutputStream().write(baos.toByteArray());
            response.getOutputStream().flush();

            logger.info("✅ Папка {} успешно скачана", folderId);

        } catch (Exception e) {
            logger.error("Ошибка скачивания папки:", e);
            throw new RuntimeException("Ошибка скачивания папки: " + e.getMessage(), e);
        }
    }

    private FolderDto toFolderDto(Folder folder) {
        if (folder == null) return null;

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
}