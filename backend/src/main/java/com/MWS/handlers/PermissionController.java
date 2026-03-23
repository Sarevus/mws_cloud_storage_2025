package com.MWS.handlers;

import com.MWS.dto.FilePermissionDto;
import com.MWS.dto.ShareRequestDto;
import com.MWS.model.Roles;
import com.MWS.repository.FileRepository;
import com.MWS.service.FilePermissionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/permission")
@Transactional
public class PermissionController {
    private static final Logger logger = LoggerFactory.getLogger(PermissionController.class);

    private final FilePermissionService filePermissionService;
    private final FileRepository fileRepository;

    @Autowired
    public PermissionController(FilePermissionService filePermissionService, FileRepository fileRepository) {
        this.filePermissionService = filePermissionService;
        this.fileRepository = fileRepository;
    }

    @PostMapping("share")
    @ResponseStatus(HttpStatus.CREATED)
    public FilePermissionDto shareFile(@RequestBody ShareRequestDto shareRequest, HttpSession session) {

        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("No such user");
        }

        logger.info("Пользователь {} делится файлом {} с пользователем {}", userId, shareRequest.fileId(), shareRequest.userEmail());

        return filePermissionService.shareFile(
                shareRequest.fileId(),
                userId,
                shareRequest.userEmail(),
                shareRequest.role());
    }

    @DeleteMapping("/{fileId}/revoke/{targetUserId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void revokeAccess(@PathVariable UUID fileId, @PathVariable UUID targetUserId, HttpSession session) {

        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("No such user");
        }

        logger.info("Пользователь {} отзывает доступ к файлу {} для {}",
                userId, fileId, targetUserId);

        filePermissionService.revokeAccess(fileId, targetUserId, userId);
    }

    @PutMapping("/{fileId}/role/{targetUserId}")
    public FilePermissionDto changeRole(@PathVariable UUID fileId, @PathVariable UUID targetUserId, HttpSession session, Roles role) {

        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("No such user");
        }

        logger.info("Пользователь {} меняет роль для {} относительно файла {}",
                userId, targetUserId, fileId);

        return filePermissionService.changeRole(fileId, targetUserId, userId, role);
    }

    @GetMapping("/shared-with-me")
    public List<FilePermissionDto> getSharedWithMe(HttpSession session) {

        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("No such user");
        }

        logger.info("Пользователь {} запрашивает файлы, доступные ему", userId);
        return filePermissionService.getFilesSharedWithMe(userId);
    }

    @GetMapping("/{fileId}/accessors")
    public List<FilePermissionDto> getFileAccessors(@PathVariable UUID fileId, HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("No such user");
        }

        logger.info("Пользователь {} запрашивает список доступов к файлу {}", userId, fileId);

        return filePermissionService.getAllAccessors(fileId, userId);
    }

    @GetMapping("/check-access")
    public boolean checkAccess(@RequestParam UUID fileId,
                               @RequestParam(required = false) Roles currentRole,
                               HttpSession session) {

        filePermissionService.getFileRepository().findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File not found"));

        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            return false;
        }

        Roles role = currentRole != null ? currentRole : Roles.READER;
        return filePermissionService.checkAccess(fileId, userId, role);
    }
}
