package com.example.rag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class ChunkingServiceTest {

    private String words(int n) {
        return IntStream.range(0, n).mapToObj(i -> "w" + i).collect(Collectors.joining(" "));
    }

    @Test
    void emptyTextGivesNoChunks() {
        assertTrue(new ChunkingService(10, 2).chunk("   ").isEmpty());
    }

    @Test
    void shortTextIsOneChunk() {
        List<String> chunks = new ChunkingService(10, 2).chunk(words(5));
        assertEquals(1, chunks.size());
    }

    @Test
    void chunksOverlapByTheConfiguredAmount() {
        // size 10, overlap 2: chunk 0 = w0..w9, chunk 1 = w8..w17
        List<String> chunks = new ChunkingService(10, 2).chunk(words(18));
        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).endsWith("w8 w9"));
        assertTrue(chunks.get(1).startsWith("w8 w9"));
    }

    @Test
    void lastWordIsAlwaysIncluded() {
        List<String> chunks = new ChunkingService(10, 2).chunk(words(25));
        assertTrue(chunks.get(chunks.size() - 1).endsWith("w24"));
    }

    @Test
    void invalidSettingsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkingService(10, 10));
    }
}
