package com.MWS.dto.folder;

import java.util.UUID;

public record MoveFolderRequest(
        UUID newParentId
) {
}
