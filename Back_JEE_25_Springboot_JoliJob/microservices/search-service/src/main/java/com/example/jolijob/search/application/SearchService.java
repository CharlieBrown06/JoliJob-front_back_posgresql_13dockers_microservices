package com.example.jolijob.search.application;

import com.example.jolijob.search.application.port.JobOfferSearchPort;

public class SearchService {

    private final JobOfferSearchPort searchPort;

    public SearchService(JobOfferSearchPort searchPort) {
        this.searchPort = searchPort;
    }

    public SearchResult search(SearchRequest request) {
        return searchPort.search(request);
    }
}
