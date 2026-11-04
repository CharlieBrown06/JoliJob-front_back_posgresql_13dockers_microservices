package com.example.jolijob.joboffer.adapter.in.rest;

import com.example.jolijob.joboffer.domain.ContractType;
import com.example.jolijob.joboffer.domain.JobOffer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JobOfferResponse(
        UUID id,
        UUID companyId,
        String companyName,
        String title,
        String description,
        String jobCategory,
        String specialty,
        ContractType contractType,
        boolean remote,
        String country,
        String region,
        String department,
        String city,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        LocalDate startDate,
        LocalDate expiresAt,
        boolean active,
        Instant createdAt
) {
    static JobOfferResponse from(JobOffer offer) {
        var d = offer.details();
        return new JobOfferResponse(offer.id(), offer.companyId(), offer.companyName(),
                d.title(), d.description(), d.jobCategory(), d.specialty(), d.contractType(),
                d.remote(), d.country(), d.region(), d.department(), d.city(),
                d.salaryMin(), d.salaryMax(), d.startDate(), d.expiresAt(), offer.active(), offer.createdAt());
    }
}
