package com.example.jolijob.profile.adapter.in.rest;

import com.example.jolijob.profile.domain.CompanyDetails;
import com.example.jolijob.profile.domain.CompanyProfile;

import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
        UUID id, UUID userId, String name, String siret, String description, String website,
        String sector, String country, String city, String logoUrl, String contactEmail,
        String contactPhone, Instant createdAt, Instant updatedAt
) {
    static CompanyResponse from(CompanyProfile profile) {
        CompanyDetails details = profile.details();
        return new CompanyResponse(profile.id(), profile.userId(), details.name(), details.siret(),
                details.description(), details.website(), details.sector(), details.country(), details.city(),
                details.logoUrl(), details.contactEmail(), details.contactPhone(),
                profile.createdAt(), profile.updatedAt());
    }
}
