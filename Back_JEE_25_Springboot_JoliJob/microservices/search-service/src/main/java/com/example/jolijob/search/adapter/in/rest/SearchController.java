package com.example.jolijob.search.adapter.in.rest;

import com.example.jolijob.search.application.SearchRequest;
import com.example.jolijob.search.application.SearchResult;
import com.example.jolijob.search.application.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/job-offers")
    public SearchResult search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String jobCategory,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String contractType,
            @RequestParam(required = false) Boolean remote,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return searchService.search(new SearchRequest(
                keyword, jobCategory, specialty, contractType, remote,
                country, region, department, city, salaryMin, salaryMax, page, size));
    }
}
