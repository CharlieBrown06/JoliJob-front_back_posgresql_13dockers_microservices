package com.example.jolijob.company.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Correspond à AppUser.id du compte COMPANY */
    @Column(unique = true, nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    private String siret;
    private String description;
    private String website;
    private String sector;
    private String country;
    private String city;
    private String logoUrl;
    private String contactEmail;
    private String contactPhone;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Company(UUID userId, String name, String contactEmail) {
        this.userId = userId;
        this.name = name;
        this.contactEmail = contactEmail;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
