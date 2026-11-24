package com.example.rag.dto;

public record SearchResult(
        long id,
        String source,
        int chunkIndex,
        String content,
        double score) {
}
