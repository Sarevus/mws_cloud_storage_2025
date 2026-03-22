package com.MWS.handlers;

import com.MWS.dto.create_update.FileDto;
import com.MWS.dto.folderManager.FilesRequestDto;
import com.MWS.dto.folderManager.RefractorDto;
import com.MWS.dto.get.GetFolderStatisticsDto;
import com.MWS.model.File;
import com.MWS.model.Folder;
import com.MWS.service.CategoryDetector;
import com.MWS.service.FileService;
import com.MWS.service.FolderManagerService;
import com.MWS.service.FolderService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FolderManagerController {
    private static final Logger logger = LoggerFactory.getLogger(FolderManagerController.class);

    private final FolderManagerService folderManagerService;
    private final FileService fileService;

    @Autowired
    public FolderManagerController(FolderService folderService,
                                   FolderManagerService folderManagerService,
                                   FileService fileService) {
        this.folderManagerService = folderManagerService;
        this.fileService = fileService;
    }

    @PostMapping("/folders/{folderId}/files")
    public void addFilesToFolder(@PathVariable UUID folderId,
                                 @RequestBody FilesRequestDto request,
                                 HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} добавляет файлы в папку {}", userId, request.folderName());

        folderManagerService.addFilesToFolder(folderId, request.fileIds(), request.addedBy());
    }

    @DeleteMapping("/folders/{folderId}/files")
    public void removeFilesFromFolder(@PathVariable UUID folderId,
                                      @RequestBody FilesRequestDto request,
                                      HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} удаляет файлы из папки {}", userId, request.folderName());

        folderManagerService.removeFilesFromFolder(folderId, request.fileIds(), userId);
    }

    @PutMapping("/folders/{folderId}/files/move")
    public void moveFiles(@PathVariable UUID folderId,
                          @RequestBody RefractorDto request,
                          HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} перемещает файлы из папки {} в папку {}",
                userId, request.sourceFolderId(), request.targetFolderId());

        folderManagerService.moveFiles(request.fileIds(),
                request.sourceFolderId(),
                request.targetFolderId(),
                (String) session.getAttribute("email"));
    }

    @PostMapping("/folders/{folderId}/files/copy")
    public void copyFilesToFolder(@PathVariable UUID folderId,
                                  @RequestBody RefractorDto request,
                                  HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} копирует файлы из папки {} в папку {}",
                userId, request.sourceFolderId(), request.targetFolderId());

        folderManagerService.copyFilesToFolder(request.fileIds(),
                request.targetFolderId(),
                (String) session.getAttribute("email"));
    }

    @GetMapping("/folders/{folderId}/files")
    public List<File> getFilesFromFolder(@PathVariable UUID folderId,
                                         HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} извлекает файлы из папки {}", userId, folderId);

        return folderManagerService.getFilesInFolder(folderId);
    }

    @GetMapping("/folders/{folderId}/statistics")
    public GetFolderStatisticsDto getFolderStatistics(@PathVariable UUID folderId,
                                                      HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} получает статистику о папке {}", userId, folderId);

        return folderManagerService.getFolderStatistics(folderId);
    }

    @GetMapping("/folders/{folderId}/files/{fileId}/exists")
    public boolean isFileInFolder(@PathVariable UUID folderId,
                                  @PathVariable UUID fileId,
                                  HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} проверяет, есть ли файл {} папке {}", userId, fileId, folderId);

        return folderManagerService.isFileInFolder(fileId, folderId);
    }

    @GetMapping("/files/{fileId}/folders")
    public List<Folder> getFoldersWithFile(@PathVariable UUID fileId,
                                           HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} получает папки, содержащие файл {}", userId, fileId);

        return folderManagerService.getFoldersWithFile(fileId);
    }

    @PostMapping("/upload-to-folder")
    public FileDto uploadFileToFolder(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folderId") UUID folderId,
            HttpSession session) throws IOException {

        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        logger.info("Пользователь {} загружает файл {} в папку {}", userId, file.getOriginalFilename(), folderId);

        // Загружаем файл через существующий метод uploadFile
        com.MWS.model.File uploadedFile = fileService.uploadFile(
                userId,
                file.getOriginalFilename(),
                file.getInputStream(),
                file.getSize(),
                file.getContentType(),
                CategoryDetector.detectCategory(file.getContentType(), file.getOriginalFilename())
        );

        // Привязываем к папке
        folderManagerService.addFilesToFolder(folderId, List.of(uploadedFile.getId()),
                (String) session.getAttribute("email"));

        return toFileDto(uploadedFile);
    }

    private FileDto toFileDto(com.MWS.model.File file) {
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