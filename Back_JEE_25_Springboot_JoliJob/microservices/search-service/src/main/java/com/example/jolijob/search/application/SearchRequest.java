package com.example.jolijob.search.application;

import java.math.BigDecimal;

public record SearchRequest(
        String keyword,
        String jobCategory,
        String specialty,
        String contractType,
        Boolean remote,
        String country,
        String region,
        String department,
        String city,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        int page,
        int size
) {
    public SearchRequest {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
    }
}
