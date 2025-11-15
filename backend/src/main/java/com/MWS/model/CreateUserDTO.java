package com.MWS.model;

public record CreateUserDTO(
        String name,
        String email,
        String phoneNumber
) {}
