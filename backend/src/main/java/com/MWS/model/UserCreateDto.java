package com.MWS.model;

public record UserCreateDto(
        String name,
        String email,
        String phoneNumber
) {}