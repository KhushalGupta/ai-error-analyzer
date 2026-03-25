package com.ai.error_analyzer.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalyzeRequest(@NotBlank(message = "Log content is required") String log) {
}
