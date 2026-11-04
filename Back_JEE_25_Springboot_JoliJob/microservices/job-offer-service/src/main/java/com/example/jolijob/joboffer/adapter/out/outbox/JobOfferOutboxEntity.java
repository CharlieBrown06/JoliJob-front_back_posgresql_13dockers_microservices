package com.example.jolijob.joboffer.adapter.out.outbox;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_offer_outbox", indexes = {
        @Index(name = "idx_job_offer_outbox_aggregate_order", columnList = "aggregate_id,event_order"),
        @Index(name = "idx_job_offer_outbox_pending", columnList = "published_at,event_order")
})
public class JobOfferOutboxEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_order")
    Long eventOrder;

    @Column(name = "event_id", nullable = false, unique = true)
    UUID eventId;

    @Column(name = "aggregate_id", nullable = false)
    UUID aggregateId;

    @Column(nullable = false)
    String topic;

    @Column(nullable = false, columnDefinition = "TEXT")
    String payload;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "published_at")
    Instant publishedAt;

    protected JobOfferOutboxEntity() {}
}
