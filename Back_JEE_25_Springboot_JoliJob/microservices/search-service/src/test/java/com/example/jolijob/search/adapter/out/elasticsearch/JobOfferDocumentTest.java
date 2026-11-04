package com.example.jolijob.search.adapter.out.elasticsearch;

import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobOfferDocumentTest {

    @Test
    void readsIndexerSourceIncludingForeignTypeHintAndIsoTimestamp() {
        var context = new SimpleElasticsearchMappingContext();
        var conversions = new ElasticsearchCustomConversions(List.of());
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        var converter = new MappingElasticsearchConverter(context);
        converter.setConversions(conversions);
        converter.afterPropertiesSet();
        var source = Document.parse("""
                {
                  "_class": "com.example.jolijob.indexer.adapter.out.elasticsearch.JobOfferDocument",
                  "id": "offer-1", "companyId": "company-1", "companyName": "Acme",
                  "title": "Developer", "description": "Build APIs",
                  "jobCategory": "IT", "specialty": "Java", "contractType": "CDI",
                  "remote": true, "country": "France", "region": "IDF",
                  "department": "75", "city": "Paris",
                  "salaryMin": 45000.50, "salaryMax": 65000.75,
                  "active": true, "createdAt": "2026-10-20T12:00:00Z"
                }
                """);

        var document = converter.read(JobOfferDocument.class, source);

        assertThat(document.id()).isEqualTo("offer-1");
        assertThat(document.salaryMin()).isEqualByComparingTo(new BigDecimal("45000.50"));
        assertThat(document.salaryMax()).isEqualByComparingTo(new BigDecimal("65000.75"));
        assertThat(document.createdAt()).isEqualTo("2026-10-20T12:00:00Z");
        assertThat(document.toResult()).usingRecursiveComparison().isEqualTo(document);
        var entity = context.getRequiredPersistentEntity(JobOfferDocument.class);
        assertThat(entity.getIndexCoordinates().getIndexName()).isEqualTo("job_offers");
        assertThat(entity.isCreateIndexAndMapping()).isFalse();
    }
}
