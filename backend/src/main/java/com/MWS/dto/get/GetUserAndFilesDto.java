package com.MWS.dto.get;

import com.MWS.dto.create_update.FileDto;

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