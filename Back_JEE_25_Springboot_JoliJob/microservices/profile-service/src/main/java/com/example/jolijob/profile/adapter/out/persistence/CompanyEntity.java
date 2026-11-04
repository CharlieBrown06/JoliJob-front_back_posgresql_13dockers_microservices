package com.example.jolijob.profile.adapter.out.persistence;

import com.example.jolijob.profile.domain.CompanyDetails;
import com.example.jolijob.profile.domain.CompanyProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "companies")
public class CompanyEntity {
    @Id
    UUID id;
    @Column(nullable = false, unique = true)
    UUID userId;
    @Column(nullable = false)
    String name;
    String siret;
    String description;
    String website;
    String sector;
    String country;
    String city;
    String logoUrl;
    String contactEmail;
    String contactPhone;
    Instant createdAt;
    Instant updatedAt;

    protected CompanyEntity() {
    }

    static CompanyEntity from(CompanyProfile profile) {
        CompanyDetails details = profile.details();
        CompanyEntity entity = new CompanyEntity();
        entity.id = profile.id();
        entity.userId = profile.userId();
        entity.name = details.name();
        entity.siret = details.siret();
        entity.description = details.description();
        entity.website = details.website();
        entity.sector = details.sector();
        entity.country = details.country();
        entity.city = details.city();
        entity.logoUrl = details.logoUrl();
        entity.contactEmail = details.contactEmail();
        entity.contactPhone = details.contactPhone();
        entity.createdAt = profile.createdAt();
        entity.updatedAt = profile.updatedAt();
        return entity;
    }

    CompanyProfile toDomain() {
        return new CompanyProfile(id, userId, new CompanyDetails(name, siret, description, website,
                sector, country, city, logoUrl, contactEmail, contactPhone), createdAt, updatedAt);
    }
}
