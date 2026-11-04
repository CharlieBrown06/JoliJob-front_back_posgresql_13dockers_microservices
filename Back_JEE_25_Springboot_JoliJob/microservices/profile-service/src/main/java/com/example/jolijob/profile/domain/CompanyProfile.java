package com.example.jolijob.profile.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CompanyProfile(
        UUID id, UUID userId, CompanyDetails details, Instant createdAt, Instant updatedAt
) {
    public CompanyProfile {
        Objects.requireNonNull(id);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(details);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
    }

    public CompanyProfile update(CompanyDetails details, Instant now) {
        return new CompanyProfile(id, userId, details, createdAt, now);
    }
}
