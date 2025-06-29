package com.dali.ecommerce.account;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String token){
        String resetUrl = "http://localhost:8080/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@dali.com");
        message.setTo(to);
        message.setSubject("DALI - Password Reset Request");
        message.setText("To reset your password, please click the link below:\n" + resetUrl);

        mailSender.send(message);
    }
}
