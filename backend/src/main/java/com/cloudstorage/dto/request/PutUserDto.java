package com.cloudstorage.dto.request;

public record PutUserDto(
        String name,
        String email,
        String phoneNumber
) {}