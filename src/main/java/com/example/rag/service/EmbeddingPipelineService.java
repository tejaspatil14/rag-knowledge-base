package com.example.rag.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.rag.dto.EmbeddingRunResponse;
import com.example.rag.dto.PendingChunk;
import com.example.rag.repository.DocumentChunkRepository;

/**
 * Finds every chunk that has no vector yet, embeds it in batches and saves the vector.
 * It is safe to run again: it only touches chunks where embedding IS NULL.
 */
@Service
public class EmbeddingPipelineService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingPipelineService.class);

    private final DocumentChunkRepository repository;
    private final EmbeddingService embeddingService;
    private final int batchSize;

    public EmbeddingPipelineService(DocumentChunkRepository repository,
                                    EmbeddingService embeddingService,
                                    @Value("${rag.embedding.batch-size:16}") int batchSize) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.batchSize = batchSize;
    }

    public EmbeddingRunResponse embedPendingChunks() {
        long start = System.currentTimeMillis();
        int embedded = 0;

        while (true) {
            List<PendingChunk> batch = repository.findChunksWithoutEmbedding(batchSize);
            if (batch.isEmpty()) {
                break;
            }
            List<float[]> vectors = embeddingService.embedDocuments(
                    batch.stream().map(PendingChunk::content).toList());
            repository.updateEmbeddings(batch.stream().map(PendingChunk::id).toList(), vectors);
            embedded += batch.size();
            log.info("Embedded {} chunks so far", embedded);
        }

        double seconds = (System.currentTimeMillis() - start) / 1000.0;
        return new EmbeddingRunResponse(embedded, repository.countWithoutEmbedding(), seconds);
    }
}
