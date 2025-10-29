package com.MWS.storage.sql;

import com.MWS.storage.sql.annotations.Email;
import com.MWS.storage.sql.annotations.NotNull;
import com.MWS.storage.sql.annotations.PhoneNumber;
import com.MWS.storage.sql.annotations.Size;

public class User {
    @NotNull(message = "ID не может быть null")
    @Size(min = 2, max = 50, message = "Имя должно быть от 2 до 50 символов")
    private final long ID;

    @NotNull(message = "Имя не может быть null")
    @Size(min = 2, max = 50, message = "Имя должно быть от 2 до 50 символов")
    private String name;

    @Email(message = "Некорректный формат email")
    @NotNull(message = "Email не может быть null")
    private String email;

    @PhoneNumber(message = "Некорректный формат телефонного номера")
    private String phoneNumber;

    public User(long ID, String name, String email, String phoneNumber) {
        this.ID = ID;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public long getID() {
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

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String toString(){
        return String.format("User{ID: %d, name='%s'}", ID, name);
    }
}

