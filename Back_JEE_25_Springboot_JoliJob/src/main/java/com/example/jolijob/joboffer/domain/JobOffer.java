package com.example.jolijob.joboffer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "job_offers")
@Getter
@Setter
@NoArgsConstructor
public class JobOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** ID de l'entreprise (Company.id) */
    @Column(nullable = false)
    private UUID companyId;

    /** Cache du nom entreprise pour éviter les jointures dans les events */
    private String companyName;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String jobCategory;
    private String specialty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractType contractType;

    private boolean remote;
    private String country;
    private String region;
    private String department;
    private String city;

    private BigDecimal salaryMin;
    private BigDecimal salaryMax;

    private LocalDate startDate;
    private LocalDate expiresAt;

    private boolean active = true;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
