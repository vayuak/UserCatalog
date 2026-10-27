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

    @Value("${MAIL_FROM:WarnPack <no-reply@warnpack.com>}")
    private String fromEmail;

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Your WarnPack verification code");
            message.setText("Your WarnPack verification code is: " + otp
                    + "\n\nIt expires in 5 minutes. If you did not request it, you can ignore this email."
                    + "\n\nWarnPack - Explore together. Travel safely.");
            mailSender.send(message);
            log.info("OTP email dispatched.");
        } catch (Exception e) {
            log.error("OTP email failed: {}", e.getMessage());
            throw new IllegalStateException("Email delivery failed.");
        }
    }
}
