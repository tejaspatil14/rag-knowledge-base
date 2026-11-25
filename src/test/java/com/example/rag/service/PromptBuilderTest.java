package com.example.rag.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.rag.dto.SearchResult;

class PromptBuilderTest {

    @Test
    void numbersTheChunksAndEndsWithTheQuestion() {
        List<SearchResult> chunks = List.of(
                new SearchResult(10, "a.pdf", 3, "Ganga flows here", 0.9),
                new SearchResult(11, "b.pdf", 7, "Another chunk", 0.8));

        PromptBuilder.Prompt prompt = new PromptBuilder().build("Which river?", chunks);

        assertTrue(prompt.user().contains("[1] (source: a.pdf, similarity: 0.9000)"));
        assertTrue(prompt.user().contains("[2] (source: b.pdf, similarity: 0.8000)"));
        assertTrue(prompt.user().contains("Ganga flows here"));
        assertTrue(prompt.user().endsWith("Question: Which river?"));
        assertTrue(prompt.system().contains("ONLY"));
    }
}
