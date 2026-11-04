package com.example.jolijob.indexer.adapter.out.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface JobOfferDocumentRepository extends ElasticsearchRepository<JobOfferDocument, String> {}
