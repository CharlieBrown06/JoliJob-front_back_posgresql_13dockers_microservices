package com.example.jolijob.profile.adapter.out.persistence;

import com.example.jolijob.profile.domain.CandidateDetails;
import com.example.jolijob.profile.domain.CandidateProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "candidates")
public class CandidateEntity {
    @Id
    UUID id;
    @Column(nullable = false, unique = true)
    UUID userId;
    @Column(nullable = false)
    String firstName;
    @Column(nullable = false)
    String lastName;
    String phone;
    String city;
    String country;
    String jobTitle;
    String summary;
    String cvUrl;
    String linkedinUrl;
    String portfolioUrl;
    LocalDate birthDate;
    boolean openToRemote;
    boolean openToRelocation;
    Instant createdAt;
    Instant updatedAt;

    protected CandidateEntity() {
    }

    static CandidateEntity from(CandidateProfile profile) {
        CandidateDetails details = profile.details();
        CandidateEntity entity = new CandidateEntity();
        entity.id = profile.id();
        entity.userId = profile.userId();
        entity.firstName = details.firstName();
        entity.lastName = details.lastName();
        entity.phone = details.phone();
        entity.city = details.city();
        entity.country = details.country();
        entity.jobTitle = details.jobTitle();
        entity.summary = details.summary();
        entity.cvUrl = details.cvUrl();
        entity.linkedinUrl = details.linkedinUrl();
        entity.portfolioUrl = details.portfolioUrl();
        entity.birthDate = details.birthDate();
        entity.openToRemote = details.openToRemote();
        entity.openToRelocation = details.openToRelocation();
        entity.createdAt = profile.createdAt();
        entity.updatedAt = profile.updatedAt();
        return entity;
    }

    CandidateProfile toDomain() {
        return new CandidateProfile(id, userId, new CandidateDetails(firstName, lastName, phone, city,
                country, jobTitle, summary, cvUrl, linkedinUrl, portfolioUrl, birthDate,
                openToRemote, openToRelocation), createdAt, updatedAt);
    }
}
