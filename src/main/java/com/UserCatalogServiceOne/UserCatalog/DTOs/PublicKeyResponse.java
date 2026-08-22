package com.UserCatalogServiceOne.UserCatalog.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicKeyResponse {
    private String username;
    private String publicKey;   // null when this user has not published yet
    private String updatedAt;   // ISO-8601, or null
}
