package com.solstice.docsqa.model;

/** confidence is the share of question terms found in the source doc (0.0 to 1.0). */
public record AskResponse(
        boolean answered,
        String answer,
        String sourceDocId,
        String sourceTitle,
        double confidence) {}
