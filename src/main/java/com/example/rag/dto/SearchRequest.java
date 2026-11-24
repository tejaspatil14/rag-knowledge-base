package com.example.rag.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** topK and minScore are optional. If you leave them out, the values from application.yml are used. */
public record SearchRequest(
        @NotBlank @Size(max = 1000) String query,
        @Min(1) @Max(20) Integer topK,
        @DecimalMin("0.0") @DecimalMax("1.0") Double minScore) {
}
