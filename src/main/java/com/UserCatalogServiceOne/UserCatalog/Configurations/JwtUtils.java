package com.UserCatalogServiceOne.UserCatalog.Configurations;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    // Must be at least 32 characters
    private final String jwtSecret = "your_very_long_secret_key_that_is_secure_and_unique_12345";
    private final int jwtExpirationMs = 86400000; // 24 hours

    // Helper to get the Signing Key once
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .subject(userPrincipal.getUsername()) // Old: setSubject
                .issuedAt(new Date()) // Old: setIssuedAt
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs)) // Old: setExpiration
                .signWith(getSigningKey()) // SignatureAlgorithm is now inferred
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser() // Old: parserBuilder()
                .verifyWith(getSigningKey()) // Old: setSigningKey()
                .build()
                .parseSignedClaims(token) // Old: parseClaimsJws()
                .getPayload() // Old: getBody()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("JWT token is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("JWT token is unsupported: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string is empty: " + e.getMessage());
        }
        return false;
    }
}