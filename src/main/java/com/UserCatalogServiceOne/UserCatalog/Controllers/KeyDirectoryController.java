package com.UserCatalogServiceOne.UserCatalog.Controllers;

import com.UserCatalogServiceOne.UserCatalog.DTOs.PublicKeyRequest;
import com.UserCatalogServiceOne.UserCatalog.DTOs.PublicKeyResponse;
import com.UserCatalogServiceOne.UserCatalog.Models.User;
import com.UserCatalogServiceOne.UserCatalog.Repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Public-key directory for end-to-end encryption.
 *
 * Gateway routing: Mode maps /v1/auth/** to /api/users/** (StripPrefix=2 then
 * PrefixPath=/api/users), so these are reachable from the app as:
 *
 *   POST /v1/auth/keys            publish my own public key
 *   GET  /v1/auth/keys/{username} fetch one user's public key
 *   POST /v1/auth/keys/batch      fetch many at once
 *
 * All three require a valid JWT: UserCatalog's SecurityConfig ends with
 * anyRequest().authenticated() and these paths are not in the permitAll list.
 *
 * SECURITY MODEL AND ITS LIMIT
 * ----------------------------
 * This is a server-mediated directory, so the server is trusted to hand out
 * the correct key. A malicious or compromised server could substitute its own
 * key and read messages -- the classic key-substitution attack. Two mitigations
 * are in place:
 *
 *   1. A user can only ever write their OWN key. The username comes from the
 *      authenticated principal, never from the request body.
 *   2. Key CHANGES are logged loudly, and the client pins the first key it sees
 *      per contact (trust-on-first-use) and refuses to silently accept a
 *      different one later.
 *
 * The real fix for full protection is out-of-band verification: show both users
 * a safety-number fingerprint of the two public keys, as Signal and WhatsApp
 * do, and let them compare it in person. Worth adding once this works.
 */
@RestController
@RequestMapping("/api/users/keys")
@RequiredArgsConstructor
@Slf4j
public class KeyDirectoryController {

    private static final int MAX_BATCH = 100;

    private final UserRepository userRepository;

    /** Publish or rotate the caller's own public key. */
    @PostMapping
    public ResponseEntity<?> publishMyKey(@Valid @RequestBody PublicKeyRequest request,
                                          Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHENTICATED"));
        }

        String username = authentication.getName().trim();

        // Exact lookup. Never a substring match for an identity operation.
        Optional<User> found = userRepository.findByUsernameIgnoreCase(username);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "USER_NOT_FOUND"));
        }

        User user = found.get();
        String incoming = request.getPublicKey().trim();

        if (!isValidX25519Base64(incoming)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "INVALID_KEY",
                                 "message", "publicKey must be base64 of exactly 32 bytes."));
        }

        String existing = user.getPublicKey();

        if (existing != null && !existing.equals(incoming)) {
            // Not blocked: users legitimately reinstall and get a new device
            // key. But it is the signal a key-substitution attack would also
            // produce, so it must be visible in your logs and to the peer.
            log.warn("KEY ROTATION for user {}: public key replaced. Peers will see a changed fingerprint.", username);
        } else if (existing == null) {
            log.info("Public key published for the first time by {}", username);
        }

        user.setPublicKey(incoming);
        user.setPublicKeyUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "publicKey", incoming,
                "rotated", existing != null && !existing.equals(incoming)
        ));
    }

    /** Fetch one user's public key. */
    @GetMapping("/{username}")
    public ResponseEntity<?> getKey(@PathVariable String username) {
        Optional<User> found = userRepository.findByUsernameIgnoreCase(username.trim());

        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "USER_NOT_FOUND"));
        }

        User user = found.get();

        if (user.getPublicKey() == null) {
            // 409 rather than 404: the user exists but cannot receive encrypted
            // messages yet. The client shows "waiting for this user to update
            // their app" instead of "no such user".
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "NO_KEY_PUBLISHED",
                    "username", user.getUsername(),
                    "message", "This user has not published an encryption key yet."
            ));
        }

        return ResponseEntity.ok(new PublicKeyResponse(
                user.getUsername(),
                user.getPublicKey(),
                user.getPublicKeyUpdatedAt() != null
                        ? user.getPublicKeyUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null
        ));
    }

    /** Batch fetch, so an inbox resolves every peer key in one round trip. */
    @PostMapping("/batch")
    public ResponseEntity<?> getKeys(@RequestBody Map<String, List<String>> body) {
        List<String> requested = body.get("usernames");

        if (requested == null || requested.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "usernames is required"));
        }
        if (requested.size() > MAX_BATCH) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "TOO_MANY", "max", MAX_BATCH));
        }

        Set<String> normalized = requested.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        List<User> users = userRepository.findByUsernameInIgnoreCase(normalized);

        List<PublicKeyResponse> keys = users.stream()
                .map(u -> new PublicKeyResponse(
                        u.getUsername(),
                        u.getPublicKey(),
                        u.getPublicKeyUpdatedAt() != null
                                ? u.getPublicKeyUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                : null))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("keys", keys));
    }

    /** Base64 decoding to exactly 32 bytes is the only shape X25519 accepts. */
    private boolean isValidX25519Base64(String value) {
        try {
            return Base64.getDecoder().decode(value).length == 32;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
