package com.example.jolijob.application.applicationservice;

import com.example.jolijob.application.domain.Actor;
import com.example.jolijob.application.domain.Application;
import com.example.jolijob.application.domain.ApplicationEvent;
import com.example.jolijob.application.domain.ApplicationStatus;
import com.example.jolijob.application.port.in.ApplicationUseCase;
import com.example.jolijob.application.port.out.ApplicationEventPort;
import com.example.jolijob.application.port.out.ApplicationRepositoryPort;
import com.example.jolijob.application.port.out.CandidateProfilePort;
import com.example.jolijob.application.port.out.CompanyProfilePort;
import com.example.jolijob.application.port.out.JobOfferPort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ApplicationService implements ApplicationUseCase {
    private static final String CANDIDATE = "CANDIDATE";
    private static final String COMPANY = "COMPANY";

    private final ApplicationRepositoryPort applications;
    private final CandidateProfilePort candidates;
    private final CompanyProfilePort companies;
    private final JobOfferPort jobOffers;
    private final ApplicationEventPort events;

    public ApplicationService(ApplicationRepositoryPort applications,
                              CandidateProfilePort candidates,
                              CompanyProfilePort companies,
                              JobOfferPort jobOffers,
                              ApplicationEventPort events) {
        this.applications = applications;
        this.candidates = candidates;
        this.companies = companies;
        this.jobOffers = jobOffers;
        this.events = events;
    }

    @Override
    public Application apply(Actor actor, UUID jobOfferId, String coverLetter, String cvUrl) {
        requireRole(actor, CANDIDATE);
        var candidate = candidates.findByUserId(actor.userId())
                .orElseThrow(() -> new IllegalStateException("Créez votre profil candidat d'abord"));
        var offer = jobOffers.findById(jobOfferId)
                .orElseThrow(() -> new IllegalArgumentException("Offre introuvable"));
        if (!offer.active()) {
            throw new IllegalStateException("Cette offre n'est plus active");
        }
        if (applications.existsByJobOfferIdAndCandidateId(offer.id(), candidate.id())) {
            throw new IllegalStateException("Vous avez déjà postulé à cette offre");
        }

        String companyEmail = companies.findById(offer.companyId())
                .map(CompanyProfilePort.CompanyProfile::contactEmail)
                .orElse(null);
        Instant now = Instant.now();
        Application application = new Application(null, offer.id(), offer.title(), candidate.id(),
                actor.email(), offer.companyId(), companyEmail, coverLetter,
                cvUrl != null ? cvUrl : candidate.cvUrl(), ApplicationStatus.SUBMITTED, now, now);
        Application saved = applications.save(application);
        publish(saved, "SUBMITTED");
        return saved;
    }

    @Override
    public Application updateStatus(Actor actor, UUID applicationId, ApplicationStatus status) {
        requireRole(actor, COMPANY);
        var company = companies.findByUserId(actor.userId())
                .orElseThrow(() -> new IllegalStateException("Profil entreprise introuvable"));
        Application application = findApplication(applicationId);
        if (!application.companyId().equals(company.id())) {
            throw new SecurityException("Accès refusé");
        }
        Application saved = applications.save(application.withStatus(status, Instant.now()));
        publish(saved, "STATUS_CHANGED");
        return saved;
    }

    @Override
    public Application withdraw(Actor actor, UUID applicationId) {
        requireRole(actor, CANDIDATE);
        var candidate = candidates.findByUserId(actor.userId())
                .orElseThrow(() -> new IllegalStateException("Profil candidat introuvable"));
        Application application = findApplication(applicationId);
        if (!application.candidateId().equals(candidate.id())) {
            throw new SecurityException("Accès refusé");
        }
        Application saved = applications.save(application.withStatus(ApplicationStatus.WITHDRAWN, Instant.now()));
        publish(saved, "WITHDRAWN");
        return saved;
    }

    @Override
    public List<Application> getMyCandidateApplications(Actor actor) {
        requireRole(actor, CANDIDATE);
        var candidate = candidates.findByUserId(actor.userId())
                .orElseThrow(() -> new IllegalStateException("Profil candidat introuvable"));
        return applications.findByCandidateId(candidate.id());
    }

    @Override
    public List<Application> getOfferApplications(Actor actor, UUID jobOfferId) {
        requireRole(actor, COMPANY);
        var company = companies.findByUserId(actor.userId())
                .orElseThrow(() -> new IllegalStateException("Profil entreprise introuvable"));
        var offer = jobOffers.findById(jobOfferId)
                .orElseThrow(() -> new IllegalArgumentException("Offre introuvable"));
        if (!offer.companyId().equals(company.id())) {
            throw new SecurityException("Accès refusé");
        }
        return applications.findByJobOfferId(jobOfferId);
    }

    private Application findApplication(UUID id) {
        return applications.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable"));
    }

    private void publish(Application application, String type) {
        events.publish(new ApplicationEvent(UUID.randomUUID().toString(), type,
                application.id().toString(), application.jobOfferId().toString(), application.jobTitle(),
                application.candidateId().toString(), application.candidateEmail(),
                application.companyId() == null ? null : application.companyId().toString(),
                application.companyEmail(), application.status().name(), Instant.now().toString()));
    }

    private static void requireRole(Actor actor, String requiredRole) {
        if (actor == null || actor.role() == null || !requiredRole.equals(actor.role())) {
            throw new SecurityException("Accès refusé");
        }
    }
}
