package com.MWS.model;

import java.lang.String;

import com.MWS.Validator.annotations.Email;
import com.MWS.Validator.annotations.NotNull;
import com.MWS.Validator.annotations.PhoneNumber;
import com.MWS.Validator.annotations.Size;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Data
@Entity
@Table(name = "users")

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    @NotNull(message = "Имя не может быть null")
    @Size(min = 2, max = 50, message = "Имя должно быть от 2 до 50 символов")
    private String name;

    @Column(name = "email", nullable = false)
    @Email(message = "Некорректный формат email")
    @NotNull(message = "Email не может быть null")
    private String email;

    @Column(name = "password", nullable = false)
    @NotNull(message = "Пароль не может быть null")
    @Size(min = 6, max = 50, message = "Пароль должен быть минимум 6 символов")
    private String password;

    @Column(name = "phone_number", nullable = false)
    @PhoneNumber(message = "Некорректный формат телефонного номера")
    private String phoneNumber;

    @Column(name = "storage_limit")
    @NotNull(message = "Лимит на размер хранилища не может быть null")
    private Long storageLimit;

    @Column(name = "subscription_id")
    private UUID subscriptionId;
}