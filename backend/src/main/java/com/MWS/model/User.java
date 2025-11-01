package com.MWS.model;

import java.lang.String;
import com.MWS.storage.sql.annotations.Email;
import com.MWS.storage.sql.annotations.NotNull;
import com.MWS.storage.sql.annotations.PhoneNumber;
import com.MWS.storage.sql.annotations.Size;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private final UUID ID;

    @NotNull(message = "Имя не может быть null")
    @Size(min = 2, max = 50, message = "Имя должно быть от 2 до 50 символов")
    private String name;

    @Email(message = "Некорректный формат email")
    @NotNull(message = "Email не может быть null")
    private String email;

    @NotNull(message = "Пароль не может быть null")
    @Size(min = 6, max = 50, message = "Пароль должен быть минимум 6 символов")
    private String password;

    @PhoneNumber(message = "Некорректный формат телефонного номера")
    private String phoneNumber;

    public User(UUID ID, String name, String email, String phoneNumber, String password) {
        this.ID = ID;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
    }

    public UUID getID() {
        return ID;
    }

    public String getName () {
        return name;
    }

    public String getEmail () {
        return email;
    }

    public String getPhoneNumber () {
        return phoneNumber;
    }

    public String getPassword () {
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

    public void setPassword (String password) {
        this.password = password;
    }

    @Override
    public String toString(){
        return String.format("User{ID: %s, name=%s}", ID, name);
    }
}

