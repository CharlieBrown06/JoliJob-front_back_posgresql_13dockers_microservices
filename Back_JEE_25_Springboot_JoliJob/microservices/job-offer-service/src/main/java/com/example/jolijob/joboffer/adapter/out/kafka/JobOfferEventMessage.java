package com.example.jolijob.joboffer.adapter.out.kafka;

import com.example.jolijob.joboffer.domain.JobOffer;
import com.example.jolijob.joboffer.domain.JobOfferEventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
) {
    public static JobOfferEventMessage from(JobOffer offer, JobOfferEventType type, Instant occurredAt) {
        var d = offer.details();
        return new JobOfferEventMessage(UUID.randomUUID().toString(), type.name(), offer.id().toString(),
                offer.companyId().toString(), offer.companyName(), d.title(), d.description(),
                d.jobCategory(), d.specialty(), d.contractType().name(), d.remote(), d.country(),
                d.region(), d.department(), d.city(), d.salaryMin(), d.salaryMax(), occurredAt);
    }
}
