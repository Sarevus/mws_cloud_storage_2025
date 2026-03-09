package com.MWS.dto;

import java.util.UUID;

public record UserInfoDto(
        UUID id,
        String name,
        String email
) {}