package com.example.rag.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.rag.dto.EmbeddingRunResponse;
import com.example.rag.repository.DocumentChunkRepository;
import com.example.rag.service.EmbeddingPipelineService;
import com.example.rag.service.EmbeddingService;

@RestController
@RequestMapping("/api/embeddings")
public class EmbeddingController {

    private final EmbeddingPipelineService pipeline;
    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository repository;

    public EmbeddingController(EmbeddingPipelineService pipeline, EmbeddingService embeddingService,
                               DocumentChunkRepository repository) {
        this.pipeline = pipeline;
        this.embeddingService = embeddingService;
        this.repository = repository;
    }

    /** Embeds every chunk that does not have a vector yet. */
    @PostMapping("/run")
    public EmbeddingRunResponse run() {
        return pipeline.embedPendingChunks();
    }

    /** How many chunks are done and how many are waiting. */
    @GetMapping("/status")
    public Map<String, Integer> status() {
        int total = repository.countAll();
        int pending = repository.countWithoutEmbedding();
        return Map.of("totalChunks", total, "withEmbedding", total - pending, "pending", pending);
    }

    /** Quick check that Ollama works: GET /api/embeddings/test?text=hello */
    @GetMapping("/test")
    public Map<String, Object> test(@RequestParam(defaultValue = "hello world") String text) {
        float[] v = embeddingService.embedQuery(text);
        return Map.of("text", text, "dimensions", v.length,
                "firstFiveValues", Arrays.toString(Arrays.copyOf(v, 5)));
    }
}
