package com.example.jolijob.application.domain;

import java.time.Instant;
import java.util.UUID;

public record Application(
        UUID id,
        UUID jobOfferId,
        String jobTitle,
        UUID candidateId,
        String candidateEmail,
        UUID companyId,
        String companyEmail,
        String coverLetter,
        String cvUrl,
        ApplicationStatus status,
        Instant submittedAt,
        Instant updatedAt
) {
    public Application withStatus(ApplicationStatus newStatus, Instant updatedAt) {
        return new Application(id, jobOfferId, jobTitle, candidateId, candidateEmail, companyId,
                companyEmail, coverLetter, cvUrl, newStatus, submittedAt, updatedAt);
    }
}
