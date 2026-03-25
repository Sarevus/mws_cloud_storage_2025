package com.MWS.service;

import com.MWS.dto.FilePermissionDto;
import com.MWS.dto.UserInfoDto;
import com.MWS.model.File;
import com.MWS.model.FilePermission;
import com.MWS.model.Roles;
import com.MWS.model.User;
import com.MWS.repository.FileRepository;
import com.MWS.repository.PermissionRepository;
import com.MWS.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FilePermissionService {
    private final PermissionRepository permissionRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    @Autowired
    public FilePermissionService(PermissionRepository permissionRepository,
                                 FileRepository fileRepository,
                                 UserRepository userRepository) {
        this.fileRepository = fileRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    public FileRepository getFileRepository() {
        return fileRepository;
    }

    @Transactional
    public FilePermissionDto shareFile(UUID fileId, UUID ownerId, String userEmail, Roles role) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getUser().getId().equals(ownerId)) {
            throw new SecurityException("Only owner can share files");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getId().equals(ownerId)) {
            throw new IllegalArgumentException("You cannot share file to yourself");
        }

        List<FilePermission> existing = permissionRepository.findByUserIdAndFileId(user.getId(), fileId);
        FilePermission filePermission;
        if (!existing.isEmpty()) {
            filePermission = existing.get(0);
            filePermission.setRole(role);
            filePermission.setTakenBackAt(null);
        } else {
            filePermission = new FilePermission(file, role, ownerId, user.getId());
        }

        FilePermission saved = permissionRepository.save(filePermission);
        return toDto(saved);
    }

    public List<FilePermissionDto> getAllAccessors(UUID fileId, UUID userId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getUser().getId().equals(userId)) {
            throw new SecurityException("Only owner can share files");
        }

        return permissionRepository.findByFileId(fileId).stream()
                .filter(p -> p.getTakenBackAt() == null)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeAccess(UUID fileId, UUID userId, UUID ownerId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getUser().getId().equals(ownerId)) {
            throw new SecurityException("Only owner can share files");
        }

        List<FilePermission> permissionsToRevoke = permissionRepository.findByUserIdAndFileId(userId, fileId);
        if (permissionsToRevoke.isEmpty()) {
            throw new IllegalArgumentException("Permissions not found");
        }

        for (FilePermission permission : permissionsToRevoke) {
            permission.setTakenBackAt();
        }
    }

    public boolean checkAccess(UUID fileId, UUID userId, Roles requiredRole) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (file.getUser().getId().equals(userId)) {
            return true;
        }

        boolean hasAccess = false;
        if (requiredRole == Roles.READER) {
            hasAccess = permissionRepository.hasPermission(fileId, userId, Roles.READER) ||
                    permissionRepository.hasPermission(fileId, userId, Roles.EDITOR);
        } else {
            hasAccess = permissionRepository.hasPermission(fileId, userId, requiredRole);
        }

        if (!hasAccess) {
            throw new SecurityException("Доступ к файлу запрещён");
        }

        return hasAccess;
    }

    @Transactional
    public FilePermissionDto changeRole(UUID fileId, UUID userId, UUID ownerId, Roles newRole) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getUser().getId().equals(ownerId)) {
            throw new SecurityException("Only owner can share files");
        }

        List<FilePermission> rolesToChange = permissionRepository.findByUserIdAndFileId(userId, fileId);
        if (rolesToChange.isEmpty()) {
            throw new IllegalArgumentException("Permissions not found");
        }

        FilePermission permission = rolesToChange.get(0);
        permission.setRole(newRole);
        return toDto(permissionRepository.save(permission));
    }

    public List<FilePermissionDto> getFilesSharedWithMe(UUID userId) {
        return permissionRepository.findByUserId(userId).stream()
                .filter(p -> p.getTakenBackAt() == null)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public FilePermissionDto toDto(FilePermission permission) {
        File file = permission.getFile();

        User owner = userRepository.findById(permission.getOwnerId())
                .orElse(null);

        User user = userRepository.findById(permission.getUserId())
                .orElse(null);

        return new FilePermissionDto(
                permission.getId(),
                file.getId(),
                file.getOriginalName(),
                file.getSize(),
                file.getCategory(),
                file.getMimeType(),
                permission.getRole(),
                permission.getGrantedAt(),
                user != null ? new UserInfoDto(user.getId(), user.getName(), user.getEmail()) : null,
                owner != null ? new UserInfoDto(owner.getId(), owner.getName(), owner.getEmail()) : null,
                permission.getTakenBackAt() == null
        );
    }
}
