package com.cloudstorage.dto.request;

public record CreateUserDTO(
        String name,
        String email,
        String phoneNumber,
        String password
) {}