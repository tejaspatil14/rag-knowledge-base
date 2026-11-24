package com.example.rag.dto;

import java.util.List;

public record SearchResponse(String query, int count, List<SearchResult> results) {
}
