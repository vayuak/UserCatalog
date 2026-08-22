package com.UserCatalogServiceOne.UserCatalog.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PublicKeyRequest {

    // Base64 of a 32-byte X25519 public key: 44 chars with padding.
    @NotBlank(message = "publicKey is required")
    @Size(min = 43, max = 64, message = "publicKey is not a valid base64 X25519 key")
    private String publicKey;
}
