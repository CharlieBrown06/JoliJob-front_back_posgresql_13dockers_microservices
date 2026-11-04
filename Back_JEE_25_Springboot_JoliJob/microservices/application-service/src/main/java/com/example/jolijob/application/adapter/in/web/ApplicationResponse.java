package com.example.jolijob.application.adapter.in.web;

import com.example.jolijob.application.domain.Application;
import com.example.jolijob.application.domain.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID jobOfferId,
        String jobTitle,
        UUID candidateId,
        String coverLetter,
        String cvUrl,
        ApplicationStatus status,
        Instant submittedAt,
        Instant updatedAt
) {
    static ApplicationResponse from(Application application) {
        return new ApplicationResponse(application.id(), application.jobOfferId(), application.jobTitle(),
                application.candidateId(), application.coverLetter(), application.cvUrl(),
                application.status(), application.submittedAt(), application.updatedAt());
    }
}
