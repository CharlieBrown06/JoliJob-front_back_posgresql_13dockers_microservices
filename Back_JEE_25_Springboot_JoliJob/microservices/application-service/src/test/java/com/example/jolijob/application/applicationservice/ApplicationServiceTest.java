package com.example.jolijob.application.applicationservice;

import com.example.jolijob.application.domain.Actor;
import com.example.jolijob.application.domain.Application;
import com.example.jolijob.application.domain.ApplicationEvent;
import com.example.jolijob.application.domain.ApplicationStatus;
import com.example.jolijob.application.port.out.ApplicationEventPort;
import com.example.jolijob.application.port.out.ApplicationRepositoryPort;
import com.example.jolijob.application.port.out.CandidateProfilePort;
import com.example.jolijob.application.port.out.CompanyProfilePort;
import com.example.jolijob.application.port.out.JobOfferPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID OFFER_ID = UUID.randomUUID();
    private static final UUID APPLICATION_ID = UUID.randomUUID();

    @Mock
    private ApplicationRepositoryPort applications;
    @Mock
    private CandidateProfilePort candidates;
    @Mock
    private CompanyProfilePort companies;
    @Mock
    private JobOfferPort jobOffers;
    @Mock
    private ApplicationEventPort events;
    @InjectMocks
    private ApplicationService service;

    private Actor candidate;

    @BeforeEach
    void setUp() {
        candidate = new Actor(USER_ID, "candidate@example.test", "CANDIDATE");
    }

    @Test
    void applyUsesProfileCvWhenRequestDoesNotProvideOneAndPublishesSubmittedEvent() {
        when(candidates.findByUserId(USER_ID))
                .thenReturn(Optional.of(new CandidateProfilePort.CandidateProfile(CANDIDATE_ID, "profile-cv.pdf")));
        when(jobOffers.findById(OFFER_ID))
                .thenReturn(Optional.of(new JobOfferPort.JobOffer(OFFER_ID, true, COMPANY_ID, "Developer")));
        when(companies.findById(COMPANY_ID))
                .thenReturn(Optional.of(new CompanyProfilePort.CompanyProfile(COMPANY_ID, "company@example.test")));
        when(applications.save(any())).thenAnswer(invocation -> {
            Application application = invocation.getArgument(0);
            return new Application(APPLICATION_ID, application.jobOfferId(), application.jobTitle(),
                    application.candidateId(), application.candidateEmail(), application.companyId(),
                    application.companyEmail(), application.coverLetter(), application.cvUrl(),
                    application.status(), application.submittedAt(), application.updatedAt());
        });

        Application result = service.apply(candidate, OFFER_ID, "Hello", null);

        assertEquals(APPLICATION_ID, result.id());
        assertEquals("profile-cv.pdf", result.cvUrl());
        assertEquals("company@example.test", result.companyEmail());
        assertEquals(ApplicationStatus.SUBMITTED, result.status());
        ArgumentCaptor<ApplicationEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(events).publish(eventCaptor.capture());
        ApplicationEvent event = eventCaptor.getValue();
        assertEquals("SUBMITTED", event.type());
        assertEquals(APPLICATION_ID.toString(), event.applicationId());
        assertEquals(OFFER_ID.toString(), event.jobOfferId());
        assertEquals("candidate@example.test", event.candidateEmail());
        assertEquals("company@example.test", event.companyEmail());
        assertNotNull(event.eventId());
        assertDoesNotThrow(() -> Instant.parse(event.occurredAt()));
    }

    @Test
    void updateStatusRejectsCompanyThatDoesNotOwnApplication() {
        UUID otherCompanyId = UUID.randomUUID();
        Actor companyActor = new Actor(USER_ID, "owner@example.test", "COMPANY");
        Application application = existingApplication(otherCompanyId, CANDIDATE_ID);
        when(companies.findByUserId(USER_ID))
                .thenReturn(Optional.of(new CompanyProfilePort.CompanyProfile(COMPANY_ID, "company@example.test")));
        when(applications.findById(APPLICATION_ID)).thenReturn(Optional.of(application));

        assertThrows(SecurityException.class,
                () -> service.updateStatus(companyActor, APPLICATION_ID, ApplicationStatus.ACCEPTED));
        verify(applications, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void withdrawRejectsCandidateThatDoesNotOwnApplication() {
        when(candidates.findByUserId(USER_ID))
                .thenReturn(Optional.of(new CandidateProfilePort.CandidateProfile(CANDIDATE_ID, null)));
        when(applications.findById(APPLICATION_ID))
                .thenReturn(Optional.of(existingApplication(COMPANY_ID, UUID.randomUUID())));

        assertThrows(SecurityException.class, () -> service.withdraw(candidate, APPLICATION_ID));
        verify(applications, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void applyRejectsActorsWithoutCandidateRole() {
        Actor companyActor = new Actor(USER_ID, "company@example.test", "COMPANY");

        assertThrows(SecurityException.class, () -> service.apply(companyActor, OFFER_ID, null, null));
        verifyNoInteractions(candidates, jobOffers, applications, events);
    }

    private static Application existingApplication(UUID companyId, UUID candidateId) {
        Instant now = Instant.now();
        return new Application(APPLICATION_ID, OFFER_ID, "Developer", candidateId, "candidate@example.test",
                companyId, "company@example.test", "Cover letter", "cv.pdf",
                ApplicationStatus.SUBMITTED, now, now);
    }
}
