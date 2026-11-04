package com.example.jolijob.joboffer.adapter.out.outbox;

import com.example.jolijob.joboffer.adapter.out.kafka.JobOfferEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class JobOfferOutboxBatchProcessor {
    private static final Logger logger = LoggerFactory.getLogger(JobOfferOutboxBatchProcessor.class);

    private static final String NEXT_PENDING = """
            SELECT pending.event_id, pending.aggregate_id, pending.topic, pending.payload
            FROM job_offer_outbox pending
            WHERE pending.published_at IS NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM job_offer_outbox earlier
                  WHERE earlier.aggregate_id = pending.aggregate_id
                    AND earlier.event_order < pending.event_order
                    AND earlier.published_at IS NULL
              )
            ORDER BY pending.event_order
            LIMIT ?
            FOR UPDATE SKIP LOCKED
            """;

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, Object> kafka;
    private final JsonMapper json;
    private final Clock clock;
    private final Duration acknowledgementTimeout;

    public JobOfferOutboxBatchProcessor(JdbcTemplate jdbc, KafkaTemplate<String, Object> kafka,
                                        JsonMapper json, Clock clock,
                                        @Value("${job-offer.outbox.relay.ack-timeout:10s}")
                                        Duration acknowledgementTimeout) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.json = json;
        this.clock = clock;
        if (acknowledgementTimeout.isNegative() || acknowledgementTimeout.isZero()) {
            throw new IllegalArgumentException("Kafka acknowledgement timeout must be positive");
        }
        this.acknowledgementTimeout = acknowledgementTimeout;
    }

    /**
     * Kafka acknowledgements precede the database mark, so a crash between them can cause
     * redelivery. A failure rolls back the whole batch transaction, including marks for
     * previously acknowledged records, so those records can be published again. Consumers
     * should use eventId for idempotency; delivery is at least once, never exactly once.
     *
     * A timed-out send is not cancelled, but processing stops before sending another event;
     * a retry uses the same stable event id and aggregate key through the Kafka producer.
     */
    @Transactional
    public int publishBatch(int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        List<PendingEvent> events = jdbc.query(NEXT_PENDING, (result, row) -> new PendingEvent(
                result.getObject("event_id", UUID.class),
                result.getObject("aggregate_id", UUID.class),
                result.getString("topic"),
                result.getString("payload")), batchSize);

        for (PendingEvent event : events) {
            JobOfferEventMessage message;
            try {
                message = json.readValue(event.payload(), JobOfferEventMessage.class);
            } catch (JacksonException exception) {
                logger.error("Could not decode pending job offer event {}; retaining it for investigation",
                        event.eventId(), exception);
                throw new IllegalStateException("Could not decode job offer outbox event " + event.eventId(),
                        exception);
            }
            try {
                kafka.send(event.topic(), event.aggregateId().toString(), message)
                        .get(acknowledgementTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while awaiting Kafka acknowledgement for job offer event {}; "
                                + "retaining it for retry", event.eventId(), exception);
                throw new IllegalStateException("Interrupted while publishing job offer event "
                        + event.eventId(), exception);
            } catch (TimeoutException exception) {
                logger.error("Kafka acknowledgement timed out for job offer event {}; retaining it for retry",
                        event.eventId(), exception);
                throw new IllegalStateException("Timed out publishing job offer event " + event.eventId(),
                        exception);
            } catch (ExecutionException | RuntimeException exception) {
                logger.error("Kafka acknowledgement failed for job offer event {}; retaining it for retry",
                        event.eventId(), exception);
                throw new IllegalStateException("Kafka acknowledgement failed for job offer event "
                        + event.eventId(), exception);
            }
            int updated = jdbc.update("""
                    UPDATE job_offer_outbox
                    SET published_at = ?
                    WHERE event_id = ? AND published_at IS NULL
                    """, Timestamp.from(clock.instant()), event.eventId());
            if (updated != 1) {
                throw new IllegalStateException("Pending job offer event was not marked delivered: "
                        + event.eventId());
            }
        }
        return events.size();
    }

    private record PendingEvent(UUID eventId, UUID aggregateId, String topic, String payload) {}
}
