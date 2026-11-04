package com.example.jolijob.search.adapter.out.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.example.jolijob.search.application.SearchRequest;
import com.example.jolijob.search.application.SearchResult;
import com.example.jolijob.search.application.port.JobOfferSearchPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ElasticsearchSearchAdapter implements JobOfferSearchPort {

    private final ElasticsearchOperations operations;

    public ElasticsearchSearchAdapter(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Override
    public SearchResult search(SearchRequest request) {
        var hits = operations.search(buildQuery(request), JobOfferDocument.class);
        var results = hits.getSearchHits().stream()
                .map(hit -> hit.getContent().toResult())
                .toList();
        return new SearchResult(results, hits.getTotalHits(), request.page(), request.size());
    }

    static NativeQuery buildQuery(SearchRequest request) {
        List<Query> filters = new ArrayList<>();
        filters.add(TermQuery.of(t -> t.field("active").value(true))._toQuery());
        if (request.keyword() != null && !request.keyword().isBlank()) {
            filters.add(MultiMatchQuery.of(m -> m
                    .fields("title^3", "description", "jobCategory", "specialty", "companyName")
                    .query(request.keyword())
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO"))._toQuery());
        }
        addTerm(filters, "jobCategory", request.jobCategory());
        addTerm(filters, "specialty", request.specialty());
        addTerm(filters, "contractType", request.contractType());
        if (request.remote() != null) {
            filters.add(TermQuery.of(t -> t.field("remote").value(request.remote()))._toQuery());
        }
        addTerm(filters, "country", request.country());
        addTerm(filters, "region", request.region());
        addTerm(filters, "department", request.department());
        addTerm(filters, "city", request.city());
        if (request.salaryMin() != null) {
            filters.add(RangeQuery.of(r -> r.number(n -> n.field("salaryMax")
                    .gte(request.salaryMin().doubleValue())))._toQuery());
        }
        if (request.salaryMax() != null) {
            filters.add(RangeQuery.of(r -> r.number(n -> n.field("salaryMin")
                    .lte(request.salaryMax().doubleValue())))._toQuery());
        }
        return NativeQuery.builder()
                .withQuery(BoolQuery.of(b -> b.must(filters))._toQuery())
                .withPageable(PageRequest.of(request.page(), request.size()))
                .build();
    }

    private static void addTerm(List<Query> filters, String field, String value) {
        if (value != null) {
            filters.add(TermQuery.of(t -> t.field(field).value(value))._toQuery());
        }
    }
}
