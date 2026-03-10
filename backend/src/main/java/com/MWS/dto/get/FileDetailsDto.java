package com.MWS.dto.get;

import java.util.UUID;

public record FileDetailsDto(
        UUID id,
        UUID userId,
        String link,
        String category,
        String originalName,
        Long size,
        String mimeType,
        Boolean isPublic,
        String userName,
        String email,
        String phoneNumber
) {}