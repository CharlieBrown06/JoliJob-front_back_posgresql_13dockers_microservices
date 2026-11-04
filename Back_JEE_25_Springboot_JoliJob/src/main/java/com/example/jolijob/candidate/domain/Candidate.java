package com.example.jolijob.candidate.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "candidates")
@Getter
@Setter
@NoArgsConstructor
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String phone;
    private String city;
    private String country;
    private String jobTitle;
    private String summary;
    private String cvUrl;
    private String linkedinUrl;
    private String portfolioUrl;
    private LocalDate birthDate;
    private boolean openToRemote;
    private boolean openToRelocation;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Candidate(UUID userId, String firstName, String lastName) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
