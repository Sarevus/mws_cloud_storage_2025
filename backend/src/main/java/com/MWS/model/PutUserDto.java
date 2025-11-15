package com.MWS.model;

public record PutUserDto(
        String name,
        String email,
        String phoneNumber
) {
}
