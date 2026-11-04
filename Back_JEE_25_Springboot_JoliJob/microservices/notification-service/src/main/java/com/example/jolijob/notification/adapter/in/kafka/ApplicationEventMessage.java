package com.example.jolijob.notification.adapter.in.kafka;

public record ApplicationEventMessage(
        String eventId,
        String type,
        String applicationId,
        String jobOfferId,
        String jobTitle,
        String candidateId,
        String candidateEmail,
        String companyId,
        String companyEmail,
        String newStatus,
        String occurredAt
) {}
