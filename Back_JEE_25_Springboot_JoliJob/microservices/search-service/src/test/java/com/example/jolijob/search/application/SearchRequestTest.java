package com.example.jolijob.search.application;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SearchRequestTest {
    @ParameterizedTest
    @CsvSource({"-1, 0, 0, 20", "0, -1, 0, 20", "2, 101, 2, 20",
            "0, 1, 0, 1", "3, 100, 3, 100", "1, 20, 1, 20"})
    void normalizesPaginationWithoutChangingTheSearchFilters(
            int page, int size, int expectedPage, int expectedSize) {
        var request = new SearchRequest("Java", "IT", "Backend", "CDI", false,
                "France", "IDF", "75", "Paris", new BigDecimal("45000.50"),
                new BigDecimal("65000.75"), page, size);

        assertThat(request.page()).isEqualTo(expectedPage);
        assertThat(request.size()).isEqualTo(expectedSize);
        assertThat(request).isEqualTo(new SearchRequest("Java", "IT", "Backend", "CDI", false,
                "France", "IDF", "75", "Paris", new BigDecimal("45000.50"),
                new BigDecimal("65000.75"), expectedPage, expectedSize));
    }
}
