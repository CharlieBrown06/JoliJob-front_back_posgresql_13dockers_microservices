package com.example.jolijob.indexer.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;

public interface JobOfferIndexPort {
    void index(IndexedJobOffer offer);
    void update(JobOfferUpdate offer);
    void deactivate(String jobOfferId);

    record IndexedJobOffer(
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
            Instant createdAt
    ) {}

    record JobOfferUpdate(
            String id,
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
            BigDecimal salaryMax
    ) {}
}
