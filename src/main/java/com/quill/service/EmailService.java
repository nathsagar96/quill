package com.quill.service;

import com.quill.config.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    public void sendVerificationEmail(String to, String token) {
        String subject = "Verify your Quill account";
        String link = appProperties.baseUrl() + "/verify-email?token=" + token;
        String body = buildVerificationBody(link);

        sendHtmlEmail(to, subject, body);
    }

    public void sendPasswordResetEmail(String to, String token) {
        String subject = "Reset your Quill password";
        String link = appProperties.baseUrl() + "/reset-password?token=" + token;
        String body = buildPasswordResetBody(link);

        sendHtmlEmail(to, subject, body);
    }

    private void sendHtmlEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            log.info("Email sent to '{}' with subject '{}'", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to '{}': {}", to, e.getMessage());
        }
    }

    private static String buildVerificationBody(String link) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: sans-serif; padding: 2em;">
                  <h2>Welcome to Quill!</h2>
                  <p>Click the link below to verify your email address:</p>
                  <p><a href="%s">Verify email</a></p>
                  <p>This link expires in 24 hours.</p>
                </body>
                </html>
                """.formatted(link);
    }

    private static String buildPasswordResetBody(String link) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: sans-serif; padding: 2em;">
                  <h2>Reset your password</h2>
                  <p>Click the link below to reset your password:</p>
                  <p><a href="%s">Reset password</a></p>
                  <p>This link expires in 24 hours. If you did not request this, you can ignore this email.</p>
                </body>
                </html>
                """.formatted(link);
    }
}
