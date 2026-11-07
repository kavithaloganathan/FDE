package com.solstice.docsqa.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(
        @NotBlank @Size(max = 500) String question,
        @Size(max = 2000) String answer,
        String sourceDocId,
        boolean helpful,
        @Size(max = 1000) String comment) {}
