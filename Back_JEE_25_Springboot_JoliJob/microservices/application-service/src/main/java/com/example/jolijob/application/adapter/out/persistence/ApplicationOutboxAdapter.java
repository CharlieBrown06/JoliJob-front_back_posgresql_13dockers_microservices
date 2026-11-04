package com.example.jolijob.application.adapter.out.persistence;

import com.example.jolijob.application.domain.ApplicationEvent;
import com.example.jolijob.application.port.out.ApplicationEventPort;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class ApplicationOutboxAdapter implements ApplicationEventPort {
    private final SpringDataApplicationOutboxRepository outbox;
    private final ObjectMapper objectMapper;

    public ApplicationOutboxAdapter(SpringDataApplicationOutboxRepository outbox, ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(ApplicationEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            outbox.save(new ApplicationOutboxEntity(
                    event.eventId(), event.applicationId(), payload, Instant.now()));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize application event " + event.eventId(), exception);
        }
    }
}
