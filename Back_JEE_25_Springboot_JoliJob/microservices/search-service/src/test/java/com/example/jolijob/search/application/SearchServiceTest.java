package com.example.jolijob.search.application;

import com.example.jolijob.search.application.port.JobOfferSearchPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceTest {
    private final JobOfferSearchPort port = mock(JobOfferSearchPort.class);
    private final SearchService service = new SearchService(port);

    @Test
    void delegatesTheRequestAndPreservesTheResult() {
        var request = new SearchRequest("Java", null, null, null, true,
                null, null, null, null, null, null, 2, 10);
        var result = new SearchResult(List.of(), 0, 2, 10);
        when(port.search(request)).thenReturn(result);

        assertThat(service.search(request)).isSameAs(result);
        verify(port).search(request);
    }

    @Test
    void propagatesSearchFailureInsteadOfReturningAnEmptyResult() {
        var request = new SearchRequest(null, null, null, null, null,
                null, null, null, null, null, null, 0, 20);
        var failure = new IllegalStateException("Search backend unavailable");
        when(port.search(request)).thenThrow(failure);

        assertThatThrownBy(() -> service.search(request)).isSameAs(failure);
    }
}
