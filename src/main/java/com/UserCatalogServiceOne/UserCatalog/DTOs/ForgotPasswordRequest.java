package com.UserCatalogServiceOne.UserCatalog.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ForgotPasswordRequest {
    @NotBlank(message = "Identifier is required")
    private String identifier;
}