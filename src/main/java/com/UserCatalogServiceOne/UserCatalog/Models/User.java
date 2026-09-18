package com.UserCatalogServiceOne.UserCatalog.Models;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "users", indexes = {
        // Explicit indexing for fast lookups during login/syncs
        @Index(name = "idx_u", columnList = "username"),
        @Index(name = "idx_i", columnList = "identity_hash")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "identity_hash", unique = true, nullable = false, length = 64)
    private String identityHash; // Irreversible SHA-256 Hash of the Operator's Email Address

    // Increased length to prevent AWS/Firebase signed URL crashes
    @Column(name = "profile_picture_url", length = 1024)
    private String profilePictureUrl;

    @Column(name = "is_premium", nullable = false)
    @Builder.Default
    private boolean isPremium = false;

    // CRITICAL FIX: To handle the bans coming from the Spherical Service
    @Column(name = "is_blocked", nullable = false)
    @Builder.Default
    private boolean isBlocked = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ====================================================================
    // E2EE KEY DIRECTORY
    // ====================================================================
    @Column(name = "public_key", length = 64)
    private String publicKey;

    @Column(name = "public_key_updated_at")
    private LocalDateTime publicKeyUpdatedAt;

    // ====================================================================
    // SECURITY TOKENS
    // ====================================================================
    @Column(name = "reset_token", unique = true, length = 128)
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    // Forces UTC Timezone so timestamps are consistent globally
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}