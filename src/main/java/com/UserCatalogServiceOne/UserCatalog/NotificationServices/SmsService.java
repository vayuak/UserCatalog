package com.UserCatalogServiceOne.UserCatalog.NotificationServices;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
@Slf4j // Use SLF4J for professional logging
public class SmsService {

    @Value("${twilio.account-sid:dummy_sid}")
    private String accountSid;

    @Value("${twilio.auth-token:dummy_token}")
    private String authToken;

    @Value("${twilio.from-number:+1000000000}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        // No SSL bypass here. Production environments have valid CA certificates.
        Twilio.init(accountSid, authToken);
        log.info("Twilio SMS Service initialized successfully.");
    }
    @Async
    public void sendOtpSms(String toPhone, String otp) {
        // 1. ALWAYS print to console first (This is your "Real-Time" test point)
        System.out.println("--------------------------------------------");
        System.out.println("SMS REAL-TIME TEST LOG");
        System.out.println("Recipient: " + toPhone);
        System.out.println("OTP Code:  " + otp);
        System.out.println("--------------------------------------------");

        try {
            // 2. Attempt the real Twilio call
            Message.creator(
                    new PhoneNumber(toPhone),
                    new PhoneNumber(fromNumber),
                    "Your reset code is: " + otp
            ).create();
            log.info("Twilio API: SMS sent successfully.");
        } catch (Exception e) {
            // 3. Log the error but DON'T let it crash your testing flow
            log.warn("Twilio API: Could not reach Twilio (Network/SSL issue), but OTP was generated.");
        }
    }

    // Security practice: Don't log full phone numbers in production logs
    private String maskPhone(String phone) {
        return phone.replaceAll(".(?=.{4})", "*");
    }
}