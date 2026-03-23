package com.MWS.dto;

import com.MWS.model.Roles;

import java.time.LocalDateTime;
import java.util.UUID;

public record FilePermissionDto(
        UUID id,
        UUID fileId,
        String fileName,
        long fileSize,
        String fileCategory,
        String mimeType,
        Roles role,
        LocalDateTime grantedAt,
        UserInfoDto user,
        UserInfoDto owner,
        boolean isActive
) {}