package com.MWS.dto.create_update;

public record PutUserPasswordDto(
        String oldPassword,
        String newPassword
) {
}
