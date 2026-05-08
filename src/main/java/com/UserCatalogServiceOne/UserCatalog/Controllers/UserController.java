package com.UserCatalogServiceOne.UserCatalog.Controllers;

import com.UserCatalogServiceOne.UserCatalog.DTOs.JwtResponse;
import com.UserCatalogServiceOne.UserCatalog.DTOs.LoginRequest;
import com.UserCatalogServiceOne.UserCatalog.DTOs.UserRegistrationRequest;
import com.UserCatalogServiceOne.UserCatalog.Models.User;
import com.UserCatalogServiceOne.UserCatalog.Services.UserServiceInterface;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceInterface userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRegistrationRequest request) {
        userService.processRegistration(request);
        return ResponseEntity.ok("OTP has been sent. Please verify to complete registration.");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam String identifier, @RequestParam String otp) {
        try {
            User user = userService.verifyAndRegister(identifier, otp);
            return ResponseEntity.status(HttpStatus.CREATED).body("Registration successful for: " + user.getUsername());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            String jwt = userService.authenticateUser(loginRequest);
            return ResponseEntity.ok(new JwtResponse(jwt, loginRequest.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }


    @PostMapping("/reset-password")
    public ResponseEntity<String> handleResetPassword(
            @RequestParam String identifier,
            @RequestParam String otpOrToken,
            @RequestParam String newPassword) {

        userService.resetPassword(identifier, otpOrToken, newPassword);
        return ResponseEntity.ok("Password has been reset successfully.");
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String identifier) {
        userService.initiatePasswordReset(identifier);
        return ResponseEntity.ok("Reset code/link has been sent to your " +
                (identifier.contains("@") ? "email" : "phone") + ".");
    }

    private final StringRedisTemplate redisTemplate; // Inject Redis Template

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Store in Redis with a 24-hour Time To Live (TTL)
            redisTemplate.opsForValue().set(token, "blacklisted", Duration.ofHours(24));

            return ResponseEntity.ok("Successfully logged out.");
        }
        return ResponseEntity.badRequest().body("No token found.");
    }

}