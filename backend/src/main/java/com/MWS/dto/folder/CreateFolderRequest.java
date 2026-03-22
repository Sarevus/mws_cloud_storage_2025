package com.MWS.dto.folder;

import java.util.UUID;

public record CreateFolderRequest(
        String folderName,
        UUID parentId
) {
}
