package com.example.jolijob.shared.event;

import java.time.Instant;

public record ApplicationKafkaEvent(
        String eventId,
        ApplicationEventType type,
        String applicationId,
        String jobOfferId,
        String jobTitle,
        String candidateId,
        String candidateEmail,
        String companyId,
        String companyEmail,
        String newStatus,
        Instant occurredAt
) {}
