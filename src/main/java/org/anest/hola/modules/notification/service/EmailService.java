package org.anest.hola.modules.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.anest.hola.common.i18n.MessageKeys;
import org.anest.hola.common.i18n.MessageUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Async
    public void sendVerificationOtp(String toEmail, String fullName, String otpCode, long expirationMinutes) {
        String subject = MessageUtils.get(MessageKeys.EMAIL_VERIFICATION_OTP_SUBJECT);
        String body = MessageUtils.get(
                MessageKeys.EMAIL_VERIFICATION_OTP_BODY,
                fullName, otpCode, expirationMinutes
        );
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendPasswordResetOtp(String toEmail, String fullName, String otpCode, long expirationMinutes) {
        String subject = MessageUtils.get(MessageKeys.EMAIL_PASSWORD_RESET_OTP_SUBJECT);
        String body = MessageUtils.get(
                MessageKeys.EMAIL_PASSWORD_RESET_OTP_BODY,
                fullName, otpCode, expirationMinutes
        );
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            javaMailSender.send(message);
        } catch (MailException ex) {
            log.error("Failed to send email to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }
}
