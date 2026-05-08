package com.UserCatalogServiceOne.UserCatalog.DTOs;

import com.UserCatalogServiceOne.UserCatalog.Models.User;
import com.UserCatalogServiceOne.UserCatalog.Models.User.ContactPreference;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UserRegistrationRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8)
    private String password;

    @NotBlank(message = "Contact identifier (Email or Phone) is required")
    private String contactIdentifier;

    @NotBlank(message = "Country code is required")
    private String countryCode;

    @NotBlank(message = "Country name is required")
    private String countryName;

    @NotNull(message = "Choose Email or Phone for OTP")
    private ContactPreference preferredContactMethod;

    @NotNull(message = "Date of Birth is required")
    private LocalDate dateOfBirth;

    private String fullName;
    private User.Gender gender;
}