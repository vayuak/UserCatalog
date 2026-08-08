package com.UserCatalogServiceOne.UserCatalog.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Username cannot be empty")
    private String username;

    @NotBlank(message = "OTP cannot be empty")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    private String otp;
}