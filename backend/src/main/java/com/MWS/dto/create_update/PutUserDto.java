package com.MWS.dto.create_update;

public record PutUserDto(
        String name,
        String email,
        String phoneNumber,
        String password
) {
}
