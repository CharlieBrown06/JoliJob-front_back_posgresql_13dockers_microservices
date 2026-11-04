package com.example.jolijob.joboffer.adapter.in.rest;

import com.example.jolijob.joboffer.adapter.config.SecurityConfiguration;
import com.example.jolijob.joboffer.application.port.in.JobOfferUseCases;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.example.jolijob.joboffer.OfferFixture.*;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({JobOfferController.class, InternalJobOfferController.class})
@Import(SecurityConfiguration.class)
class JobOfferControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean JobOfferUseCases offers;
    @MockitoBean JwtDecoder decoder;

    private static final String BASE = "/api/v1/job-offers";
    private static final String BODY = """
            {"title":"Java developer","description":"Build services","jobCategory":"IT",
             "specialty":"Backend","contractType":"CDI","remote":true,"country":"France",
             "region":"Ile-de-France","department":"75","city":"Paris","salaryMin":50000,
             "salaryMax":65000,"startDate":"2026-11-01","expiresAt":"2026-12-01"}
            """;

    @BeforeEach
    void setup() {
        when(decoder.decode("company")).thenReturn(token("COMPANY", USER_ID.toString()));
        when(decoder.decode("candidate")).thenReturn(token("CANDIDATE", USER_ID.toString()));
        when(offers.getById(OFFER_ID)).thenReturn(offer());
        when(offers.create(eq(USER_ID), any())).thenReturn(offer());
        when(offers.update(eq(USER_ID), eq(OFFER_ID), any())).thenReturn(offer());
        when(offers.getMyOffers(USER_ID)).thenReturn(List.of(offer()));
    }

    @Test
    void preservesCreateAndUpdateRequestAndFlattenedResponse() throws Exception {
        mvc.perform(post(BASE).header("Authorization", "Bearer company")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", aMapWithSize(19)))
                .andExpect(jsonPath("$.id").value(OFFER_ID.toString()))
                .andExpect(jsonPath("$.companyId").value(COMPANY_ID.toString()))
                .andExpect(jsonPath("$.companyName").value("Joli Company"))
                .andExpect(jsonPath("$.contractType").value("CDI"))
                .andExpect(jsonPath("$.startDate").value("2026-11-01"))
                .andExpect(jsonPath("$.expiresAt").value("2026-12-01"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").value(NOW.toString()));
        verify(offers).create(USER_ID, details());
        mvc.perform(put(BASE + "/" + OFFER_ID).header("Authorization", "Bearer company")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Java developer"));
        verify(offers).update(USER_ID, OFFER_ID, details());
    }

    @Test
    void preservesMineDetailAndDeleteRoutes() throws Exception {
        mvc.perform(get(BASE + "/mine").header("Authorization", "Bearer company"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(OFFER_ID.toString()));
        mvc.perform(get(BASE + "/" + OFFER_ID).header("Authorization", "Bearer candidate"))
                .andExpect(status().isOk());
        mvc.perform(delete(BASE + "/" + OFFER_ID).header("Authorization", "Bearer company"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(offers).getMyOffers(USER_ID);
        verify(offers).delete(USER_ID, OFFER_ID);
    }

    @Test
    void requiresAuthenticationForExternalApiIncludingDetail() throws Exception {
        mvc.perform(get(BASE + "/" + OFFER_ID)).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(offers);
    }

    @Test
    void restrictsAllCompanyOperationsUsingSingularRoleClaim() throws Exception {
        mvc.perform(post(BASE).header("Authorization", "Bearer candidate")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        mvc.perform(put(BASE + "/" + OFFER_ID).header("Authorization", "Bearer candidate")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        mvc.perform(delete(BASE + "/" + OFFER_ID).header("Authorization", "Bearer candidate"))
                .andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/mine").header("Authorization", "Bearer candidate"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(offers);
    }

    @Test
    void validatesRequestAndUserIdentity() throws Exception {
        mvc.perform(post(BASE).header("Authorization", "Bearer company")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"title":" ","description":"","contractType":null}
                                """)).andExpect(status().isBadRequest());
        when(decoder.decode("company")).thenReturn(token("COMPANY", null));
        mvc.perform(get(BASE + "/mine").header("Authorization", "Bearer company"))
                .andExpect(status().isUnauthorized());
        when(decoder.decode("company")).thenReturn(token("COMPANY", "invalid-uuid"));
        mvc.perform(get(BASE + "/mine").header("Authorization", "Bearer company"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(offers);
    }

    @Test
    void preservesOptionalRemoteDefaultWhenOmitted() throws Exception {
        mvc.perform(post(BASE).header("Authorization", "Bearer company")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"title":"Developer","description":"Build services","contractType":"CDI"}
                                """)).andExpect(status().isCreated());
        verify(offers).create(eq(USER_ID), argThat(details -> !details.remote()
                && details.title().equals("Developer") && details.startDate() == null));
    }

    @Test
    void mapsOwnershipAndMissingProfileErrors() throws Exception {
        doThrow(new SecurityException("Not owned")).when(offers).delete(USER_ID, OFFER_ID);
        mvc.perform(delete(BASE + "/" + OFFER_ID).header("Authorization", "Bearer company"))
                .andExpect(status().isForbidden());
        when(offers.getMyOffers(USER_ID)).thenThrow(new IllegalStateException("Missing profile"));
        mvc.perform(get(BASE + "/mine").header("Authorization", "Bearer company"))
                .andExpect(status().isConflict());
    }

    @Test
    void internalContractIsExactlyFourFieldsAndIncludesInactiveOffersForAuthenticatedCallers() throws Exception {
        when(offers.getById(OFFER_ID)).thenReturn(offer().deactivate(NOW));
        mvc.perform(get("/internal/job-offers/" + OFFER_ID).header("Authorization", "Bearer candidate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.id").value(OFFER_ID.toString()))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.companyId").value(COMPANY_ID.toString()))
                .andExpect(jsonPath("$.title").value("Java developer"));
    }

    @Test
    void internalContractReturns404ForMissingOfferAnd400ForInvalidUuid() throws Exception {
        when(offers.getById(OFFER_ID)).thenThrow(new IllegalArgumentException("Offre introuvable"));
        mvc.perform(get("/internal/job-offers/" + OFFER_ID).header("Authorization", "Bearer candidate"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(get("/internal/job-offers/not-a-uuid").header("Authorization", "Bearer candidate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void internalContractRequiresAuthentication() throws Exception {
        mvc.perform(get("/internal/job-offers/" + OFFER_ID)).andExpect(status().isUnauthorized());
    }

    private static Jwt token(String role, String subject) {
        var builder = Jwt.withTokenValue("test-token").header("alg", "RS256")
                .subject(subject == null ? "invalid-uuid" : subject)
                .issuer("http://localhost:8081/realms/jolijob")
                .claim("aud", List.of("jolijob-api"))
                .claim("email", "company@example.com").claim("email_verified", true)
                .claim("realm_access", java.util.Map.of("roles", List.of(role)));
        return builder.build();
    }
}
