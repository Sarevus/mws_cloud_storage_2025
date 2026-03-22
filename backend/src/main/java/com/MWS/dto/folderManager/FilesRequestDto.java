package com.MWS.dto.folderManager;

import java.util.List;
import java.util.UUID;

public record FilesRequestDto(
        List<UUID> fileIds,
        String addedBy,
        String folderName
) {
}
