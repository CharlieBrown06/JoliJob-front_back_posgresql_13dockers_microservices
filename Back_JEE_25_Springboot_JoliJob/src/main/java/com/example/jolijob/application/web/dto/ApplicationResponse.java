package com.example.jolijob.application.web.dto;

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
    public static ApplicationResponse from(Application a) {
        return new ApplicationResponse(
                a.getId(), a.getJobOfferId(), a.getJobTitle(),
                a.getCandidateId(), a.getCoverLetter(), a.getCvUrl(),
                a.getStatus(), a.getSubmittedAt(), a.getUpdatedAt()
        );
    }
}
