package com.MWS.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
public class User {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String password;

    private String phoneNumber;

    private String email;


}
