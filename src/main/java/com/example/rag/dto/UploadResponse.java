package com.example.rag.dto;

public record UploadResponse(
        String source,
        int totalChunks,
        int totalCharacters,
        String message) {
}
