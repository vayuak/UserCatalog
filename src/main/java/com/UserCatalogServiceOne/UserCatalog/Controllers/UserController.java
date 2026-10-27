package com.UserCatalogServiceOne.UserCatalog.Controllers;

import com.UserCatalogServiceOne.UserCatalog.DTOs.*;
import com.UserCatalogServiceOne.UserCatalog.Models.User;
import com.UserCatalogServiceOne.UserCatalog.Repositories.UserRepository;
import com.UserCatalogServiceOne.UserCatalog.Services.UserServiceInterface;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserServiceInterface userService;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegistrationRequest request) {
        userService.processRegistration(request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Handshake challenge code generated successfully.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsernameAvailability(@RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        boolean available = userService.isUsernameAvailable(username);

        response.put("available", available);
        if (!available) {
            List<String> alternatives = userService.generateAlternativeUsernames(username);
            response.put("suggestions", alternatives);
        }
        return ResponseEntity.ok(response);
    }
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("Verify-OTP request for @{}", request.getUsername());

        User verifiedUser = userService.verifyAndRegister(request.getUsername(), request.getOtp());
        return ResponseEntity.ok(Map.of("message", "User verified and registered successfully."));
    }
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        String identifier = loginRequest.getIdentifier().trim();
        if (identifier.contains("@") && !identifier.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Malformed credential format: Provide a valid email address structure."));
        }

        try {
            String jwt = userService.authenticateUser(loginRequest);
            return ResponseEntity.ok(new JwtResponse(jwt));
        } catch (org.springframework.security.core.AuthenticationException e) {
            // 🟢 CHANGED: Using 400 Bad Request instead of 401 to bypass the frontend "Session Expired" interceptor.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Incorrect email or password. Please try again or register."));
        } catch (Exception e) {
            log.error("Login failure trace: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Authentication failed."));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateRequest request, Authentication authentication) {
        userService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Profile sync completed."));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                stringRedisTemplate.opsForValue().set(token, "blacklisted", Duration.ofHours(24));
            } catch (Exception e) {
                // Mock fallback intercepts logs silently when offline
            }
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Successfully logged out."));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "No token found."));
    }

    /** Internal only. Returns a SAFE view - never the entity (it holds the password hash and the reset token). */
    @GetMapping("/internal/{id}")
    public ResponseEntity<?> getInternalUser(@PathVariable Long id) {
        return userRepository.findById(id).map(u -> ResponseEntity.ok((Object) safeView(u))).orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> safeView(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("username", u.getUsername());
        m.put("profilePictureUrl", u.getProfilePictureUrl());
        return m;
    }

    // 🟢 UPDATED: Now uses strict ForgotPasswordRequest DTO
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String identifier = request.getIdentifier();

        if (identifier.contains("@") && !identifier.trim().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Malformed target format: Provide a valid email address structure."));
        }

        userService.initiatePasswordReset(identifier);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Recovery OTP dispatched."));
    }

    // 🟢 UPDATED: Now uses strict ResetPasswordRequest DTO
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getIdentifier(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Password securely reset. Please login."));
    }

    /**
     * SECURITY FIX: this used to return the raw User entities. The social service forwarded them to any logged-in user,
     * including password hashes, FCM tokens and the password-reset token -> full account takeover of any username.
     * Now: only username + avatar, exact matches first, max 20.
     */
    @GetMapping("/internal/search-owners")
    public ResponseEntity<List<Map<String, Object>>> searchUsersByHandle(@RequestParam String username) {
        final String q = username.trim().toLowerCase();
        if (q.isEmpty()) return ResponseEntity.ok(List.of());
        List<User> users = new java.util.ArrayList<>(userRepository.findByUsernameContainingIgnoreCase(q));
        users.sort(Comparator.comparing((User u) -> !u.getUsername().equalsIgnoreCase(q)).thenComparing(User::getUsername));
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (User u : users) { if (out.size() >= 20) break; out.add(safeView(u)); }
        return ResponseEntity.ok(out);
    }

    /** Internal only (called by the social service). Permanently deletes the account row. */
    @DeleteMapping("/internal/user/{username}")
    public ResponseEntity<?> deleteInternalUser(@PathVariable String username) {
        final String name = username.trim().toLowerCase();
        userRepository.findByUsernameIgnoreCase(name).ifPresent(userRepository::delete);
        try { stringRedisTemplate.delete("banned:" + name); } catch (Exception ignored) { }
        log.warn("Account deleted at the user's request: @{}", name);
        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserProfile(Authentication authentication) {
        String username = authentication.getName();
        // Assuming your repository has a findByUsername method. Adjust slightly if it returns an Optional.
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("username", user.getUsername());
        profileData.put("profilePictureUrl", user.getProfilePictureUrl());
        profileData.put("isPremium", user.isPremium());

        return ResponseEntity.ok(profileData);
    }


    @PutMapping("/internal/profile/update-avatar")
    public ResponseEntity<?> updateInternalAvatar(
            @RequestParam String username,
            @RequestBody Map<String, String> payload) {

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username parameter is required."));
        }

        String avatarUrl = payload != null ? payload.get("profilePictureUrl") : null;
        if (avatarUrl == null) {
            // Fallback check if BLOB passed 'url' or 'avatar' key instead
            avatarUrl = payload.getOrDefault("url", payload.get("avatar"));
        }

        try {
            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setProfilePictureUrl(avatarUrl);

            userService.updateProfile(username.trim(), request);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Avatar updated for " + username));
        } catch (Exception e) {
            log.error("Failed to sync avatar for username: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cross-node avatar update failed: " + e.getMessage()));
        }
    }
    /**
     * INTERNAL API: Called by Spherical Service when a user reaches 3 abuse strikes.
     * Protected by ShieldHandshakeFilter.
     */
    @PostMapping("/internal/block/{username}")
    public ResponseEntity<?> blockInternalUser(@PathVariable String username) {
        log.warn("🚨 [INTERNAL ROUTER] Received cross-node block request for @{}", username);
        userService.blockUser(username);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "User banned globally."));
    }
    /**
     * INTERNAL API: Called by Social Service upon successful Stripe payment.
     * Protected by ShieldHandshakeFilter.
     */
    @PostMapping("/internal/premium/{userId}")
    public ResponseEntity<?> upgradeInternalUserToPremium(@PathVariable Long userId) {
        userService.upgradeUserToPremium(userId);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "User upgraded to premium globally."));
    }
    @PutMapping("/profile/device-token")
    public ResponseEntity<?> updateDeviceToken(@RequestBody Map<String, String> payload, Authentication authentication) {
        String token = payload.get("fcmToken");
        if (token != null && !token.isBlank()) {
            // Add a setFcmToken() method to your User entity and update it here
            User user = userRepository.findByUsernameIgnoreCase(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setFcmToken(token);
            userRepository.save(user);
        }
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Device token synced."));
    }
    @GetMapping("/internal/token/{username}")
    public ResponseEntity<String> getFcmToken(@PathVariable String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .map(User::getFcmToken)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok("")); // Return empty string if not found
    }
}
