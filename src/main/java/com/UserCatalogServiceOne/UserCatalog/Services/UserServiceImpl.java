package com.UserCatalogServiceOne.UserCatalog.Services;

import com.UserCatalogServiceOne.UserCatalog.DTOs.LoginRequest;
import com.UserCatalogServiceOne.UserCatalog.DTOs.ProfileUpdateRequest;
import com.UserCatalogServiceOne.UserCatalog.DTOs.UserRegistrationRequest;
import com.UserCatalogServiceOne.UserCatalog.ExceptionsHandlers.ClientValidationException;
import com.UserCatalogServiceOne.UserCatalog.Models.User;
import com.UserCatalogServiceOne.UserCatalog.NotificationServices.EmailService;
import com.UserCatalogServiceOne.UserCatalog.Repositories.UserRepository;
import com.UserCatalogServiceOne.UserCatalog.Configurations.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserServiceInterface {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
    private static final String OTP_ATTEMPT_PREFIX = "otp:attempts:";
    private static final String STAGE_USER_PREFIX = "u:stage:";
    private static final String STAGE_OTP_PREFIX = "u:otp:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // 🟢 FIX 1: Preserves in-memory Redis state across server restarts
    @PostConstruct
    public void initRedisCache() {
        log.info("🚀 [REDIS CACHE] Engine active. Preserving existing sessions and OTP states.");
    }

    private void enforcePasswordEntropy(String plainPassword) {
        if (plainPassword == null || plainPassword.length() < 8) {
            throw new ClientValidationException("Password complexity breach: String must measure 8+ characters.");
        }
        if (!plainPassword.matches(".*[A-Z].*")) {
            throw new ClientValidationException("Password complexity breach: Upper case signature element missing.");
        }
        if (!plainPassword.matches(".*[0-9].*")) {
            throw new ClientValidationException("Password complexity breach: Numeric parameter entry absent.");
        }
        if (!plainPassword.matches(".*[!@#$%^&*(),.?\":{}|<>_].*")) {
            throw new ClientValidationException("Password complexity breach: Special symbol parameter vacant.");
        }
    }

    // Native SHA-256 Hashing
    private String hashIdentifier(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Critical security failure: SHA-256 algorithm missing.", e);
        }
    }

    private String normalizeAndHash(String genericInput, char[] methodOut) {
        String input = genericInput.trim();
        if (input.matches(EMAIL_REGEX)) {
            if (methodOut != null && methodOut.length > 0) methodOut[0] = 'E';
            return hashIdentifier(input.toLowerCase());
        } else {
            if (methodOut != null && methodOut.length > 0) methodOut[0] = 'U';
            return input.toLowerCase();
        }
    }

    private String generateSixDigitOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(900000) + 100000);
    }

    @Override
    public void processRegistration(UserRegistrationRequest request) {
        if (request == null || request.getUsername() == null || request.getContactIdentifier() == null) {
            throw new ClientValidationException("Invalid registration payload.");
        }

        String cleanUsername = request.getUsername().trim().toLowerCase();
        String rawEmail = request.getContactIdentifier().trim();
        String plainPassword = request.getPassword();

        if (!rawEmail.matches(EMAIL_REGEX)) {
            throw new ClientValidationException("Mistake detected: Provide a valid email address structure (e.g., user@domain.com).");
        }

        enforcePasswordEntropy(plainPassword);

        if (userRepository.existsByUsername(cleanUsername)) {
            throw new ClientValidationException("Identity signature already bound to infrastructure.");
        }

        char[] methodContainer = new char[]{'E'};
        String targetHash = normalizeAndHash(rawEmail, methodContainer);

        if (userRepository.findByIdentityHash(targetHash).isPresent()) {
            throw new ClientValidationException("Contact method already registered to platform footprint.");
        }

        redisTemplate.delete(STAGE_USER_PREFIX + cleanUsername);
        stringRedisTemplate.delete(STAGE_OTP_PREFIX + cleanUsername);
        stringRedisTemplate.delete(OTP_ATTEMPT_PREFIX + cleanUsername);

        User user = new User();
        user.setUsername(cleanUsername);
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setIdentityHash(targetHash);
        user.setPremium(false);

        // 🟢 FIX 3: Map Cryptographic Public Key if provided
        if (request.getPublicKey() != null && !request.getPublicKey().trim().isEmpty()) {
            user.setPublicKey(request.getPublicKey().trim());
            user.setPublicKeyUpdatedAt(LocalDateTime.now());
        }

        String otp = generateSixDigitOtp();

        redisTemplate.opsForValue().set(STAGE_USER_PREFIX + cleanUsername, user, Duration.ofMinutes(10));
        stringRedisTemplate.opsForValue().set(STAGE_OTP_PREFIX + cleanUsername, otp, Duration.ofMinutes(10));

        log.info("🔑 [REGISTRATION OTP] Generated OTP for @{}: {}", cleanUsername, otp);

        // 🟢 FIX 2: Graceful Exception Handling & Rollback for Dispatch Failures
        try {
            emailService.sendOtpEmail(rawEmail, otp);
            log.info("📡 [PRODUCTION HARDENED] Outbound mail engine active for '{}'. Redis cluster keys synchronized.", cleanUsername);
        } catch (Exception e) {
            // Rollback OTP state from Redis if email sending fails to prevent deadlocks
            redisTemplate.delete(STAGE_USER_PREFIX + cleanUsername);
            stringRedisTemplate.delete(STAGE_OTP_PREFIX + cleanUsername);
            log.error("⚠️ [DISPATCH FAILURE] Could not deliver OTP to {}: {}", rawEmail, e.getMessage());
            throw new IllegalStateException("Failed to deliver verification code. Please check your email configuration.");
        }
    }

    @Override
    public User verifyAndRegister(String username, String otp) {
        if (username == null || otp == null) {
            throw new ClientValidationException("Username and verification code must be provided.");
        }

        String cleanUser = username.trim().toLowerCase();
        String otpCacheKey = STAGE_OTP_PREFIX + cleanUser;
        String userCacheKey = STAGE_USER_PREFIX + cleanUser;

        String cachedOtp = stringRedisTemplate.opsForValue().get(otpCacheKey);

        if (cachedOtp == null) {
            throw new ClientValidationException("No active registration handshake found or code expired.");
        }

        String cleanCached = cachedOtp.replaceAll("[^0-9]", "");
        String cleanInput = otp.replaceAll("[^0-9]", "");

        log.info("🔍 OTP Check -> Redis Raw: '{}', Cleaned: '{}' | Input Raw: '{}', Cleaned: '{}'",
                cachedOtp, cleanCached, otp, cleanInput);

        if (cleanCached.equals(cleanInput) && !cleanCached.isEmpty()) {
            Object rawUserObj = redisTemplate.opsForValue().get(userCacheKey);
            User user = null;

            if (rawUserObj instanceof User) {
                user = (User) rawUserObj;
            } else if (rawUserObj != null) {
                user = objectMapper.convertValue(rawUserObj, User.class);
            }

            stringRedisTemplate.delete(otpCacheKey);
            redisTemplate.delete(userCacheKey);
            stringRedisTemplate.delete(OTP_ATTEMPT_PREFIX + cleanUser);

            if (user == null) {
                throw new ClientValidationException("Staging window lost. Re-initiate onboarding workflow.");
            }

            log.info("🌟 [HANDSHAKE SUCCESS] Persistent profile row committed to DB for: {}", user.getUsername());
            return userRepository.save(user);
        }

        handleFailedOtpAttempt(cleanUser, () -> {
            stringRedisTemplate.delete(otpCacheKey);
            redisTemplate.delete(userCacheKey);
        });

        return null;
    }

    private void handleFailedOtpAttempt(String cacheKey, Runnable nukeAction) {
        String counterKey = OTP_ATTEMPT_PREFIX + cacheKey;

        Long attempts = stringRedisTemplate.opsForValue().increment(counterKey);
        long currentAttempts = (attempts != null && attempts > 0) ? attempts : 1;

        if (currentAttempts == 1) {
            stringRedisTemplate.expire(counterKey, Duration.ofMinutes(5));
        }

        if (currentAttempts >= 3) {
            stringRedisTemplate.delete(counterKey);
            nukeAction.run();
            throw new ClientValidationException("Too many invalid attempts. This verification code has expired. Request a new one.");
        }

        long remainingLeft = 3 - currentAttempts;
        throw new ClientValidationException("Invalid verification code. You have " + remainingLeft + " attempt(s) remaining before it expires permanently.");
    }

    @Override
    public boolean isUsernameAvailable(String username) {
        if (username == null || username.trim().length() < 3) return false;
        String cleanName = username.trim().toLowerCase();

        boolean inDb = userRepository.existsByUsername(cleanName);
        boolean inCache = Boolean.TRUE.equals(redisTemplate.hasKey(STAGE_USER_PREFIX + cleanName));

        return !inDb && !inCache;
    }

    @Override
    public List<String> generateAlternativeUsernames(String base) {
        List<String> variants = new ArrayList<>();
        if (base == null || base.trim().isEmpty()) {
            return variants;
        }

        String cleanBase = base.trim().toLowerCase().replaceAll("\\s+", "");
        while (variants.size() < 3) {
            String candidate = cleanBase + SECURE_RANDOM.nextInt(999);
            if (!userRepository.existsByUsername(candidate) &&
                    !Boolean.TRUE.equals(redisTemplate.hasKey(STAGE_USER_PREFIX + candidate))) {
                variants.add(candidate);
            }
        }
        return variants;
    }

    @Override
    public String authenticateUser(LoginRequest loginRequest) {
        if (loginRequest == null || loginRequest.getIdentifier() == null) {
            throw new ClientValidationException("Invalid login payload.");
        }

        char[] resolvedType = new char[]{'U'};
        String inputId = loginRequest.getIdentifier().trim();
        String targetResult = normalizeAndHash(inputId, resolvedType);

        String usernameToAuthenticate = targetResult;

        if (resolvedType[0] == 'E') {
            User matchedUser = userRepository.findByIdentityHash(targetResult)
                    .orElseThrow(() -> new BadCredentialsException("Invalid identity credentials matrix."));
            usernameToAuthenticate = matchedUser.getUsername();
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(usernameToAuthenticate, loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return jwtUtils.generateToken(authentication);
    }

    @Override
    @Transactional
    public void initiatePasswordReset(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new ClientValidationException("Identifier parameter cannot be empty.");
        }

        char[] resolvedType = new char[]{'U'};
        String targetResult = normalizeAndHash(identifier, resolvedType);

        User user = (resolvedType[0] == 'E')
                ? userRepository.findByIdentityHash(targetResult).orElse(null)
                : userRepository.findByUsername(targetResult).orElse(null);

        String otp = generateSixDigitOtp();

        if (user != null) {
            user.setResetToken(otp);
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);

            stringRedisTemplate.delete(OTP_ATTEMPT_PREFIX + user.getUsername());

            log.info("🔑 [FORGOT PASSWORD OTP] Generated recovery OTP for @{}: {}", user.getUsername(), otp);

            if (identifier.contains("@")) {
                try {
                    emailService.sendOtpEmail(identifier, otp);
                } catch (Exception e) {
                    log.error("⚠️ [DISPATCH FAILURE] Could not deliver forgot password OTP to {}", identifier, e);
                    throw new IllegalStateException("Failed to deliver verification code. Please check your email configuration.");
                }
            }
        } else {
            log.warn("⚠️ [FORGOT PASSWORD] Recovery attempt for untraceable identity: {}", identifier);
        }

        log.info("Password recovery loop processed inside transaction frame.");
    }

    @Override
    @Transactional
    public void resetPassword(String identifier, String otp, String newPassword) {
        if (identifier == null || otp == null || newPassword == null) {
            throw new ClientValidationException("All reset parameters are required.");
        }

        char[] resolvedType = new char[]{'U'};
        String targetResult = normalizeAndHash(identifier, resolvedType);

        User user = (resolvedType[0] == 'E')
                ? userRepository.findByIdentityHash(targetResult).orElseThrow(() -> new ClientValidationException("Invalid verification parameters mapping match."))
                : userRepository.findByUsername(targetResult).orElseThrow(() -> new ClientValidationException("Invalid verification parameters mapping match."));

        if (user.getResetToken() == null) {
            throw new ClientValidationException("No active recovery transaction session found for profile configuration.");
        }
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ClientValidationException("Cryptographic verification timeframe expired or no active session.");
        }

        String cleanCached = user.getResetToken().replaceAll("[^0-9]", "");
        String cleanInput = otp.replaceAll("[^0-9]", "");

        if (cleanCached.equals(cleanInput) && !cleanCached.isEmpty()) {
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                throw new ClientValidationException("New password cannot be identical to your current password.");
            }

            enforcePasswordEntropy(newPassword);

            user.setPassword(passwordEncoder.encode(newPassword));
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);

            stringRedisTemplate.delete(OTP_ATTEMPT_PREFIX + user.getUsername());

            log.info("Transactional table re-write finalized successfully for @{}.", user.getUsername());
            return;
        }

        handleFailedOtpAttempt(user.getUsername(), () -> {
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public void updateProfile(String username, ProfileUpdateRequest request) {
        if (username == null || request == null) {
            throw new ClientValidationException("Profile update request parameters incomplete.");
        }

        User user = userRepository.findByUsername(username.trim().toLowerCase())
                .orElseThrow(() -> new ClientValidationException("Operator identity footprint untraceable."));

        if (request.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(request.getProfilePictureUrl().trim());
        }
        userRepository.save(user);
    }

    @Override
    public String getGhostId(String u) {
        return "id_mapped_node";
    }

    @Override
    public Optional<User> findByIdentityHash(String h) {
        return userRepository.findByIdentityHash(h);
    }

    @Override
    public Optional<User> findIdentityHash(String h) {
        return userRepository.findByIdentityHash(h);
    }

    @Override
    public Optional<User> findByResetToken(String t) {
        return userRepository.findByResetToken(t);
    }

    @Override
    public boolean existsByUsername(String u) {
        return userRepository.existsByUsername(u);
    }
}