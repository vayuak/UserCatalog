package com.UserCatalogServiceOne.UserCatalog.Services;

import com.UserCatalogServiceOne.UserCatalog.DTOs.LoginRequest;
import com.UserCatalogServiceOne.UserCatalog.DTOs.UserRegistrationRequest;
import com.UserCatalogServiceOne.UserCatalog.DTOs.ProfileUpdateRequest;
import com.UserCatalogServiceOne.UserCatalog.Models.User;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;

public interface UserServiceInterface {
    void processRegistration(UserRegistrationRequest request);
    User verifyAndRegister(String username, String otp);
    String authenticateUser(LoginRequest loginRequest);
    void initiatePasswordReset(String identifier);
    void resetPassword(String identifier, String otpOrToken, String newPassword);

    boolean isUsernameAvailable(String username);
    List<String> generateAlternativeUsernames(String base);
    void updateProfile(String username, ProfileUpdateRequest request);


    String getGhostId(String username);
    Optional<User> findByIdentityHash(String identityHash);
    Optional<User> findIdentityHash(String h);
    Optional<User> findByResetToken(String resetToken);
    boolean existsByUsername(String username);
}