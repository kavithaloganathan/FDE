package com.solstice.docsqa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solstice.docsqa.model.StoredFeedback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Appends feedback as JSON lines so it can be reviewed and added to the eval set. */
@Component
public class FeedbackStore {

    private final Path file;
    private final ObjectMapper mapper;

    public FeedbackStore(@Value("${docs.feedback-file:data/feedback.jsonl}") String file, ObjectMapper mapper) {
        this.file = Path.of(file);
        this.mapper = mapper;
    }

    public synchronized void append(StoredFeedback feedback) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        String line = mapper.writeValueAsString(feedback) + System.lineSeparator();
        Files.writeString(file, line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
