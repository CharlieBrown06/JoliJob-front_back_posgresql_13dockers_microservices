package com.example.jolijob.search.web;

import com.example.jolijob.search.service.SearchService;
import com.example.jolijob.search.web.dto.SearchRequest;
import com.example.jolijob.search.web.dto.SearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Recherche", description = "Recherche multicritère d'offres d'emploi (Elasticsearch)")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/job-offers")
    @Operation(summary = "Recherche d'offres d'emploi multicritère")
    public SearchResponse search(
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
        SearchRequest request = new SearchRequest(
                keyword, jobCategory, specialty, contractType, remote,
                country, region, department, city, salaryMin, salaryMax,
                page, size
        );
        return searchService.search(request);
    }
}
