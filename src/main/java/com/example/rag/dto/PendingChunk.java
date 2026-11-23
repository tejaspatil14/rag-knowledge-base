package com.example.rag.dto;

/** A chunk that still needs an embedding. */
public record PendingChunk(long id, String content) {
}
