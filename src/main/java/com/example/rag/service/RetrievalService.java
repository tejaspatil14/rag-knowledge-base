package com.example.rag.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.rag.dto.SearchResult;
import com.example.rag.repository.DocumentChunkRepository;
import com.example.rag.util.VectorUtils;

/**
 * The "R" in RAG: question -> vector -> most similar chunks.
 * In Week 5 the LLM service will call this and use the chunks as context.
 */
@Service
public class RetrievalService {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository repository;
    private final int defaultTopK;
    private final double defaultMinScore;

    public RetrievalService(EmbeddingService embeddingService,
                            DocumentChunkRepository repository,
                            @Value("${rag.retrieval.top-k:5}") int defaultTopK,
                            @Value("${rag.retrieval.min-score:0.4}") double defaultMinScore) {
        this.embeddingService = embeddingService;
        this.repository = repository;
        this.defaultTopK = defaultTopK;
        this.defaultMinScore = defaultMinScore;
    }

    /** topK and minScore may be null, in which case the defaults from application.yml are used. */
    public List<SearchResult> search(String query, Integer topK, Double minScore) {
        int k = topK != null ? topK : defaultTopK;
        double threshold = minScore != null ? minScore : defaultMinScore;

        float[] queryVector = embeddingService.embedQuery(query);

        return repository.searchSimilar(VectorUtils.toLiteral(queryVector), k).stream()
                .filter(r -> r.score() >= threshold)
                .toList();
    }
}
