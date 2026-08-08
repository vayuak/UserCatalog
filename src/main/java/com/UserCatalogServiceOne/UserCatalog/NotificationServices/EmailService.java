package com.UserCatalogServiceOne.UserCatalog.NotificationServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Your Ghost Shield Verification Code");
            message.setText("Your OTP is: " + otp + ". It will expire in 5 minutes.");

            mailSender.send(message);
            log.info("Async Notification Outbound: Mail pushed to {}", to);
        } catch (Exception e) {
            log.error("CRITICAL SMTP CHANNEL FAILURE: {}", e.getMessage());
        }
    }
}