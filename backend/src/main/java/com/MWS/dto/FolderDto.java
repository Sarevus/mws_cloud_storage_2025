package com.MWS.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FolderDto(
        UUID id,
        String name,
        String path,
        UUID parentId,
        UUID ownerId,
        long folderSize,
        int filesQuantity,
        int foldersQuantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}