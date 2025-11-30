package com.MWS.security;

import java.util.Collection;

public class CustomUserDetails  {
    private Long userId;
    private String username;
    private String password;

    public CustomUserDetails(Long userId, String username, String password) {
        this.userId = userId;
        this.username = username;
        this.password = password;
    }

  public Long getUserId() {
    return userId;
    }
}
