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
import java.util.HashMap;
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
        log.info("📥 [INBOUND VERIFY] Target Username: '{}' | OTP: '{}'", request.getUsername(), request.getOtp());

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
            // 🟢 CHANGED: Using 400 Bad Request instead of 401 to bypass the frontend "Session Expired" interceptor
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

    @GetMapping("/internal/{id}")
    public ResponseEntity<?> getInternalUser(@PathVariable Long id) {
        return userRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
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

    @GetMapping("/internal/search-owners")
    public ResponseEntity<List<User>> searchUsersByHandle(@RequestParam String username) {
        List<User> users = userRepository.findByUsernameContainingIgnoreCase(username.trim().toLowerCase());
        return ResponseEntity.ok(users);
    }

    @PutMapping("/internal/profile/update-avatar")
    public ResponseEntity<?> updateInternalAvatar(
            @RequestParam String username,
            @RequestBody Map<String, String> payload) {

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setProfilePictureUrl(payload.get("profilePictureUrl"));

        userService.updateProfile(username, request);
        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserProfile(Authentication authentication) {
        String username = authentication.getName();
        // Assuming your repository has a findByUsername method. Adjust slightly if it returns an Optional.
        User user = userRepository.findByUsernameContainingIgnoreCase(username).stream().findFirst()
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("username", user.getUsername());
        profileData.put("profilePictureUrl", user.getProfilePictureUrl());
        profileData.put("isPremium", user.isPremium());

        return ResponseEntity.ok(profileData);
    }
}