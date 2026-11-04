package com.example.jolijob.notification.adapter.in.kafka;

import com.example.jolijob.notification.application.domain.ApplicationEvent;
import com.example.jolijob.notification.application.domain.ApplicationEventType;
import com.example.jolijob.notification.application.port.in.ProcessApplicationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationKafkaConsumer {

    private final ProcessApplicationEvent processApplicationEvent;

    public ApplicationKafkaConsumer(ProcessApplicationEvent processApplicationEvent) {
        this.processApplicationEvent = processApplicationEvent;
    }

    @KafkaListener(topics = "application.events", groupId = "jolijob-notification")
    public void consume(ApplicationEventMessage message) {
        processApplicationEvent.process(new ApplicationEvent(
                ApplicationEventType.valueOf(message.type()),
                message.applicationId(),
                message.jobTitle(),
                message.candidateEmail(),
                message.companyEmail(),
                message.newStatus()));
    }
}
