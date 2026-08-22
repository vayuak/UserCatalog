package com.UserCatalogServiceOne.UserCatalog.Models;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
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

    @Column(name = "profile_picture_url", nullable = true)
    private String profilePictureUrl;

    @Column(name = "is_premium", nullable = false)
    @Builder.Default
    private boolean isPremium = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ====================================================================
    // E2EE KEY DIRECTORY
    // ====================================================================
    // Base64 X25519 (Curve25519) PUBLIC key for this user's device.
    // A public key is public by definition: publishing it is safe, and doing so
    // is what makes end-to-end encryption possible. The matching PRIVATE key
    // never leaves the device and must never be sent here.
    //
    // Nullable so existing users keep working until their app publishes a key.
    @Column(name = "public_key", length = 64)
    private String publicKey;

    @Column(name = "public_key_updated_at")
    private LocalDateTime publicKeyUpdatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    private String resetToken;
    private LocalDateTime resetTokenExpiry;
}
