package com.UserCatalogServiceOne.UserCatalog.Services;

import com.UserCatalogServiceOne.UserCatalog.DTOs.LoginRequest;
import com.UserCatalogServiceOne.UserCatalog.DTOs.UserRegistrationRequest;
import com.UserCatalogServiceOne.UserCatalog.Models.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserServiceInterface {
    void processRegistration(UserRegistrationRequest request);
    User verifyAndRegister(String identifier, String otp);
    String authenticateUser(LoginRequest loginRequest);
    void initiatePasswordReset(String identifier);
    void resetPassword(String identifier, String otpOrToken, String newPassword);

    @Transactional
    void follow(String followerUsername, String targetUsername);

    String getGhostId(String username);

    // These link to the Repo
    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String resetToken);
    boolean existsByUsername(String username);
}