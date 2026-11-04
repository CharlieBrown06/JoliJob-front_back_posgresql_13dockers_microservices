package com.example.jolijob.company.web.dto;

import com.example.jolijob.company.domain.Company;

import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        UUID userId,
        String name,
        String siret,
        String description,
        String website,
        String sector,
        String country,
        String city,
        String logoUrl,
        String contactEmail,
        String contactPhone,
        Instant createdAt,
        Instant updatedAt
) {
    public static CompanyResponse from(Company c) {
        return new CompanyResponse(
                c.getId(), c.getUserId(), c.getName(), c.getSiret(),
                c.getDescription(), c.getWebsite(), c.getSector(),
                c.getCountry(), c.getCity(), c.getLogoUrl(),
                c.getContactEmail(), c.getContactPhone(), c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
