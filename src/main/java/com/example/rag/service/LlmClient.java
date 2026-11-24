package com.example.rag.service;

/**
 * Talks to a language model and returns the raw JSON text it produced.
 * Today: Ollama (free, local). Later you could add Gemini or OpenAI behind the same interface.
 */
public interface LlmClient {

    String generateJson(String systemPrompt, String userPrompt);
}
