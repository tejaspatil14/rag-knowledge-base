package com.example.rag.dto;

import java.time.Instant;

/** One consistent error shape for the whole API. */
public record ErrorResponse(String timestamp, int status, String error, String message) {

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(Instant.now().toString(), status, error, message);
    }
}
