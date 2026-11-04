package com.example.jolijob.joboffer.web.dto;

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
    public static JobOfferResponse from(JobOffer o) {
        return new JobOfferResponse(
                o.getId(), o.getCompanyId(), o.getCompanyName(),
                o.getTitle(), o.getDescription(), o.getJobCategory(), o.getSpecialty(),
                o.getContractType(), o.isRemote(), o.getCountry(), o.getRegion(),
                o.getDepartment(), o.getCity(), o.getSalaryMin(), o.getSalaryMax(),
                o.getStartDate(), o.getExpiresAt(), o.isActive(), o.getCreatedAt()
        );
    }
}
