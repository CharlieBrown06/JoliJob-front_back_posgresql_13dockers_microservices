package com.example.jolijob.search.web.dto;

import com.example.jolijob.search.document.JobOfferDocument;

import java.util.List;

public record SearchResponse(
        List<JobOfferDocument> results,
        long totalHits,
        int page,
        int size
) {}
