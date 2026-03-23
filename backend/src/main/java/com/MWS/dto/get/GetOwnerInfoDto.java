package com.MWS.dto.get;

import java.util.UUID;

public record GetOwnerInfoDto(
        UUID id,
        String email,
        String name
) {
}
