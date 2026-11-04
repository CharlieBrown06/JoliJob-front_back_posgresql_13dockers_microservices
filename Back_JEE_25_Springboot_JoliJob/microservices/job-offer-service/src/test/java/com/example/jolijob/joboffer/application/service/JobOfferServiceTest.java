package com.example.jolijob.joboffer.application.service;

import com.example.jolijob.joboffer.application.port.out.CompanyPort;
import com.example.jolijob.joboffer.application.port.out.JobOfferEventPort;
import com.example.jolijob.joboffer.application.port.out.JobOfferRepositoryPort;
import com.example.jolijob.joboffer.domain.JobOffer;
import com.example.jolijob.joboffer.domain.JobOfferDetails;
import com.example.jolijob.joboffer.domain.ContractType;
import com.example.jolijob.joboffer.domain.JobOfferEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.jolijob.joboffer.OfferFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JobOfferServiceTest {
    private final JobOfferRepositoryPort repository = mock(JobOfferRepositoryPort.class);
    private final CompanyPort companies = mock(CompanyPort.class);
    private final JobOfferEventPort events = mock(JobOfferEventPort.class);
    private final JobOfferService service = new JobOfferService(repository, companies, events,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setup() {
        when(companies.findByUserId(USER_ID)).thenReturn(Optional.of(
                new CompanyPort.Company(COMPANY_ID, USER_ID, "Joli Company")));
        doAnswer(invocation -> invocation.getArgument(0)).when(repository).save(any());
        when(repository.findById(OFFER_ID)).thenReturn(Optional.of(offer()));
    }

    @Test
    void createsActiveOfferWithCompanyIdentityAndPublishesSavedOffer() {
        JobOffer created = service.create(USER_ID, details());
        assertThat(created.id()).isNotNull();
        assertThat(created.companyId()).isEqualTo(COMPANY_ID);
        assertThat(created.companyName()).isEqualTo("Joli Company");
        assertThat(created.details()).isEqualTo(details());
        assertThat(created.active()).isTrue();
        assertThat(created.createdAt()).isEqualTo(NOW);
        var order = inOrder(repository, events);
        order.verify(repository).save(created);
        order.verify(events).publish(created, JobOfferEventType.CREATED);
    }

    @Test
    void updatesOwnedOfferWithoutChangingIdentityOrReactivatingIt() {
        var old = offer().deactivate(NOW.minusSeconds(10));
        when(repository.findById(OFFER_ID)).thenReturn(Optional.of(old));
        var replacement = new JobOfferDetails("Updated title", "Updated description", "Engineering",
                "Frontend", ContractType.CDD, false, "Germany", "Berlin", null, "Berlin",
                null, null, null, null);
        var updated = service.update(USER_ID, OFFER_ID, replacement);
        assertThat(updated.details()).isEqualTo(replacement);
        assertThat(updated.id()).isEqualTo(old.id());
        assertThat(updated.companyId()).isEqualTo(old.companyId());
        assertThat(updated.createdAt()).isEqualTo(old.createdAt());
        assertThat(updated.updatedAt()).isEqualTo(NOW);
        assertThat(updated.active()).isFalse();
        verify(events).publish(updated, JobOfferEventType.UPDATED);
    }

    @Test
    void softDeletesOwnedOfferAndPublishesDeletedEvent() {
        service.delete(USER_ID, OFFER_ID);
        var deleted = offer().deactivate(NOW);
        verify(repository).save(deleted);
        verify(events).publish(deleted, JobOfferEventType.DELETED);
    }

    @Test
    void rejectsUpdatesAndDeletesForAnotherCompanyBeforeWritingOrPublishing() {
        when(companies.findByUserId(USER_ID)).thenReturn(Optional.of(
                new CompanyPort.Company(UUID.randomUUID(), USER_ID, "Other company")));
        assertThatThrownBy(() -> service.update(USER_ID, OFFER_ID, details())).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> service.delete(USER_ID, OFFER_ID)).isInstanceOf(SecurityException.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void requiresCompanyProfileForAllCompanyOperations() {
        when(companies.findByUserId(USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(USER_ID, details())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.update(USER_ID, OFFER_ID, details())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.delete(USER_ID, OFFER_ID)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.getMyOffers(USER_ID)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(events);
        verify(repository, never()).save(any());
    }

    @Test
    void listsOnlyCurrentCompanyActiveOffers() {
        when(repository.findActiveByCompanyId(COMPANY_ID)).thenReturn(List.of(offer()));
        assertThat(service.getMyOffers(USER_ID)).containsExactly(offer());
        verify(repository).findActiveByCompanyId(COMPANY_ID);
    }

    @Test
    void readsInactiveOffersForInternalConsumersAndReportsMissingOffers() {
        var inactive = offer().deactivate(NOW);
        when(repository.findById(OFFER_ID)).thenReturn(Optional.of(inactive));
        assertThat(service.getById(OFFER_ID)).isEqualTo(inactive);
        when(repository.findById(OFFER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(OFFER_ID)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesNotPublishWhenPersistenceFailsAndDoesNotHidePublicationFailures() {
        doThrow(new IllegalStateException("database unavailable")).when(repository).save(any());
        assertThatThrownBy(() -> service.create(USER_ID, details())).hasMessage("database unavailable");
        verifyNoInteractions(events);
        doAnswer(invocation -> invocation.getArgument(0)).when(repository).save(any());
        doThrow(new IllegalStateException("broker unavailable")).when(events).publish(any(), any());
        assertThatThrownBy(() -> service.delete(USER_ID, OFFER_ID)).hasMessage("broker unavailable");
    }
}
