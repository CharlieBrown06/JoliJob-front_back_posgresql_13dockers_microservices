package com.example.jolijob.search.repository;

import com.example.jolijob.search.document.JobOfferDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface JobOfferSearchRepository extends ElasticsearchRepository<JobOfferDocument, String> {}
