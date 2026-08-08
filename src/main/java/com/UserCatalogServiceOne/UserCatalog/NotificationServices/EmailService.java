package com.UserCatalogServiceOne.UserCatalog.NotificationServices;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        log.info("=================================================");
        log.info("🔑 [GHOST SHIELD OTP DISPATCH]");
        log.info("📩 TARGET RECIPIENT: {}", toEmail);
        log.info("⚡ VERIFICATION CODE: {}", otp);
        log.info("=================================================");

        try {
            Resend resend = new Resend(resendApiKey);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("GhostShield <onboarding@resend.dev>")
                    .to(toEmail)
                    .subject("Your Ghost Shield Verification Code")
                    .html("<div style='font-family: sans-serif; background: #000; color: #fff; padding: 20px; border-radius: 8px;'>" +
                            "<h2 style='color: #888;'>GHOST SHIELD SECURE TERMINAL</h2>" +
                            "<p>Your verification code is: <b style='font-size: 24px; color: #00C851;'>" + otp + "</b></p>" +
                            "<p style='color: #888;'>This code will expire in 5 minutes.</p>" +
                            "</div>")
                    .build();

            CreateEmailResponse data = resend.emails().send(params);
            log.info("✅ Live OTP email delivered via Resend API! Message ID: {}", data.getId());

        } catch (Exception e) {
            log.error("⚠️ Resend API Dispatch Failure: {}", e.getMessage());
        }
    }
}