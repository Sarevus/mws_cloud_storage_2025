package com.MWS.dto;

import com.MWS.model.Roles;

import java.util.UUID;

public record ShareRequestDto(
        UUID fileId,
        String userEmail,
        Roles role
) {
}
