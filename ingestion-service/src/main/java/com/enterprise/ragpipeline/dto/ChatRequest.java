package com.enterprise.ragpipeline.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String question) {}
