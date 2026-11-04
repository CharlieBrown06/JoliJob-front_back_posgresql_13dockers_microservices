package com.example.jolijob.notification.adapter.config;

import com.example.jolijob.notification.application.port.in.ProcessApplicationEvent;
import com.example.jolijob.notification.application.port.out.EmailSenderPort;
import com.example.jolijob.notification.application.service.ApplicationNotificationHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfiguration {

    @Bean
    ProcessApplicationEvent processApplicationEvent(EmailSenderPort emailSenderPort) {
        return new ApplicationNotificationHandler(emailSenderPort);
    }
}
