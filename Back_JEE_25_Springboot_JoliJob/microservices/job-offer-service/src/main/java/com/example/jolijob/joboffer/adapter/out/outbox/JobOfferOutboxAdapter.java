package com.example.jolijob.joboffer.adapter.out.outbox;

import com.example.jolijob.joboffer.adapter.out.kafka.JobOfferEventMessage;
import com.example.jolijob.joboffer.application.port.out.JobOfferEventPort;
import com.example.jolijob.joboffer.domain.JobOffer;
import com.example.jolijob.joboffer.domain.JobOfferEventType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;

@Component
public class JobOfferOutboxAdapter implements JobOfferEventPort {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final Clock clock;

    public JobOfferOutboxAdapter(JdbcTemplate jdbc, JsonMapper json, Clock clock) {
        this.jdbc = jdbc;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public void publish(JobOffer offer, JobOfferEventType type) {
        var occurredAt = clock.instant();
        var message = JobOfferEventMessage.from(offer, type, occurredAt);
        final String payload;
        try {
            payload = json.writeValueAsString(message);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize job offer event " + message.eventId(), exception);
        }
        jdbc.update("""
                INSERT INTO job_offer_outbox (event_id, aggregate_id, topic, payload, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, UUID.fromString(message.eventId()), offer.id(), JobOfferOutboxRelay.TOPIC, payload,
                Timestamp.from(occurredAt));
    }
}
