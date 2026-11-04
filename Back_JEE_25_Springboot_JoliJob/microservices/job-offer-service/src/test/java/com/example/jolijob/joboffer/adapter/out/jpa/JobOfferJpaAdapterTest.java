package com.example.jolijob.joboffer.adapter.out.jpa;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.example.jolijob.joboffer.OfferFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobOfferJpaAdapterTest {
    private final SpringDataJobOfferRepository repository = mock(SpringDataJobOfferRepository.class);
    private final JobOfferJpaAdapter adapter = new JobOfferJpaAdapter(repository);

    @Test
    void mapsAllFieldsWithoutLosingDatesIdentityOrInactiveStatus() {
        var inactive = offer().deactivate(NOW.plusSeconds(60));
        assertThat(JobOfferEntity.from(inactive).toDomain()).isEqualTo(inactive);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(adapter.save(inactive)).isEqualTo(inactive);
    }

    @Test
    void readsIncludingInactiveAndQueriesOnlyActiveCompanyOffersForMine() {
        when(repository.findById(OFFER_ID)).thenReturn(Optional.of(JobOfferEntity.from(offer().deactivate(NOW))));
        when(repository.findByCompanyIdAndActiveTrue(COMPANY_ID)).thenReturn(List.of(JobOfferEntity.from(offer())));
        assertThat(adapter.findById(OFFER_ID)).contains(offer().deactivate(NOW));
        assertThat(adapter.findActiveByCompanyId(COMPANY_ID)).containsExactly(offer());
        verify(repository).findByCompanyIdAndActiveTrue(COMPANY_ID);
    }
}
