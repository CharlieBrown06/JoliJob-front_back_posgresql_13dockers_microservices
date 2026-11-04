package com.example.jolijob.notification.adapter.out.mail;

import com.example.jolijob.notification.application.port.out.EmailSenderPort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SpringMailAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;

    public SpringMailAdapter(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom("noreply@jolijob.fr");
        mailSender.send(message);
    }
}
