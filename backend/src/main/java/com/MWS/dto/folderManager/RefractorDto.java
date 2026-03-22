package com.MWS.dto.folderManager;

import java.util.List;
import java.util.UUID;

public record RefractorDto(
        List<UUID> fileIds,
        UUID sourceFolderId,
        UUID targetFolderId
) {
}
