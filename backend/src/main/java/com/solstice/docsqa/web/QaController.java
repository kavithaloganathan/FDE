package com.solstice.docsqa.web;

import com.solstice.docsqa.model.AskRequest;
import com.solstice.docsqa.model.AskResponse;
import com.solstice.docsqa.model.Doc;
import com.solstice.docsqa.model.FeedbackRequest;
import com.solstice.docsqa.model.StoredFeedback;
import com.solstice.docsqa.service.DocsQaService;
import com.solstice.docsqa.service.FeedbackStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;

@RestController
@RequestMapping("/api")
public class QaController {

    private final DocsQaService service;
    private final FeedbackStore feedbackStore;

    public QaController(DocsQaService service, FeedbackStore feedbackStore) {
        this.service = service;
        this.feedbackStore = feedbackStore;
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return service.ask(request.question());
    }

    @GetMapping("/docs/{docId}")
    public ResponseEntity<Doc> article(@PathVariable String docId) {
        return service.findDoc(docId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/feedback")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void feedback(@Valid @RequestBody FeedbackRequest request) throws IOException {
        feedbackStore.append(new StoredFeedback(Instant.now().toString(), request));
    }
}
