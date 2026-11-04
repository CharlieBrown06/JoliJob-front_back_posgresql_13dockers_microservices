package com.example.jolijob.profile.domain;

public record CompanyDetails(
        String name, String siret, String description, String website, String sector,
        String country, String city, String logoUrl, String contactEmail, String contactPhone
) {
    public CompanyDetails {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Company name is required");
        }
    }
}
