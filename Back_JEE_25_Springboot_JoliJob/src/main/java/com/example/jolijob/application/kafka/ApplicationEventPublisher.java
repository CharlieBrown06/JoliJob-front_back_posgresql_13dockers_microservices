package com.example.jolijob.application.kafka;

import com.example.jolijob.application.domain.Application;
import com.example.jolijob.application.port.out.ApplicationEventPort;
import com.example.jolijob.shared.event.ApplicationEventType;
import com.example.jolijob.shared.event.ApplicationKafkaEvent;
import com.example.jolijob.shared.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationEventPublisher implements ApplicationEventPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(Application app, ApplicationEventType type) {
        ApplicationKafkaEvent event = new ApplicationKafkaEvent(
                UUID.randomUUID().toString(),
                type,
                app.getId().toString(),
                app.getJobOfferId().toString(),
                app.getJobTitle(),
                app.getCandidateId().toString(),
                app.getCandidateEmail(),
                app.getCompanyId() != null ? app.getCompanyId().toString() : null,
                app.getCompanyEmail(),
                app.getStatus().name(),
                Instant.now()
        );
        kafkaTemplate.send(KafkaTopics.APPLICATION_EVENTS, app.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Erreur publication Kafka application event {}", app.getId(), ex);
                    }
                });
    }
}
