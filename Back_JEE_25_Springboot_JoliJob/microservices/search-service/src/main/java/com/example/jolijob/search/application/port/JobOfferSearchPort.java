package com.example.jolijob.search.application.port;

import com.example.jolijob.search.application.SearchRequest;
import com.example.jolijob.search.application.SearchResult;

public interface JobOfferSearchPort {
    SearchResult search(SearchRequest request);
}
