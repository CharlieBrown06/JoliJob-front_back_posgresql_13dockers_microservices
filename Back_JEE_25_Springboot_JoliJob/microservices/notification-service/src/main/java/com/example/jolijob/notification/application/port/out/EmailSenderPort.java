package com.example.jolijob.notification.application.port.out;

public interface EmailSenderPort {
    void send(String to, String subject, String body);
}
