package com.example.jolijob.indexer.adapter.in.kafka;

import java.math.BigDecimal;
import java.time.Instant;

public record JobOfferEventMessage(
        String eventId,
        String type,
        String jobOfferId,
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
        Instant occurredAt
) {}
