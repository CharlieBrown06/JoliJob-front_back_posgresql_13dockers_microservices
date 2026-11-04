package com.example.jolijob.application.domain;

public record ApplicationEvent(
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
) {
}
