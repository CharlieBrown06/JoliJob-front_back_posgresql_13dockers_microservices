package com.example.jolijob.joboffer.web.dto;

import com.example.jolijob.joboffer.domain.ContractType;
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
) {}
