package com.cloudstorage.dto.request;

public record PutUserPasswordDto(
        String oldPassword,
        String newPassword
) {}