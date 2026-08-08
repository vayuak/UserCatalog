package com.UserCatalogServiceOne.UserCatalog.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Identifier is required")
    private String identifier;
}