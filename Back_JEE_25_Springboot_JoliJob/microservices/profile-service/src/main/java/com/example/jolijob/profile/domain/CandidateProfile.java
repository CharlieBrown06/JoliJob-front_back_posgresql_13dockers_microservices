package com.example.jolijob.profile.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CandidateProfile(
        UUID id, UUID userId, CandidateDetails details, Instant createdAt, Instant updatedAt
) {
    public CandidateProfile {
        Objects.requireNonNull(id);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(details);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
    }

    public CandidateProfile update(CandidateDetails details, Instant now) {
        return new CandidateProfile(id, userId, details, createdAt, now);
    }
}
