// REPLACES: UserCatalog/.../Configurations/MailConfig.java
//
// Your old file had a real Gmail address and its 16-character app password written in the source code (and in git).
// Treat that password as leaked: revoke it at https://myaccount.google.com/apppasswords right now.
// This version takes SMTP settings from environment variables (see deploy/.env). Use a transactional email provider
// (Brevo free = 300 mails/day, or Resend / Zoho ZeptoMail) with your own warnpack.com domain so codes do not land in spam.
package com.UserCatalogServiceOne.UserCatalog.Configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${SMTP_HOST:smtp-relay.brevo.com}") private String host;
    @Value("${SMTP_PORT:587}") private int port;
    @Value("${SMTP_USERNAME:}") private String username;
    @Value("${SMTP_PASSWORD:}") private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "8000");
        props.put("mail.smtp.writetimeout", "8000");
        props.put("mail.debug", "false");
        return mailSender;
    }
}
