package com.UserCatalogServiceOne.UserCatalog.NotificationServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender; // Auto-configured Brevo Sender

    @Value("${spring.mail.username}")
    private String brevoEmail;

    @Value("${gmail.fallback.username}")
    private String gmailUsername;

    @Value("${gmail.fallback.password}")
    private String gmailPassword;

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        log.info("=================================================");
        log.info("  [GHOST SHIELD OTP DISPATCH]");
        log.info("  TARGET RECIPIENT: {}", toEmail);
        log.info("  VERIFICATION CODE: {}", otp);
        log.info("=================================================");

        try {
            // 1. Attempt Primary Dispatch (Brevo)
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(brevoEmail);
            message.setTo(toEmail);
            message.setSubject("Your Ghost Shield Verification Code");
            message.setText("Your verification code is: " + otp + "\n\nThis code will expire in 5 minutes.");

            mailSender.send(message);
            log.info("  Live OTP email delivered via Brevo SMTP to {}", toEmail);

        } catch (Exception e) {
            log.warn("  Brevo SMTP Dispatch Failure: {}. Initiating Gmail Fallback.", e.getMessage());

            // 2. Attempt Fallback Dispatch (Gmail)
            try {
                JavaMailSenderImpl fallbackSender = new JavaMailSenderImpl();
                fallbackSender.setHost("smtp.gmail.com");
                fallbackSender.setPort(587);
                fallbackSender.setUsername(gmailUsername);
                fallbackSender.setPassword(gmailPassword);

                Properties props = fallbackSender.getJavaMailProperties();
                props.put("mail.transport.protocol", "smtp");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");

                SimpleMailMessage fallbackMsg = new SimpleMailMessage();
                fallbackMsg.setFrom(gmailUsername);
                fallbackMsg.setTo(toEmail);
                fallbackMsg.setSubject("Your Ghost Shield Verification Code");
                fallbackMsg.setText("Your verification code is: " + otp + "\n\nThis code will expire in 5 minutes.");

                fallbackSender.send(fallbackMsg);
                log.info("  Live OTP email delivered via GMAIL FALLBACK to {}", toEmail);

            } catch (Exception fallbackEx) {
                log.error("  FATAL: Both Brevo and Gmail dispatch failed. {}", fallbackEx.getMessage());
                // Throwing this triggers the rollback mechanism we wrote in UserServiceImpl
                throw new IllegalStateException("Email delivery failed on all channels.");
            }
        }
    }
}