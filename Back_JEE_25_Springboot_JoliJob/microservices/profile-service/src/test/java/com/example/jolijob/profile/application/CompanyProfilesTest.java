package com.example.jolijob.profile.application;

import com.example.jolijob.profile.application.port.out.CompanyProfileRepository;
import com.example.jolijob.profile.domain.CompanyDetails;
import com.example.jolijob.profile.domain.CompanyProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyProfilesTest {
    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final UUID USER = UUID.randomUUID();
    private final MemoryRepository repository = new MemoryRepository();
    private CompanyProfiles profiles;

    @BeforeEach
    void setUp() {
        profiles = new CompanyProfiles(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsCompleteProfileForUser() {
        CompanyDetails details = details("Acme", "jobs@acme.org");
        CompanyProfile profile = profiles.create(USER, details);

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
        CompanyProfile original = profiles.create(USER, details("Acme", "jobs@acme.org"));

        assertThatThrownBy(() -> profiles.create(USER, details("Other", null)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(repository.profiles).hasSize(1);
        assertThat(profiles.getByUserId(USER)).isEqualTo(original);
    }

    @Test
    void updatesEveryFieldWhilePreservingIdentityAndCreationTime() {
        CompanyProfile original = profiles.create(USER, details("Acme", "old@acme.org"));
        Instant later = NOW.plusSeconds(60);
        profiles = new CompanyProfiles(repository, Clock.fixed(later, ZoneOffset.UTC));
        CompanyDetails replacement = new CompanyDetails("New name", "new-siret", "new-description",
                "new-site", "new-sector", "new-country", "new-city", "new-logo", "new@mail.org", "new-phone");

        CompanyProfile updated = profiles.update(USER, replacement);

        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.userId()).isEqualTo(USER);
        assertThat(updated.createdAt()).isEqualTo(NOW);
        assertThat(updated.updatedAt()).isEqualTo(later);
        assertThat(updated.details()).isEqualTo(replacement);
        assertThat(profiles.getById(original.id())).isEqualTo(updated);
        assertThat(repository.profiles).hasSize(1);
    }

    @Test
    void permitsClearingOptionalFieldsIncludingContactEmail() {
        profiles.create(USER, details("Acme", "jobs@acme.org"));
        CompanyDetails replacement = new CompanyDetails("Acme", null, null, null, null,
                null, null, null, null, null);

        assertThat(profiles.update(USER, replacement).details()).isEqualTo(replacement);
    }

    @Test
    void isolatesProfilesByUserAndByProfileId() {
        CompanyProfile first = profiles.create(USER, details("Acme", "a@acme.org"));
        UUID otherUser = UUID.randomUUID();
        CompanyProfile second = profiles.create(otherUser, details("Other", "b@acme.org"));

        profiles.update(otherUser, details("Updated", "c@acme.org"));

        assertThat(profiles.getByUserId(USER)).isEqualTo(first);
        assertThat(profiles.getById(second.id()).details().contactEmail()).isEqualTo("c@acme.org");
        assertThat(second.id()).isNotEqualTo(first.id()).isNotEqualTo(otherUser);
    }

    @Test
    void missingOwnProfileIsConflict() {
        assertThatThrownBy(() -> profiles.getByUserId(USER))
                .isInstanceOf(OwnProfileNotFoundException.class);
    }

    @Test
    void updatingMissingProfileDoesNotCreateIt() {
        assertThatThrownBy(() -> profiles.update(USER, details("Acme", null)))
                .isInstanceOf(OwnProfileNotFoundException.class);
        assertThat(repository.profiles).isEmpty();
    }

    @Test
    void missingProfileIdIsNotFound() {
        assertThatThrownBy(() -> profiles.getById(UUID.randomUUID()))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void requiresCompanyName() {
        assertThatThrownBy(() -> details(" ", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> details(null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    private CompanyDetails details(String name, String email) {
        return new CompanyDetails(name, "12345678901234", "Description", "https://acme.org",
                "Technology", "France", "Paris", "logo", email, "0123456789");
    }

    private static final class MemoryRepository implements CompanyProfileRepository {
        private final Map<UUID, CompanyProfile> profiles = new HashMap<>();

        @Override
        public Optional<CompanyProfile> findByUserId(UUID userId) {
            return profiles.values().stream().filter(profile -> profile.userId().equals(userId)).findFirst();
        }

        @Override
        public Optional<CompanyProfile> findById(UUID id) {
            return Optional.ofNullable(profiles.get(id));
        }

        @Override
        public CompanyProfile save(CompanyProfile profile) {
            profiles.put(profile.id(), profile);
            return profile;
        }
    }
}
