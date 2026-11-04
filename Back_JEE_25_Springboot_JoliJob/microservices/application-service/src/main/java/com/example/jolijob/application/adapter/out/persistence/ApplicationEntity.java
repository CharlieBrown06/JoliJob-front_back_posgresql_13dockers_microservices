package com.example.jolijob.application.adapter.out.persistence;

import com.example.jolijob.application.domain.Application;
import com.example.jolijob.application.domain.ApplicationStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "applications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"job_offer_id", "candidate_id"}))
class ApplicationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "job_offer_id", nullable = false)
    private UUID jobOfferId;
    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;
    private String candidateEmail;
    private String companyEmail;
    private String jobTitle;
    private UUID companyId;
    @Column(columnDefinition = "TEXT")
    private String coverLetter;
    private String cvUrl;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;
    @Column(nullable = false)
    private Instant submittedAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected ApplicationEntity() {
    }

    static ApplicationEntity from(Application application) {
        ApplicationEntity entity = new ApplicationEntity();
        entity.id = application.id();
        entity.jobOfferId = application.jobOfferId();
        entity.candidateId = application.candidateId();
        entity.candidateEmail = application.candidateEmail();
        entity.companyEmail = application.companyEmail();
        entity.jobTitle = application.jobTitle();
        entity.companyId = application.companyId();
        entity.coverLetter = application.coverLetter();
        entity.cvUrl = application.cvUrl();
        entity.status = application.status();
        entity.submittedAt = application.submittedAt();
        entity.updatedAt = application.updatedAt();
        return entity;
    }

    Application toDomain() {
        return new Application(id, jobOfferId, jobTitle, candidateId, candidateEmail, companyId,
                companyEmail, coverLetter, cvUrl, status, submittedAt, updatedAt);
    }
}
