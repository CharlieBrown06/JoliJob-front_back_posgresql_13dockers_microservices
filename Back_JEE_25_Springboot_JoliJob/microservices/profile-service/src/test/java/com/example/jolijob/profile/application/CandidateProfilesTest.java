package com.example.jolijob.profile.application;

import com.example.jolijob.profile.application.port.out.CandidateProfileRepository;
import com.example.jolijob.profile.domain.CandidateDetails;
import com.example.jolijob.profile.domain.CandidateProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidateProfilesTest {
    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final UUID USER = UUID.randomUUID();
    private final MemoryRepository repository = new MemoryRepository();
    private CandidateProfiles profiles;

    @BeforeEach
    void setUp() {
        profiles = new CandidateProfiles(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsCompleteProfileForUser() {
        CandidateDetails details = details("Ada", "https://example.org/cv.pdf");
        CandidateProfile profile = profiles.create(USER, details);

        assertThat(profile.id()).isNotNull();
        assertThat(profile.userId()).isEqualTo(USER);
        assertThat(profile.details()).isEqualTo(details);
        assertThat(profile.createdAt()).isEqualTo(NOW);
        assertThat(profile.updatedAt()).isEqualTo(NOW);
        assertThat(profiles.getById(profile.id())).isEqualTo(profile);
        assertThat(profiles.getByUserId(USER)).isEqualTo(profile);
    }

    @Test
    void rejectsDuplicateWithoutOverwritingOriginal() {
        CandidateProfile original = profiles.create(USER, details("Ada", "cv"));

        assertThatThrownBy(() -> profiles.create(USER, details("Other", null)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(repository.profiles).hasSize(1);
        assertThat(profiles.getByUserId(USER)).isEqualTo(original);
    }

    @Test
    void updatesEveryFieldWhilePreservingIdentityAndCreationTime() {
        CandidateProfile original = profiles.create(USER, details("Ada", "old-cv"));
        Instant later = NOW.plusSeconds(60);
        profiles = new CandidateProfiles(repository, Clock.fixed(later, ZoneOffset.UTC));
        CandidateDetails replacement = new CandidateDetails("Grace", "Hopper", "new-phone", "New York",
                "US", "Admiral", "new-summary", "new-cv", "new-linkedin", "new-portfolio",
                LocalDate.of(1906, 12, 9), false, true);

        CandidateProfile updated = profiles.update(USER, replacement);

        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.userId()).isEqualTo(USER);
        assertThat(updated.createdAt()).isEqualTo(NOW);
        assertThat(updated.updatedAt()).isEqualTo(later);
        assertThat(updated.details()).isEqualTo(replacement);
        assertThat(profiles.getById(original.id())).isEqualTo(updated);
        assertThat(repository.profiles).hasSize(1);
    }

    @Test
    void permitsClearingOptionalFieldsAndBooleanFlags() {
        profiles.create(USER, details("Ada", "cv"));
        CandidateDetails replacement = new CandidateDetails("Ada", "Lovelace", null, null, null,
                null, null, null, null, null, null, false, false);

        assertThat(profiles.update(USER, replacement).details()).isEqualTo(replacement);
    }

    @Test
    void isolatesProfilesByUserAndByProfileId() {
        CandidateProfile first = profiles.create(USER, details("Ada", "cv-a"));
        UUID otherUser = UUID.randomUUID();
        CandidateProfile second = profiles.create(otherUser, details("Grace", "cv-b"));

        profiles.update(otherUser, details("Updated", "cv-c"));

        assertThat(profiles.getByUserId(USER)).isEqualTo(first);
        assertThat(profiles.getById(second.id()).details().cvUrl()).isEqualTo("cv-c");
        assertThat(second.id()).isNotEqualTo(first.id()).isNotEqualTo(otherUser);
    }

    @Test
    void missingOwnProfileIsConflict() {
        assertThatThrownBy(() -> profiles.getByUserId(USER))
                .isInstanceOf(OwnProfileNotFoundException.class);
    }

    @Test
    void updatingMissingProfileDoesNotCreateIt() {
        assertThatThrownBy(() -> profiles.update(USER, details("Ada", null)))
                .isInstanceOf(OwnProfileNotFoundException.class);
        assertThat(repository.profiles).isEmpty();
    }

    @Test
    void missingProfileIdIsNotFound() {
        assertThatThrownBy(() -> profiles.getById(UUID.randomUUID()))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void requiresBothNames() {
        assertThatThrownBy(() -> details(" ", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CandidateDetails("Ada", null, null, null, null,
                null, null, null, null, null, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private CandidateDetails details(String firstName, String cvUrl) {
        return new CandidateDetails(firstName, "Lovelace", "0123456789", "London", "UK",
                "Engineer", "Summary", cvUrl, "linkedin", "portfolio",
                LocalDate.of(1990, 1, 1), true, false);
    }

    private static final class MemoryRepository implements CandidateProfileRepository {
        private final Map<UUID, CandidateProfile> profiles = new HashMap<>();

        @Override
        public Optional<CandidateProfile> findByUserId(UUID userId) {
            return profiles.values().stream().filter(profile -> profile.userId().equals(userId)).findFirst();
        }

        @Override
        public Optional<CandidateProfile> findById(UUID id) {
            return Optional.ofNullable(profiles.get(id));
        }

        @Override
        public CandidateProfile save(CandidateProfile profile) {
            profiles.put(profile.id(), profile);
            return profile;
        }
    }
}
