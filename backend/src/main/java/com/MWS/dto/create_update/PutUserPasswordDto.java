package com.MWS.dto.create_update;

import com.MWS.Validator.annotations.NotNull;
import com.MWS.Validator.annotations.Size;

public record PutUserPasswordDto(
        @NotNull(message = "Пароль обязателен")
        @Size(min = 6, max = 100, message = "Пароль должен быть от 6 до 100 символов")
        String oldPassword,

        @NotNull(message = "Пароль обязателен")
        @Size(min = 6, max = 100, message = "Пароль должен быть от 6 до 100 символов")
        String newPassword
) {
}
