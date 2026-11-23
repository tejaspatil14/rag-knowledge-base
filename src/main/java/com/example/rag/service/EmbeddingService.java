package com.example.rag.service;

import java.util.List;

/**
 * Turns text into vectors. Today the only implementation is Ollama (free, local).
 * Because everything else depends on this interface, you can add an OpenAI or Gemini
 * implementation later without touching the rest of the project.
 */
public interface EmbeddingService {

    /** For chunks that are stored in the database. */
    List<float[]> embedDocuments(List<String> texts);

    /** For a user's question (used from Week 4). */
    float[] embedQuery(String text);
}
