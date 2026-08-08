package com.UserCatalogServiceOne.UserCatalog.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Login identifier is required")
    private String identifier; // 🟢 This provides getIdentifier() natively via Lombok!

    @NotBlank(message = "Password is required")
    private String password;   // 🟢 This provides getPassword() natively via Lombok!
}