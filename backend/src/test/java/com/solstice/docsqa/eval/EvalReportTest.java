package com.solstice.docsqa.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solstice.docsqa.model.AskResponse;
import com.solstice.docsqa.model.DocsFile;
import com.solstice.docsqa.search.Synonyms;
import com.solstice.docsqa.search.Tokenizer;
import com.solstice.docsqa.service.DocsQaService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Run with: gradlew evalDocs. Excluded from the normal test task. */
class EvalReportTest {

    private static final double MIN_CORRECT_DOC_RATE = 0.8;
    private static final double MIN_REFUSAL_RATE = 0.8;

    @Test
    void reportsAccuracyAndRefusalOnLabeledSet() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Synonyms synonyms;
        try (InputStream in = getClass().getResourceAsStream("/data/synonyms.json")) {
            synonyms = new Synonyms(mapper.readValue(in, new TypeReference<Map<String, List<String>>>() {}));
        }
        DocsQaService service;
        try (InputStream in = getClass().getResourceAsStream("/data/docs.json")) {
            service = new DocsQaService(mapper.readValue(in, DocsFile.class).docs(), new Tokenizer(synonyms));
        }
        JsonNode questions;
        try (InputStream in = getClass().getResourceAsStream("/data/eval_questions.json")) {
            questions = mapper.readTree(in).get("questions");
        }

        int answerable = 0;
        int correct = 0;
        int unanswerable = 0;
        int refused = 0;
        int wrong = 0;

        for (JsonNode q : questions) {
            String id = q.get("question_id").asText();
            String question = q.get("question").asText();
            JsonNode expectedNode = q.get("expected_doc_id");
            String expected = expectedNode == null || expectedNode.isNull() ? null : expectedNode.asText();
            AskResponse r = service.ask(question);

            if (expected == null) {
                unanswerable++;
                if (r.answered()) {
                    wrong++;
                    System.out.println("WRONG ANSWER " + id + ": " + question + " -> " + r.sourceDocId());
                } else {
                    refused++;
                }
            } else {
                answerable++;
                if (r.answered() && expected.equals(r.sourceDocId())) {
                    correct++;
                } else {
                    if (r.answered()) {
                        wrong++;
                    }
                    System.out.println("MISS " + id + ": " + question + " -> "
                            + (r.answered() ? r.sourceDocId() : "refused"));
                }
            }
        }

        double correctRate = (double) correct / answerable;
        double refusalRate = (double) refused / unanswerable;
        System.out.printf("Answerable correct doc: %d/%d (%.0f%%)%n", correct, answerable, correctRate * 100);
        System.out.printf("Unanswerable refused:   %d/%d (%.0f%%)%n", refused, unanswerable, refusalRate * 100);
        System.out.printf("Wrong answers total:    %d%n", wrong);

        assertTrue(correctRate >= MIN_CORRECT_DOC_RATE, "correct-doc rate below " + MIN_CORRECT_DOC_RATE);
        assertTrue(refusalRate >= MIN_REFUSAL_RATE, "refusal rate below " + MIN_REFUSAL_RATE);
    }
}
