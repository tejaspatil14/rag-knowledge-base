package com.example.rag.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Splits text into overlapping chunks.
 *
 * We count WORDS as an approximation of tokens (1 token is about 0.75 words).
 * Later you can replace this with a real tokenizer such as jtokkit.
 *
 * Example with size=10, overlap=2: words 0-9, then 8-17, then 16-25 ...
 * The overlap keeps a sentence that is cut at a boundary readable in both chunks.
 */
@Service
public class ChunkingService {

    private final int sizeWords;
    private final int overlapWords;

    public ChunkingService(
            @Value("${rag.chunking.size-words:380}") int sizeWords,
            @Value("${rag.chunking.overlap-words:40}") int overlapWords) {
        if (sizeWords <= 0 || overlapWords < 0 || overlapWords >= sizeWords) {
            throw new IllegalArgumentException(
                    "Need size > 0 and 0 <= overlap < size, got size=" + sizeWords + ", overlap=" + overlapWords);
        }
        this.sizeWords = sizeWords;
        this.overlapWords = overlapWords;
    }

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] words = text.trim().split("\\s+");
        List<String> chunks = new ArrayList<>();
        int step = sizeWords - overlapWords;

        for (int start = 0; start < words.length; start += step) {
            int end = Math.min(start + sizeWords, words.length);
            chunks.add(String.join(" ", Arrays.copyOfRange(words, start, end)));
            if (end == words.length) {
                break;
            }
        }
        return chunks;
    }
}
