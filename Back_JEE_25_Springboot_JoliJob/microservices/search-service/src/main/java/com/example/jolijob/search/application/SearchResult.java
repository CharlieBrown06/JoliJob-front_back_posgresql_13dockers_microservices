package com.example.jolijob.search.application;

import java.util.List;

public record SearchResult(List<JobOffer> results, long totalHits, int page, int size) {}
