package com.example.jolijob.joboffer.domain;

import java.time.Instant;
import java.util.UUID;

public record JobOffer(
        UUID id,
        UUID companyId,
        String companyName,
        JobOfferDetails details,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public JobOffer update(JobOfferDetails replacement, Instant now) {
        return new JobOffer(id, companyId, companyName, replacement, active, createdAt, now);
    }

    public JobOffer deactivate(Instant now) {
        return new JobOffer(id, companyId, companyName, details, false, createdAt, now);
    }
}
