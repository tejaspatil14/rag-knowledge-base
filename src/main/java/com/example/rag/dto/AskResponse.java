package com.example.rag.dto;

import java.time.Instant;
import java.util.List;

public record AskResponse(
        String question,
        String answer,
        List<SourceRef> sources,
        double confidence,
        Instant timestamp) {
}
