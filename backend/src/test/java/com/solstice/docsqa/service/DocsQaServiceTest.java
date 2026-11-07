package com.solstice.docsqa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solstice.docsqa.model.AskResponse;
import com.solstice.docsqa.model.DocsFile;
import com.solstice.docsqa.search.Synonyms;
import com.solstice.docsqa.search.Tokenizer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocsQaServiceTest {

    private static DocsQaService service;
    private static JsonNode examples;

    @BeforeAll
    static void setUp() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Synonyms synonyms;
        try (InputStream in = DocsQaServiceTest.class.getResourceAsStream("/data/synonyms.json")) {
            synonyms = new Synonyms(mapper.readValue(in, new TypeReference<Map<String, List<String>>>() {}));
        }
        try (InputStream in = DocsQaServiceTest.class.getResourceAsStream("/data/docs.json")) {
            service = new DocsQaService(mapper.readValue(in, DocsFile.class).docs(), new Tokenizer(synonyms));
        }
        try (InputStream in = DocsQaServiceTest.class.getResourceAsStream("/data/example_questions.json")) {
            examples = mapper.readTree(in).get("example_questions");
        }
    }

    @Test
    void answersCoveredQuestionsFromExpectedDoc() {
        for (JsonNode q : examples) {
            JsonNode expected = q.get("expected_doc_id");
            if (expected == null || expected.isNull()) {
                continue;
            }
            AskResponse r = service.ask(q.get("question").asText());
            assertTrue(r.answered(), q.get("question_id").asText() + " should be answered");
            assertEquals(expected.asText(), r.sourceDocId(), q.get("question_id").asText());
        }
    }

    @Test
    void refusesQuestionsNotCoveredByDocs() {
        for (JsonNode q : examples) {
            JsonNode expected = q.get("expected_doc_id");
            if (expected != null && !expected.isNull()) {
                continue;
            }
            AskResponse r = service.ask(q.get("question").asText());
            assertFalse(r.answered(), q.get("question_id").asText() + " should be refused");
            assertEquals(DocsQaService.NOT_FOUND, r.answer());
        }
    }
}
