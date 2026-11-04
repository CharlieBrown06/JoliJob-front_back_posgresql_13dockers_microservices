package com.example.jolijob.joboffer.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record JobOfferDetails(
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
        LocalDate expiresAt
) {}
