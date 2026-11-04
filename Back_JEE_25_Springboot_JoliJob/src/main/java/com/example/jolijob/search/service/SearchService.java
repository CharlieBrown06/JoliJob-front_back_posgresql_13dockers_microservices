package com.example.jolijob.search.service;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.example.jolijob.search.document.JobOfferDocument;
import com.example.jolijob.search.web.dto.SearchRequest;
import com.example.jolijob.search.web.dto.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public SearchResponse search(SearchRequest req) {
        List<Query> filters = new ArrayList<>();

        // Seules les offres actives
        filters.add(TermQuery.of(t -> t.field("active").value(true))._toQuery());

        if (req.keyword() != null && !req.keyword().isBlank()) {
            filters.add(MultiMatchQuery.of(m -> m
                    .fields("title^3", "description", "jobCategory", "specialty", "companyName")
                    .query(req.keyword())
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO"))._toQuery());
        }
        if (req.jobCategory() != null) {
            filters.add(TermQuery.of(t -> t.field("jobCategory").value(req.jobCategory()))._toQuery());
        }
        if (req.specialty() != null) {
            filters.add(TermQuery.of(t -> t.field("specialty").value(req.specialty()))._toQuery());
        }
        if (req.contractType() != null) {
            filters.add(TermQuery.of(t -> t.field("contractType").value(req.contractType()))._toQuery());
        }
        if (req.remote() != null) {
            filters.add(TermQuery.of(t -> t.field("remote").value(req.remote()))._toQuery());
        }
        if (req.country() != null) {
            filters.add(TermQuery.of(t -> t.field("country").value(req.country()))._toQuery());
        }
        if (req.region() != null) {
            filters.add(TermQuery.of(t -> t.field("region").value(req.region()))._toQuery());
        }
        if (req.department() != null) {
            filters.add(TermQuery.of(t -> t.field("department").value(req.department()))._toQuery());
        }
        if (req.city() != null) {
            filters.add(TermQuery.of(t -> t.field("city").value(req.city()))._toQuery());
        }
        if (req.salaryMin() != null) {
            filters.add(RangeQuery.of(r -> r
                    .number(n -> n.field("salaryMax").gte(req.salaryMin().doubleValue())))._toQuery());
        }
        if (req.salaryMax() != null) {
            filters.add(RangeQuery.of(r -> r
                    .number(n -> n.field("salaryMin").lte(req.salaryMax().doubleValue())))._toQuery());
        }

        Query boolQuery = BoolQuery.of(b -> b.must(filters))._toQuery();

        NativeQuery query = NativeQuery.builder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of(req.page(), req.size()))
                .build();

        SearchHits<JobOfferDocument> hits = elasticsearchOperations.search(query, JobOfferDocument.class);

        List<JobOfferDocument> results = hits.getSearchHits().stream()
                .map(h -> h.getContent())
                .toList();

        return new SearchResponse(results, hits.getTotalHits(), req.page(), req.size());
    }
}
