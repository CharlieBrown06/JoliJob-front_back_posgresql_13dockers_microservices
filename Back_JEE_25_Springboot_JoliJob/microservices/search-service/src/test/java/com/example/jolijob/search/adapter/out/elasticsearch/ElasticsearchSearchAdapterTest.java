package com.example.jolijob.search.adapter.out.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.example.jolijob.search.application.SearchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElasticsearchSearchAdapterTest {

    @Test
    void includesAllFiltersAndUsesSalaryOverlapRatherThanContainedRange() {
        var request = new SearchRequest("developer", "IT", "Java", "CDI", false,
                "France", "IDF", "75", "Paris", new BigDecimal("45000"),
                new BigDecimal("65000"), 2, 15);

        var query = ElasticsearchSearchAdapter.buildQuery(request);
        var clauses = query.getQuery().bool().must();

        assertThat(clauses).hasSize(12);
        var terms = clauses.stream().filter(Query::isTerm).map(Query::term).toList();
        assertThat(terms).hasSize(9);
        assertThat(terms.stream().filter(t -> t.field().equals("active")).findFirst().orElseThrow()
                .value().booleanValue()).isTrue();
        assertThat(terms.stream().filter(t -> t.field().equals("remote")).findFirst().orElseThrow()
                .value().booleanValue()).isFalse();
        var expectedTerms = Map.of("jobCategory", "IT", "specialty", "Java",
                "contractType", "CDI", "country", "France", "region", "IDF",
                "department", "75", "city", "Paris");
        expectedTerms.forEach((field, value) ->
                assertThat(terms.stream().filter(t -> t.field().equals(field)).findFirst()
                        .orElseThrow().value().stringValue()).isEqualTo(value));

        var keyword = clauses.stream().filter(Query::isMultiMatch).findFirst().orElseThrow().multiMatch();
        assertThat(keyword.fields()).containsExactly(
                "title^3", "description", "jobCategory", "specialty", "companyName");
        assertThat(keyword.query()).isEqualTo("developer");
        assertThat(keyword.type()).isEqualTo(TextQueryType.BestFields);
        assertThat(keyword.fuzziness()).isEqualTo("AUTO");

        var ranges = clauses.stream().filter(Query::isRange).map(q -> q.range().number()).toList();
        assertThat(ranges).hasSize(2);
        assertThat(ranges.get(0).field()).isEqualTo("salaryMax");
        assertThat(ranges.get(0).gte()).isEqualTo(45000d);
        assertThat(ranges.get(1).field()).isEqualTo("salaryMin");
        assertThat(ranges.get(1).lte()).isEqualTo(65000d);
        assertThat(query.getPageable().getOffset()).isEqualTo(30);
        assertThat(query.getPageable().getPageSize()).isEqualTo(15);
    }

    @Test
    void absentOptionalFiltersAndBlankKeywordOnlyRequireActiveOffers() {
        for (String keyword : new String[]{null, "", "  "}) {
            var request = new SearchRequest(keyword, null, null, null, null,
                    null, null, null, null, null, null, 0, 20);
            var clauses = ElasticsearchSearchAdapter.buildQuery(request).getQuery().bool().must();
            assertThat(clauses).hasSize(1);
            assertThat(clauses.getFirst().term().field()).isEqualTo("active");
            assertThat(clauses.getFirst().term().value().booleanValue()).isTrue();
        }
    }

    @Test
    void retainsEmptyExactFilterAndIndependentSalaryBounds() {
        var lower = new SearchRequest(null, "", null, null, null,
                null, null, null, null, BigDecimal.TEN, null, 0, 20);
        var lowerClauses = ElasticsearchSearchAdapter.buildQuery(lower).getQuery().bool().must();
        assertThat(lowerClauses).hasSize(3);
        assertThat(lowerClauses.get(1).term().value().stringValue()).isEmpty();
        assertThat(lowerClauses.get(2).range().number().field()).isEqualTo("salaryMax");
        assertThat(lowerClauses.get(2).range().number().gte()).isEqualTo(10d);

        var upper = new SearchRequest(null, null, null, null, null,
                null, null, null, null, null, BigDecimal.TEN, 0, 20);
        var upperClauses = ElasticsearchSearchAdapter.buildQuery(upper).getQuery().bool().must();
        assertThat(upperClauses).hasSize(2);
        assertThat(upperClauses.get(1).range().number().field()).isEqualTo("salaryMin");
        assertThat(upperClauses.get(1).range().number().lte()).isEqualTo(10d);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapsSearchHitsWithoutLeakingElasticsearchDocuments() {
        var operations = mock(ElasticsearchOperations.class);
        SearchHits<JobOfferDocument> hits = mock(SearchHits.class);
        SearchHit<JobOfferDocument> hit = mock(SearchHit.class);
        var document = new JobOfferDocument("offer-1", "company-1", "Acme", "Developer",
                "Build APIs", "IT", "Java", "CDI", true, "France", "IDF", "75", "Paris",
                new BigDecimal("45000.50"), new BigDecimal("65000.75"), true,
                "2026-10-20T12:00:00Z");
        when(operations.search(any(org.springframework.data.elasticsearch.core.query.Query.class),
                eq(JobOfferDocument.class))).thenReturn(hits);
        when(hits.getSearchHits()).thenReturn(List.of(hit));
        when(hit.getContent()).thenReturn(document);
        when(hits.getTotalHits()).thenReturn(42L);
        var request = new SearchRequest(null, null, null, null, null,
                null, null, null, null, null, null, 1, 10);

        var result = new ElasticsearchSearchAdapter(operations).search(request);

        assertThat(result.totalHits()).isEqualTo(42);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.results()).hasSize(1);
        assertThat(result.results().getFirst()).usingRecursiveComparison().isEqualTo(document);
        verify(operations).search(any(org.springframework.data.elasticsearch.core.query.Query.class),
                eq(JobOfferDocument.class));
    }
}
