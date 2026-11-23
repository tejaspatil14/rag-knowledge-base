package com.example.rag.dto;

public record EmbeddingRunResponse(int embeddedNow, int stillPending, double seconds) {
}
