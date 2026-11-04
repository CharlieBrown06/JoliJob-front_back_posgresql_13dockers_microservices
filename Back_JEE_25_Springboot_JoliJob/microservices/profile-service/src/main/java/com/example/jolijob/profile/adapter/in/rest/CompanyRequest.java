package com.example.jolijob.profile.adapter.in.rest;

import com.example.jolijob.profile.domain.CompanyDetails;
import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(
        @NotBlank String name, String siret, String description, String website, String sector,
        String country, String city, String logoUrl, String contactEmail, String contactPhone
) {
    CompanyDetails toDetails() {
        return new CompanyDetails(name, siret, description, website, sector, country,
                city, logoUrl, contactEmail, contactPhone);
    }
}
