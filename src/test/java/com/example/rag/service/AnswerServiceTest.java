package com.example.rag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.example.rag.dto.AskResponse;
import com.example.rag.dto.SearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Validation;

class AnswerServiceTest {

    private static final String VALID = "{\"answer\":\"The Ganga\",\"usedChunks\":[1],\"confidence\":0.9}";

    private final RetrievalService retrieval = mock(RetrievalService.class);
    private final LlmClient llm = mock(LlmClient.class);
    private final AnswerService service = new AnswerService(
            retrieval, new PromptBuilder(), llm, new ObjectMapper(),
            Validation.buildDefaultValidatorFactory().getValidator());

    private final List<SearchResult> chunks = List.of(
            new SearchResult(10, "a.pdf", 3, "Ganga flows here", 0.91234567),
            new SearchResult(11, "b.pdf", 7, "Other text", 0.5));

    @Test
    void noRelevantChunksMeansIDontKnowAndNoLlmCall() {
        when(retrieval.search(anyString(), any(), any())).thenReturn(List.of());

        AskResponse response = service.ask("How to bake a cake?");

        assertEquals(AnswerService.NO_ANSWER, response.answer());
        assertEquals(0.0, response.confidence());
        assertTrue(response.sources().isEmpty());
        verify(llm, never()).generateJson(anyString(), anyString());
    }

    @Test
    void validAnswerIsMappedToSources() {
        when(retrieval.search(anyString(), any(), any())).thenReturn(chunks);
        when(llm.generateJson(anyString(), anyString())).thenReturn(VALID);

        AskResponse response = service.ask("Which river?");

        assertEquals("The Ganga", response.answer());
        assertEquals(1, response.sources().size());
        assertEquals("a.pdf", response.sources().get(0).source());
        assertEquals(3, response.sources().get(0).chunkIndex());
        assertEquals(0.9123, response.sources().get(0).score(), 0.00001);
    }

    @Test
    void invalidChunkNumbersAreIgnored() {
        when(retrieval.search(anyString(), any(), any())).thenReturn(chunks);
        when(llm.generateJson(anyString(), anyString()))
                .thenReturn("{\"answer\":\"x\",\"usedChunks\":[1, 9],\"confidence\":0.5}");

        assertEquals(1, service.ask("q").sources().size());
    }

    @Test
    void retriesOnceWhenJsonIsInvalid() {
        when(retrieval.search(anyString(), any(), any())).thenReturn(chunks);
        when(llm.generateJson(anyString(), anyString())).thenReturn("not json at all", VALID);

        assertEquals("The Ganga", service.ask("q").answer());
        verify(llm, times(2)).generateJson(anyString(), anyString());
    }

    @Test
    void failsWhenModelNeverReturnsValidJson() {
        when(retrieval.search(anyString(), any(), any())).thenReturn(chunks);
        when(llm.generateJson(anyString(), anyString())).thenReturn("garbage");

        assertThrows(ResponseStatusException.class, () -> service.ask("q"));
    }
}
