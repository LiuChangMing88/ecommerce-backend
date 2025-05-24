package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.model.PasswordResetToken;
import com.github.liuchangming88.ecommerce_backend.model.VerificationToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender javaMailSender;

    @Value("${email.from}")
    private String fromAddress;

    @Value("${frontend.url}")
    private String url;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    private SimpleMailMessage createMailMessage() {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom(fromAddress);
        return simpleMailMessage;
    }

    /**
     * Handles sending email to a client
     */

    public void sendVerificationEmail (VerificationToken verificationToken) {
        SimpleMailMessage simpleMailMessage = createMailMessage();
        simpleMailMessage.setTo(verificationToken.getLocalUser().getEmail());
        simpleMailMessage.setSubject("Follow this link to verify your email and activate your account");
        simpleMailMessage.setText("Click this link to verify the account your email is associated with. If you did not make any account, ignore this email.\n" +
                url + "/auth/verify?token=" + verificationToken.getToken());
        javaMailSender.send(simpleMailMessage);
    }

    public void sendPasswordResetEmail (PasswordResetToken passwordResetToken) {
        SimpleMailMessage simpleMailMessage = createMailMessage();
        simpleMailMessage.setTo(passwordResetToken.getLocalUser().getEmail());
        simpleMailMessage.setSubject("Follow this link to reset the password to your account");
        simpleMailMessage.setText("Click this link to reset the password to your account (username: " + passwordResetToken.getLocalUser().getUsername() +
                ". If you did not make password reset request, ignore this email.\n" +
                url + "/auth/reset?token=" + passwordResetToken.getToken());
        javaMailSender.send(simpleMailMessage);
    }
}