package com.example.jolijob.indexer.adapter.out.elasticsearch;

import com.example.jolijob.indexer.application.port.out.JobOfferIndexPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElasticsearchJobOfferIndexAdapterTest {

    private final JobOfferDocumentRepository repository = mock(JobOfferDocumentRepository.class);
    private final ElasticsearchJobOfferIndexAdapter adapter = new ElasticsearchJobOfferIndexAdapter(repository);

    @Test
    void updatePreservesCreationMetadataAndCompanyFields() {
        Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
        when(repository.findById("offer-1")).thenReturn(Optional.of(document(createdAt, true)));

        adapter.update(new JobOfferIndexPort.JobOfferUpdate(
                "offer-1", "Nouveau titre", "Nouvelle description", "IT", "Java",
                "CDI", true, "France", "Région", "Département", "Ville",
                BigDecimal.valueOf(45000), BigDecimal.valueOf(60000)));

        ArgumentCaptor<JobOfferDocument> saved = ArgumentCaptor.forClass(JobOfferDocument.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().title()).isEqualTo("Nouveau titre");
        assertThat(saved.getValue().companyName()).isEqualTo("JoliJob");
        assertThat(saved.getValue().active()).isTrue();
        assertThat(saved.getValue().createdAt()).isEqualTo(createdAt);
    }

    @Test
    void updateDoesNotCreateAnIndexEntryWhenOfferIsMissing() {
        when(repository.findById("offer-1")).thenReturn(Optional.empty());

        adapter.update(new JobOfferIndexPort.JobOfferUpdate(
                "offer-1", "Titre", "Description", "IT", "Java", "CDI",
                false, "France", "Région", "Département", "Ville",
                null, null));

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deletionOnlyChangesActiveFlagAndPreservesAllOtherFields() {
        Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
        when(repository.findById("offer-1")).thenReturn(Optional.of(document(createdAt, true)));

        adapter.deactivate("offer-1");

        verify(repository).save(document(createdAt, false));
        verify(repository, never()).deleteById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deletionOfMissingOfferDoesNotCreateOrDeleteDocuments() {
        when(repository.findById("offer-1")).thenReturn(Optional.empty());

        adapter.deactivate("offer-1");

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(repository, never()).deleteById(org.mockito.ArgumentMatchers.any());
    }

    private JobOfferDocument document(Instant createdAt, boolean active) {
        return new JobOfferDocument(
                "offer-1", "company-1", "JoliJob", "Ancien titre", "Ancienne description",
                "IT", "Java", "CDI", false, "France", "Région", "Département",
                "Ville", BigDecimal.valueOf(40000), BigDecimal.valueOf(50000), active, createdAt);
    }
}
