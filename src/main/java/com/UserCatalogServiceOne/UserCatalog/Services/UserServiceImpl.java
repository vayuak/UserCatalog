package com.UserCatalogServiceOne.UserCatalog.Services;

import com.UserCatalogServiceOne.UserCatalog.DTOs.LoginRequest;
import com.UserCatalogServiceOne.UserCatalog.DTOs.UserRegistrationRequest;
import com.UserCatalogServiceOne.UserCatalog.GodMode.CryptoUtils;
import com.UserCatalogServiceOne.UserCatalog.Models.User;
import com.UserCatalogServiceOne.UserCatalog.NotificationServices.SmsService;
import com.UserCatalogServiceOne.UserCatalog.NotificationServices.EmailService;
import com.UserCatalogServiceOne.UserCatalog.Others.UserMapper;
import com.UserCatalogServiceOne.UserCatalog.Repositories.UserRepository;
import com.UserCatalogServiceOne.UserCatalog.Configurations.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserServiceInterface {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SmsService smsService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    private final Map<String, User> pendingUserCache = new ConcurrentHashMap<>();
    private final Map<String, String> otpCache = new ConcurrentHashMap<>();

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";

    @Override
    public void processRegistration(UserRegistrationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken.");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPremium(false);
        user.setGender(request.getGender());

        String rawIdentifier = request.getContactIdentifier().trim();
        // Generate Blind Index (Hash)
        String hashedId = CryptoUtils.hashIdentifier(rawIdentifier);

        if (rawIdentifier.matches(EMAIL_REGEX)) {
            user.setEmail(hashedId); // DB sees hash
        } else {
            String fullPhone = rawIdentifier.startsWith("+") ? rawIdentifier : request.getCountryCode() + rawIdentifier;
            user.setPhoneNumber(CryptoUtils.hashIdentifier(fullPhone)); // DB sees hash
        }

        String otp = String.format("%06d", new Random().nextInt(999999));
        pendingUserCache.put(rawIdentifier, user);
        otpCache.put(rawIdentifier, otp);

        // For testing/production independence, we send to the RAW identifier
        if (request.getPreferredContactMethod() == User.ContactPreference.EMAIL) {
            emailService.sendOtpEmail(rawIdentifier, otp);
        } else {
            String targetPhone = rawIdentifier.startsWith("+") ? rawIdentifier : request.getCountryCode() + rawIdentifier;
            smsService.sendOtpSms(targetPhone, otp);
        }

        log.info("Secure registration initiated. OTP sent to: {}", maskIdentifier(rawIdentifier));
        System.out.println(">>> REAL-TIME OTP DEBUG: " + otp);
    }

    @Override
    @Transactional
    public User verifyAndRegister(String identifier, String otp) {
        String cachedOtp = otpCache.get(identifier);
        if (cachedOtp != null && cachedOtp.equals(otp)) {
            User user = pendingUserCache.remove(identifier);
            otpCache.remove(identifier);
            if (user == null) throw new RuntimeException("Session expired.");
            return userRepository.save(user);
        }
        throw new RuntimeException("Invalid OTP.");
    }

    @Override
    public String authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return jwtUtils.generateToken(authentication);
    }

    @Override
    @Transactional
    public void initiatePasswordReset(String identifier) {
        String hashedId = CryptoUtils.hashIdentifier(identifier.trim());

        // Search using the Blind Index
        User user = userRepository.findByEmail(hashedId)
                .or(() -> userRepository.findByPhoneNumber(hashedId))
                .orElseThrow(() -> new RuntimeException("Identity not found."));

        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setResetToken(otp);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        // We send to the RAW identifier provided by the user
        if (identifier.contains("@")) {
            emailService.sendOtpEmail(identifier, otp);
        } else {
            smsService.sendOtpSms(identifier, otp);
        }
        log.info("Reset initiated for encrypted user ID: {}", user.getId());
    }

    @Override
    @Transactional
    public void resetPassword(String identifier, String otp, String newPassword) {
        User user = userRepository.findByResetToken(otp)
                .orElseThrow(() -> new RuntimeException("Invalid code."));

        String hashedId = CryptoUtils.hashIdentifier(identifier.trim());
        if (!hashedId.equals(user.getEmail()) && !hashedId.equals(user.getPhoneNumber())) {
            throw new RuntimeException("Identity mismatch.");
        }

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Code expired.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    private String maskIdentifier(String id) {
        if (id.contains("@")) return id.replaceAll("(^..)(.*)(@.*)", "$1****$3");
        return id.replaceAll(".(?=.{4})", "*");
    }
    @Transactional
    @Override
    public void follow(String followerUsername, String targetUsername) {
        User follower = userRepository.findByUsername(followerUsername)
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException("Target not found"));

        target.getFollowers().add(follower);
        userRepository.save(target);
    }

    @Override
    public String getGhostId(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.getId().toString())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override public Optional<User> findByEmail(String email) { return userRepository.findByEmail(email); }
    @Override public Optional<User> findByResetToken(String resetToken) { return userRepository.findByResetToken(resetToken); }
    @Override public boolean existsByUsername(String username) { return userRepository.existsByUsername(username); }
}