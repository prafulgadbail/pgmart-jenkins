package com.pgmart.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Sends the OTP email (SMTP: Mailpit locally, Amazon SES in AWS). */
@Component
public class Mailer {

    private static final Logger log = LoggerFactory.getLogger(Mailer.class);

    private final JavaMailSender sender;
    private final String from;

    public Mailer(JavaMailSender sender, @Value("${app.mail-from}") String from) {
        this.sender = sender;
        this.from = from;
    }

    public void sendOtp(String to, String name, String code, String purpose) {
        boolean reset = "RESET".equals(purpose);
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(to);
        mail.setSubject(code + (reset ? " is your PGMart password reset code" : " is your PGMart verification code"));
        mail.setText("""
                Hi %s,

                Your PGMart %s code is: %s

                It is valid for 10 minutes. If you did not ask for it, you can ignore this email.

                PGMart · Cloud & DevOps Book Store
                """.formatted(name, reset ? "password reset" : "verification", code));
        try {
            sender.send(mail);
            log.info("OTP email sent purpose={}", purpose);
        } catch (MailException e) {
            log.error("Could not send OTP email", e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_FAILED",
                    "We could not send the email right now. Please try again in a minute.");
        }
    }
}
