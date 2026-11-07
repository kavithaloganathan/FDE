package com.solstice.docsqa.service;

import com.solstice.docsqa.model.AskResponse;
import com.solstice.docsqa.model.Doc;
import com.solstice.docsqa.search.Bm25Index;
import com.solstice.docsqa.search.Passage;
import com.solstice.docsqa.search.Tokenizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Extractive answering: BM25 picks passages, a coverage gate decides whether
 * the docs really address the question, and otherwise we refuse.
 */
public class DocsQaService {

    public static final String NOT_FOUND = "I don't have an answer for that in the Solstice documentation.";

    static final double MIN_COVERAGE = 0.5;

    private final List<Doc> docs;
    private final Tokenizer tokenizer;
    private final Map<String, Set<String>> docTerms = new HashMap<>();
    private final Map<String, List<Passage>> passagesByDoc = new HashMap<>();
    private final Bm25Index index;

    public DocsQaService(List<Doc> docs, Tokenizer tokenizer) {
        this.docs = List.copyOf(docs);
        this.tokenizer = tokenizer;
        List<Passage> passages = new ArrayList<>();
        for (Doc doc : this.docs) {
            docTerms.put(doc.docId(), new HashSet<>(tokenizer.tokenize(doc.title() + " " + doc.content())));
            List<Passage> docPassages = toPassages(doc);
            passagesByDoc.put(doc.docId(), docPassages);
            passages.addAll(docPassages);
        }
        this.index = new Bm25Index(passages);
    }

    public AskResponse ask(String question) {
        List<String> queryTerms = tokenizer.tokenize(question).stream().distinct().toList();
        if (queryTerms.isEmpty()) {
            return notAnswered(0.0);
        }

        Map<String, Double> coverage = new HashMap<>();
        for (Doc doc : docs) {
            Set<String> terms = docTerms.get(doc.docId());
            long matched = queryTerms.stream().filter(terms::contains).count();
            coverage.put(doc.docId(), (double) matched / queryTerms.size());
        }

        Optional<Passage> top = index.score(queryTerms).stream()
                .map(Bm25Index.Scored::passage)
                .filter(p -> coverage.get(p.docId()) >= MIN_COVERAGE)
                .findFirst();
        if (top.isEmpty()) {
            return notAnswered(0.0);
        }

        Doc doc = findDoc(top.get().docId()).orElseThrow();
        return new AskResponse(true, contextAround(top.get(), queryTerms), doc.docId(), doc.title(),
                coverage.get(doc.docId()));
    }

    public Optional<Doc> findDoc(String docId) {
        return docs.stream().filter(d -> d.docId().equals(docId)).findFirst();
    }

    /** Top passage plus adjacent sentences that share a query term. */
    private String contextAround(Passage top, List<String> queryTerms) {
        List<Passage> siblings = passagesByDoc.get(top.docId());
        List<String> parts = new ArrayList<>();
        for (int i = top.index() - 1; i <= top.index() + 1; i++) {
            if (i < 0 || i >= siblings.size()) {
                continue;
            }
            Passage p = siblings.get(i);
            if (i == top.index() || queryTerms.stream().anyMatch(p.termFreq()::containsKey)) {
                parts.add(p.text());
            }
        }
        return String.join(" ", parts);
    }

    private static AskResponse notAnswered(double confidence) {
        return new AskResponse(false, NOT_FOUND, null, null, confidence);
    }

    private List<Passage> toPassages(Doc doc) {
        List<Passage> out = new ArrayList<>();
        String[] sentences = doc.content().split("(?<=[.!?])\\s+");
        for (int i = 0; i < sentences.length; i++) {
            List<String> tokens = tokenizer.tokenize(sentences[i]);
            Map<String, Integer> tf = new HashMap<>();
            tokens.forEach(t -> tf.merge(t, 1, Integer::sum));
            out.add(new Passage(doc.docId(), doc.title(), i, sentences[i].trim(), tf, tokens.size()));
        }
        return out;
    }
}
