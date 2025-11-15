package com.MWS.model;

import java.util.UUID;

public record GetSimpleUserDto(
        UUID id,
        String name,
        String email,
        String phoneNumber
) {
}