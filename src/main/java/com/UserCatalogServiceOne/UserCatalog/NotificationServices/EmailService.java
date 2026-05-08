package com.UserCatalogServiceOne.UserCatalog.NotificationServices;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;




@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:dummy@gmail.com}")
    private String fromEmail;
    @Async
    public void sendOtpEmail(String to, String otp) {
        // Mock check: if using default, just log to console
        if ("dummy@gmail.com".equals(fromEmail)) {
            System.out.println("MOCK EMAIL SENT to " + to + " | OTP: " + otp);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Your Verification Code");
        message.setText("Your OTP is: " + otp + ". It will expire in 5 minutes.");

        mailSender.send(message);
    }
}