package com.example.rag.service;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Service
public class OllamaEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingService.class);

    // nomic-embed-text works best when it is told what the text is for.
    private static final String DOC_PREFIX = "search_document: ";
    private static final String QUERY_PREFIX = "search_query: ";
    private static final int MAX_ATTEMPTS = 3;

    private final RestClient client;
    private final String baseUrl;
    private final String model;
    private final int expectedDimensions;

    public OllamaEmbeddingService(
            @Value("${rag.ollama.base-url}") String baseUrl,
            @Value("${rag.embedding.model}") String model,
            @Value("${rag.embedding.dimensions}") int expectedDimensions) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.baseUrl = baseUrl;
        this.model = model;
        this.expectedDimensions = expectedDimensions;
    }

    @Override
    public List<float[]> embedDocuments(List<String> texts) {
        return embed(texts.stream().map(t -> DOC_PREFIX + t).toList());
    }

    @Override
    public float[] embedQuery(String text) {
        return embed(List.of(QUERY_PREFIX + text)).get(0);
    }

    private List<float[]> embed(List<String> inputs) {
        RestClientException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                EmbedResponse response = client.post()
                        .uri("/api/embed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new EmbedRequest(model, inputs))
                        .retrieve()
                        .body(EmbedResponse.class);
                return toVectors(response, inputs.size());
            } catch (RestClientException e) {
                last = e;
                log.warn("Ollama call failed (attempt {}/{}): {}", attempt, MAX_ATTEMPTS, e.getMessage());
                sleep(attempt * 1000L); // simple backoff: 1s, 2s, 3s
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Could not get embeddings from Ollama at " + baseUrl
                        + ". Is Ollama running, and did you run: ollama pull " + model,
                last);
    }

    private List<float[]> toVectors(EmbedResponse response, int expectedCount) {
        if (response == null || response.embeddings() == null || response.embeddings().size() != expectedCount) {
            throw new IllegalStateException("Ollama returned an unexpected number of embeddings");
        }
        return response.embeddings().stream().map(list -> {
            if (list.size() != expectedDimensions) {
                throw new IllegalStateException("Model " + model + " returned " + list.size()
                        + " dimensions but the table expects " + expectedDimensions
                        + ". Fix rag.embedding.dimensions and the VECTOR(n) column together.");
            }
            float[] v = new float[list.size()];
            for (int i = 0; i < v.length; i++) {
                v[i] = list.get(i).floatValue();
            }
            return v;
        }).toList();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public record EmbedRequest(String model, List<String> input) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmbedResponse(List<List<Double>> embeddings) {
    }
}
