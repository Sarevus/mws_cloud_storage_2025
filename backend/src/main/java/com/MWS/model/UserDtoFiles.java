package com.MWS.model;

import java.util.List;
import java.util.UUID;

public record UserDtoFiles(
        UUID id,
        String name,
        String email,
        String phoneNumber,
        List<FileDto> files
) {}
