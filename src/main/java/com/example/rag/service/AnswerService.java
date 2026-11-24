package com.example.rag.service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.rag.dto.AskResponse;
import com.example.rag.dto.LlmAnswer;
import com.example.rag.dto.SearchResult;
import com.example.rag.dto.SourceRef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * The whole RAG pipeline for one question:
 * retrieve chunks -> build prompt -> ask the LLM -> validate the JSON -> build the response.
 */
@Service
public class AnswerService {

    static final String NO_ANSWER = "I don't know based on the provided documents.";
    private static final Logger log = LoggerFactory.getLogger(AnswerService.class);
    private static final int MAX_ATTEMPTS = 2; // first try + one retry

    private final RetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public AnswerService(RetrievalService retrievalService, PromptBuilder promptBuilder,
                         LlmClient llmClient, ObjectMapper objectMapper, Validator validator) {
        this.retrievalService = retrievalService;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public AskResponse ask(String question) {
        List<SearchResult> chunks = retrievalService.search(question, null, null);

        // Nothing relevant found: do not even call the LLM. This is the strongest protection against hallucination.
        if (chunks.isEmpty()) {
            return new AskResponse(question, NO_ANSWER, List.of(), 0.0, Instant.now());
        }

        PromptBuilder.Prompt prompt = promptBuilder.build(question, chunks);
        LlmAnswer answer = generateValidAnswer(prompt);

        return new AskResponse(question, answer.answer(), toSources(answer, chunks),
                answer.confidence(), Instant.now());
    }

    private LlmAnswer generateValidAnswer(PromptBuilder.Prompt prompt) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String raw = llmClient.generateJson(prompt.system(), prompt.user());
            try {
                LlmAnswer parsed = objectMapper.readValue(raw, LlmAnswer.class);
                Set<ConstraintViolation<LlmAnswer>> violations = validator.validate(parsed);
                if (violations.isEmpty()) {
                    return parsed;
                }
                ConstraintViolation<LlmAnswer> first = violations.iterator().next();
                log.warn("LLM answer failed validation (attempt {}/{}): {} {}",
                        attempt, MAX_ATTEMPTS, first.getPropertyPath(), first.getMessage());
            } catch (JsonProcessingException e) {
                log.warn("LLM returned invalid JSON (attempt {}/{}): {}", attempt, MAX_ATTEMPTS, e.getOriginalMessage());
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "The language model did not return a valid answer. Please try again.");
    }

    /** Sources come from OUR retrieved chunks, so the model can never invent a source. */
    private List<SourceRef> toSources(LlmAnswer answer, List<SearchResult> chunks) {
        Set<Integer> valid = new LinkedHashSet<>();
        for (Integer n : answer.usedChunks()) {
            if (n != null && n >= 1 && n <= chunks.size()) {
                valid.add(n);
            }
        }
        return valid.stream().map(n -> {
            SearchResult c = chunks.get(n - 1);
            return new SourceRef(c.source(), c.chunkIndex(), Math.round(c.score() * 10000) / 10000.0);
        }).toList();
    }
}
