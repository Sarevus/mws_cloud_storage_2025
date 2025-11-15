package com.MWS.model;

import java.util.List;
import java.util.UUID;

public record GetUserAndFilesDto(
        UUID id,
        String name,
        String email,
        String phoneNumber,
        List<FileDto> files
) {
}