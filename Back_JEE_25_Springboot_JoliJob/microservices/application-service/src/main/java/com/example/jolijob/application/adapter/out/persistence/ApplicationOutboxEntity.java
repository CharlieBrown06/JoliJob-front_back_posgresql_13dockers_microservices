package com.example.jolijob.application.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "application_outbox", indexes = {
        @Index(name = "idx_application_outbox_pending", columnList = "published_at, next_attempt_at, sequence_id"),
        @Index(name = "idx_application_outbox_aggregate_order",
                columnList = "aggregate_id, published_at, sequence_id")
})
public class ApplicationOutboxEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sequenceId;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    protected ApplicationOutboxEntity() {
    }

    ApplicationOutboxEntity(String eventId, String aggregateId, String payload, Instant createdAt) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.createdAt = createdAt;
        this.nextAttemptAt = createdAt;
    }

    public String getPayload() {
        return payload;
    }

    public String getEventId() {
        return eventId;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int recordFailure(Instant nextAttemptAt, String error) {
        attemptCount++;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = error;
        return attemptCount;
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
        this.lastError = null;
    }
}
