package com.MWS.model;

import java.lang.String;

import com.MWS.Validator.annotations.Email;
import com.MWS.Validator.annotations.NotNull;
import com.MWS.Validator.annotations.PhoneNumber;
import com.MWS.Validator.annotations.Size;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {
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

    @Column(name = "phoneNumber", nullable = false)
    @PhoneNumber(message = "Некорректный формат телефонного номера")
    private String phoneNumber;

    public UserEntity() {
    }

    public UserEntity(UUID id, String name, String email, String phoneNumber, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return String.format("User{ID: %s, name=%s}", id, name);
    }

}

