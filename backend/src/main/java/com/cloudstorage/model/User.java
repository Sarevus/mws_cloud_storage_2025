package com.cloudstorage.model;

import java.util.UUID;


/**
 * моделька юзера, дженерик
 */
public class User {
    private UUID id;
    private String name;
    private String email;
    private String password;

    public User(String name, String email, String password){
        this.id = UUID.randomUUID();
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }

    public void setId(UUID id) { this.id = id; }
    public void setPassword(String password) { this.password = password; }
}
