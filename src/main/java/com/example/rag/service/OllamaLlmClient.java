
package com.example.rag.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

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
import com.fasterxml.jackson.annotation.JsonProperty;

@Service
public class OllamaLlmClient implements LlmClient {

    private static final Logger log =
            LoggerFactory.getLogger(OllamaLlmClient.class);

    /*
     * Ollama structured output schema.
     * The model must return JSON matching this structure.
     */

    private static final Map<String, Object> ANSWER_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "answer", Map.of(
                            "type", "string"
                    ),
                    "usedChunks", Map.of(
                            "type", "array",
                            "items", Map.of(
                                    "type", "integer",
                                    "minimum", 1,
                                    "maximum", 5
                            ),
                            "minItems", 0,
                            "maxItems", 5,
                            "uniqueItems", true
                    ),
                    "confidence", Map.of(
                            "type", "number",
                            "minimum", 0,
                            "maximum", 1
                    )
            ),
            "required", List.of(
                    "answer",
                    "usedChunks",
                    "confidence"
            ),
            "additionalProperties", false
    );


    private final RestClient client;
    private final String baseUrl;
    private final String model;
    private final double temperature;

    public OllamaLlmClient(
            @Value("${rag.ollama.base-url}") String baseUrl,
            @Value("${rag.llm.model}") String model,
            @Value("${rag.llm.num-ctx:8192}") int numCtx,
            @Value("${rag.llm.temperature:0.0}") double temperature) {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofMinutes(5));

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();

        this.baseUrl = baseUrl;
        this.model = model;
        this.temperature = temperature;

        log.info("Ollama client configured: model={}, context={}, GPU=CPU-only",
                model, 2048);
    }

    @Override
    public String generateJson(String systemPrompt, String userPrompt) {

        ChatRequest request = new ChatRequest(
                model,
                List.of(
                        new Message("system", systemPrompt),
                        new Message("user", userPrompt)
                ),
                false,
                ANSWER_SCHEMA,
                Map.of(
                        "temperature", temperature,
                        "num_ctx", 4096,
                        "num_gpu", 0,
                        "num_predict", 1024
                )
        );

        try {
            ChatResponse response = client.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);

            if (response == null
                    || response.message() == null
                    || response.message().content() == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Ollama returned an empty response"
                );
            }

            // Log generation details for debugging.
            log.warn(
                    "Ollama generation finished: done={}, reason={}, tokens={}",
                    response.done(),
                    response.doneReason(),
                    response.evalCount()
            );

            String content = response.message().content();

            log.warn("Ollama response content: {}", content);

            return content;

        } catch (RestClientException e) {

            log.error("Ollama request failed: {}", e.getMessage());

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not get an answer from Ollama at " + baseUrl
                            + ". Is it running, and did you run: ollama pull " + model,
                    e
            );
        }
    }

    public record Message(String role, String content) {
    }

    public record ChatRequest(
            String model,
            List<Message> messages,
            boolean stream,
            Object format,
            Map<String, Object> options) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatResponse(
            Message message,
            Boolean done,

            @JsonProperty("done_reason")
            String doneReason,

            @JsonProperty("eval_count")
            Integer evalCount) {
    }
}
