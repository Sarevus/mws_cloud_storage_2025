package com.MWS.dto.create_update;

import com.MWS.Validator.annotations.*;

public record PutUserDto(
        @NotNull(message = "Имя не может быть пустым")
        @Size(min = 2, max = 50, message = "Имя должно быть от 2 до 50 символов")
        String name,

        @NotNull(message = "Email не может быть пустым")
        @Email(message = "Некорректный формат email")
        String email,

        @NotNull(message = "Телефон обязателен")
        @PhoneNumber(message = "Некорректный номер телефона")
        String phoneNumber
) {
}
