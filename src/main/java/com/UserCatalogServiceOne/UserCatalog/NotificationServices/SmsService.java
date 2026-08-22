package com.UserCatalogServiceOne.UserCatalog.NotificationServices;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.from-number}")
    private String fromNumber;

    @PostConstruct
    public void initTwilio() {
        try {
            Twilio.init(accountSid, authToken);
            log.info("📱 [TWILIO SMS ENGINE] Initialized successfully.");
        } catch (Exception e) {
            log.error("❌ Failed to initialize Twilio SDK: {}", e.getMessage());
        }
    }

    @Async // 🟢 Non-blocking background execution
    public void sendOtpSms(String toPhoneNumber, String otp) {
        log.info("=================================================");
        log.info("📱 [GHOST SHIELD SMS DISPATCH]");
        log.info("📩 TARGET PHONE: {}", toPhoneNumber);
        log.info("⚡ VERIFICATION CODE: {}", otp);
        log.info("=================================================");

        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber), // E.164 format: e.g. +1234567890 or +919876543210
                    new PhoneNumber(fromNumber),
                    "Your Ghost Shield verification code is: " + otp + ". Valid for 5 minutes."
            ).create();

            log.info("✅ SMS OTP delivered via Twilio! SID: {}", message.getSid());
        } catch (Exception e) {
            log.error("⚠️ Twilio SMS Dispatch Failure: {}", e.getMessage());
        }
    }
}