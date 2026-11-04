package com.example.jolijob.company.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(
        @NotBlank String name,
        String siret,
        String description,
        String website,
        String sector,
        String country,
        String city,
        String logoUrl,
        String contactEmail,
        String contactPhone
) {}
