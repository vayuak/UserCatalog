package com.UserCatalogServiceOne.UserCatalog.DTOs;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // Required for JSON serialization
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private String username;

    // This is the one your UserController is looking for!
    public JwtResponse(String token) {
        this.token = token;
    }

    // Optional: if you want to pass the username too later
    public JwtResponse(String token, String username) {
        this.token = token;
        this.username = username;
    }
}