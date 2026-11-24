package com.example.rag.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * The JSON we require from the language model. Bean Validation rejects anything that does not fit.
 * usedChunks holds the numbers [1], [2] ... of the context chunks the model says it used.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmAnswer(
        @NotBlank String answer,
        @NotNull List<Integer> usedChunks,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double confidence) {
}
