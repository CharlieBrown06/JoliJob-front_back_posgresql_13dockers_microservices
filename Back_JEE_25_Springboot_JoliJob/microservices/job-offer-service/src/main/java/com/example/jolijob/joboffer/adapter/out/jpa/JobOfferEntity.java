package com.example.jolijob.joboffer.adapter.out.jpa;

import com.example.jolijob.joboffer.domain.ContractType;
import com.example.jolijob.joboffer.domain.JobOffer;
import com.example.jolijob.joboffer.domain.JobOfferDetails;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "job_offers")
public class JobOfferEntity {
    @Id
    UUID id;
    @Column(nullable = false)
    UUID companyId;
    String companyName;
    @Column(nullable = false)
    String title;
    @Column(columnDefinition = "TEXT")
    String description;
    String jobCategory;
    String specialty;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ContractType contractType;
    boolean remote;
    String country;
    String region;
    String department;
    String city;
    BigDecimal salaryMin;
    BigDecimal salaryMax;
    LocalDate startDate;
    LocalDate expiresAt;
    boolean active;
    Instant createdAt;
    Instant updatedAt;

    protected JobOfferEntity() {}

    static JobOfferEntity from(JobOffer offer) {
        var entity = new JobOfferEntity();
        var d = offer.details();
        entity.id = offer.id();
        entity.companyId = offer.companyId();
        entity.companyName = offer.companyName();
        entity.title = d.title();
        entity.description = d.description();
        entity.jobCategory = d.jobCategory();
        entity.specialty = d.specialty();
        entity.contractType = d.contractType();
        entity.remote = d.remote();
        entity.country = d.country();
        entity.region = d.region();
        entity.department = d.department();
        entity.city = d.city();
        entity.salaryMin = d.salaryMin();
        entity.salaryMax = d.salaryMax();
        entity.startDate = d.startDate();
        entity.expiresAt = d.expiresAt();
        entity.active = offer.active();
        entity.createdAt = offer.createdAt();
        entity.updatedAt = offer.updatedAt();
        return entity;
    }

    JobOffer toDomain() {
        return new JobOffer(id, companyId, companyName,
                new JobOfferDetails(title, description, jobCategory, specialty, contractType,
                        remote, country, region, department, city, salaryMin, salaryMax, startDate, expiresAt),
                active, createdAt, updatedAt);
    }
}
