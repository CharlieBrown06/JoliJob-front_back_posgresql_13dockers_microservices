package com.example.jolijob.joboffer.adapter.in.rest;

import com.example.jolijob.joboffer.domain.ContractType;
import com.example.jolijob.joboffer.domain.JobOfferDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record JobOfferRequest(
        @NotBlank String title,
        @NotBlank String description,
        String jobCategory,
        String specialty,
        @NotNull ContractType contractType,
        boolean remote,
        String country,
        String region,
        String department,
        String city,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        LocalDate startDate,
        LocalDate expiresAt
) {
    JobOfferDetails toDetails() {
        return new JobOfferDetails(title, description, jobCategory, specialty, contractType,
                remote, country, region, department, city, salaryMin, salaryMax, startDate, expiresAt);
    }
}
