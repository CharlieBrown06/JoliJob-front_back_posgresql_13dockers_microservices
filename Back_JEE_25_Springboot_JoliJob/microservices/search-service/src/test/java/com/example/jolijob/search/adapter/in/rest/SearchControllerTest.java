package com.example.jolijob.search.adapter.in.rest;

import com.example.jolijob.search.application.JobOffer;
import com.example.jolijob.search.application.SearchRequest;
import com.example.jolijob.search.application.SearchResult;
import com.example.jolijob.search.application.SearchService;
import com.example.jolijob.search.application.port.JobOfferSearchPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchControllerTest {

    private final AtomicReference<SearchRequest> captured = new AtomicReference<>();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        JobOfferSearchPort port = request -> {
            captured.set(request);
            return new SearchResult(List.of(new JobOffer("offer-1", "company-1", "Acme",
                    "Developer", "Build APIs", "IT", "Java", "CDI", false,
                    "France", "IDF", "75", "Paris", new BigDecimal("45000"),
                    new BigDecimal("65000"), true, "2026-10-20T12:00:00Z")),
                    42, request.page(), request.size());
        };
        mvc = MockMvcBuilders.standaloneSetup(new SearchController(new SearchService(port))).build();
    }

    @Test
    void publicGetPreservesDefaultsAndResponseFields() throws Exception {
        mvc.perform(get("/api/v1/search/job-offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits").value(42))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.results[0].id").value("offer-1"))
                .andExpect(jsonPath("$.results[0].companyId").value("company-1"))
                .andExpect(jsonPath("$.results[0].companyName").value("Acme"))
                .andExpect(jsonPath("$.results[0].title").value("Developer"))
                .andExpect(jsonPath("$.results[0].description").value("Build APIs"))
                .andExpect(jsonPath("$.results[0].jobCategory").value("IT"))
                .andExpect(jsonPath("$.results[0].specialty").value("Java"))
                .andExpect(jsonPath("$.results[0].contractType").value("CDI"))
                .andExpect(jsonPath("$.results[0].remote").value(false))
                .andExpect(jsonPath("$.results[0].country").value("France"))
                .andExpect(jsonPath("$.results[0].region").value("IDF"))
                .andExpect(jsonPath("$.results[0].department").value("75"))
                .andExpect(jsonPath("$.results[0].city").value("Paris"))
                .andExpect(jsonPath("$.results[0].salaryMin").value(45000))
                .andExpect(jsonPath("$.results[0].salaryMax").value(65000))
                .andExpect(jsonPath("$.results[0].active").value(true))
                .andExpect(jsonPath("$.results[0].createdAt").value("2026-10-20T12:00:00Z"));
        assertThat(captured.get()).isEqualTo(new SearchRequest(null, null, null, null, null,
                null, null, null, null, null, null, 0, 20));
    }

    @Test
    void bindsAllExistingQueryParameters() throws Exception {
        mvc.perform(get("/api/v1/search/job-offers")
                        .param("keyword", "developer").param("jobCategory", "IT")
                        .param("specialty", "Java").param("contractType", "CDI")
                        .param("remote", "false").param("country", "France")
                        .param("region", "IDF").param("department", "75").param("city", "Paris")
                        .param("salaryMin", "45000.50").param("salaryMax", "65000.75")
                        .param("page", "2").param("size", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(15));
        assertThat(captured.get()).isEqualTo(new SearchRequest("developer", "IT", "Java", "CDI",
                false, "France", "IDF", "75", "Paris", new BigDecimal("45000.50"),
                new BigDecimal("65000.75"), 2, 15));
    }

    @Test
    void normalizesOutOfRangePaginationAsBefore() throws Exception {
        for (int size : new int[]{-1, 0, 101}) {
            mvc.perform(get("/api/v1/search/job-offers").param("page", "-1")
                            .param("size", Integer.toString(size)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20));
        }
        mvc.perform(get("/api/v1/search/job-offers").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void rejectsMalformedTypedParameters() throws Exception {
        for (String parameter : List.of("remote", "salaryMin", "salaryMax", "page", "size")) {
            mvc.perform(get("/api/v1/search/job-offers").param(parameter, "invalid"))
                    .andExpect(status().isBadRequest());
        }
    }
}
