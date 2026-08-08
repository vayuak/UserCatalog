package com.UserCatalogServiceOne.UserCatalog.Configurations;

import com.UserCatalogServiceOne.UserCatalog.Models.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtUtils {

    @Value("${ghost.shield.jwt-secret:SuperSecurePermanentSecretKeyThatIsAtLeast64BytesLongForSecurityGuarantees}")
    private String jwtSecret;

    @Value("${ghost.shield.jwt-expiration:86400000}")
    private int jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            log.error("❌ GHOST SHIELD CRITICAL: Secret key length insufficient to guarantee cryptographic security.");
        } else {
            log.info("✅ GHOST SHIELD ACTIVE: High-Entropy cryptographic key signature loop ready for execution.");
        }
    }

    public String generateToken(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        String username;
        Long userId = null;
        boolean isPremium = false;

        if (principal instanceof CustomUserDetails customUser) {
            username = customUser.getUsername();
            userId = customUser.getId();
            isPremium = customUser.isPremium(); // Extracts the user profile state flag directly
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails springUser) {
            username = springUser.getUsername();
        } else {
            username = principal.toString();
        }

        // Bake permission payload assertions directly into the tamper-proof JWT capsule
        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .claim("username", username)
                .claim("isPremium", isPremium);

        if (userId != null) {
            builder.claim("id", userId);
        }

        return builder
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Cryptographic signature evaluation breakdown: {}", e.getMessage());
        }
        return false;
    }
}