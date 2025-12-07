package com.cloudstorage.model;

import java.util.UUID;


/**
 * моделька юзера, дженерик
 */
public class User {
    private UUID id;
    private String name;
    private String email;
    private String phoneNumber;
    private String password;

    public User() {}

    public User(String name, String email, String phoneNumber, String password) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPassword() { return password; }


    public void setId(UUID id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setPassword(String password) { this.password = password; }

    @Override
    public String toString() {
        return String.format("User{id=%s, name='%s', email='%s'}", id, name, email);
    }
}
