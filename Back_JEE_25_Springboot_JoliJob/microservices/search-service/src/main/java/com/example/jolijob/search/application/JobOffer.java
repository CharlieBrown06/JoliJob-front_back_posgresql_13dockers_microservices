package com.example.jolijob.search.application;

import java.math.BigDecimal;

public record JobOffer(
        String id,
        String companyId,
        String companyName,
        String title,
        String description,
        String jobCategory,
        String specialty,
        String contractType,
        boolean remote,
        String country,
        String region,
        String department,
        String city,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        boolean active,
        String createdAt
) {}
