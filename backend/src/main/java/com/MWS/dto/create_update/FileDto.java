package com.MWS.dto.create_update;

import java.util.UUID;

public record FileDto(
        UUID id,
        String name,
        long size,
        String mimeType,
        String extension,
        String category,
        UUID userId,
        String link
) {}
