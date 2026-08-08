package com.UserCatalogServiceOne.UserCatalog.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpVerificationEngine {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String OTP_KEY_PREFIX = "auth:otp:";
    private static final String OTP_ATTEMPTS_PREFIX = "auth:otp:attempts:";
    private static final int MAX_ALLOWED_ATTEMPTS = 3;

    /**
     * Cache verification codes with an atomic attempt counter.
     */
    public void stageOtpToken(String username, String generatedOtp) {
        String otpKey = OTP_KEY_PREFIX + username;
        String attemptKey = OTP_ATTEMPTS_PREFIX + username;

        // Code has an exact 5-minute expiration lifespan
        redisTemplate.opsForValue().set(otpKey, generatedOtp, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(attemptKey, "0", 5, TimeUnit.MINUTES);

        log.info("🔒 Cryptographic OTP tracking parameters initialized for node: {}", username);
    }

    /**
     * Validates incoming tokens and automatically self-destructs them upon reaching failure thresholds.
     */
    public boolean verifyIncomingOtp(String username, String incomingOtp) {
        String otpKey = OTP_KEY_PREFIX + username;
        String attemptKey = OTP_ATTEMPTS_PREFIX + username;

        Object cachedOtp = redisTemplate.opsForValue().get(otpKey);
        Object cachedAttempts = redisTemplate.opsForValue().get(attemptKey);

        if (cachedOtp == null || cachedAttempts == null) {
            throw new RuntimeException("Verification code expired or untraceable. Request a new token payload.");
        }

        int currentAttempts = Integer.parseInt(cachedAttempts.toString());

        if (currentAttempts >= MAX_ALLOWED_ATTEMPTS) {
            purgeOtpRegistry(username);
            throw new RuntimeException("Security Exception: Access blocked due to threat signatures. Token trashed.");
        }

        if (cachedOtp.toString().equals(incomingOtp.trim())) {
            purgeOtpRegistry(username);
            log.info("✅ Security Pass: Token match recorded for user node: {}", username);
            return true;
        } else {
            // Atomic operations protect against multithreaded race condition bypass attempts
            long updatedAttempts = redisTemplate.opsForValue().increment(attemptKey, 1);
            log.warn("⚠️ Invalid code input logged for user: {} | Active Attempt Counter: {}", username, updatedAttempts);

            if (updatedAttempts >= MAX_ALLOWED_ATTEMPTS) {
                purgeOtpRegistry(username);
                log.error("🚨 COUNTER-INTELLIGENCE TRASH ACTION: 3 failed attempts registered for user handle: {}. Token self-destructed.", username);
                throw new RuntimeException("Security Violation: Failure limit hit. OTP invalidated permanently.");
            }
            return false;
        }
    }

    public void purgeOtpRegistry(String username) {
        redisTemplate.delete(OTP_KEY_PREFIX + username);
        redisTemplate.delete(OTP_ATTEMPTS_PREFIX + username);
    }
}