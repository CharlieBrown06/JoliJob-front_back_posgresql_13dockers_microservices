package com.example.jolijob.joboffer;

import com.example.jolijob.joboffer.domain.ContractType;
import com.example.jolijob.joboffer.domain.JobOffer;
import com.example.jolijob.joboffer.domain.JobOfferDetails;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class OfferFixture {
    public static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID OFFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");

    private OfferFixture() {}

    public static JobOfferDetails details() {
        return new JobOfferDetails("Java developer", "Build services", "IT", "Backend",
                ContractType.CDI, true, "France", "Ile-de-France", "75", "Paris",
                new BigDecimal("50000"), new BigDecimal("65000"),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1));
    }

    public static JobOffer offer() {
        return new JobOffer(OFFER_ID, COMPANY_ID, "Joli Company", details(), true, NOW, NOW);
    }
}
