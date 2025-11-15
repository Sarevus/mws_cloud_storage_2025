package com.MWS.dto.create_update;

public record CreateUserDTO(
        String name,
        String email,
        String phoneNumber,
        String password
) {
}
