package com.example.rag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.rag.dto.SearchResult;
import com.example.rag.repository.DocumentChunkRepository;

class RetrievalServiceTest {

    private final EmbeddingService embeddingService = mock(EmbeddingService.class);
    private final DocumentChunkRepository repository = mock(DocumentChunkRepository.class);

    @Test
    void dropsResultsBelowTheDefaultThreshold() {
        when(embeddingService.embedQuery("q")).thenReturn(new float[] { 1f, 0f });
        when(repository.searchSimilar(eq("[1.0,0.0]"), eq(5))).thenReturn(List.of(
                new SearchResult(1, "a.pdf", 0, "relevant", 0.9),
                new SearchResult(2, "a.pdf", 1, "irrelevant", 0.2)));

        RetrievalService service = new RetrievalService(embeddingService, repository, 5, 0.4);
        List<SearchResult> results = service.search("q", null, null);

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).id());
    }

    @Test
    void explicitTopKAndMinScoreOverrideDefaults() {
        when(embeddingService.embedQuery(anyString())).thenReturn(new float[] { 1f, 0f });
        when(repository.searchSimilar(anyString(), eq(3))).thenReturn(List.of(
                new SearchResult(1, "a.pdf", 0, "x", 0.9),
                new SearchResult(2, "a.pdf", 1, "y", 0.2)));

        RetrievalService service = new RetrievalService(embeddingService, repository, 5, 0.4);
        List<SearchResult> results = service.search("q", 3, 0.0);

        assertEquals(2, results.size());
        verify(repository).searchSimilar("[1.0,0.0]", 3);
    }
}
