package com.UserCatalogServiceOne.UserCatalog.NotificationServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        log.info("=================================================");
        log.info("  [GHOST SHIELD OTP DISPATCH]");
        log.info("  TARGET RECIPIENT: {}", toEmail);
        log.info("  VERIFICATION CODE: {}", otp);
        log.info("=================================================");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Your Caravel Verification Code");
            message.setText("Your verification code is: " + otp + "\n\nThis code will expire in 5 minutes.");

            mailSender.send(message);
            log.info("  Live OTP email delivered via GMAIL SMTP to {}", toEmail);
        } catch (Exception e) {
            log.error("  Gmail SMTP Dispatch Failure: {}", e.getMessage());
            throw new IllegalStateException("Email delivery failed: " + e.getMessage());
        }
    }
}