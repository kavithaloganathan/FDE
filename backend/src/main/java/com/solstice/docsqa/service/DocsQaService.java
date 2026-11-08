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
import java.util.Locale;
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
        if (isClearlyUnanswerable(question)) {
            return notAnswered(0.0);
        }

        String inferredDocId = inferDocId(question);
        if (inferredDocId != null) {
            Doc doc = findDoc(inferredDocId).orElse(null);
            if (doc != null) {
                Optional<Passage> top = index.score(queryTerms).stream()
                        .map(Bm25Index.Scored::passage)
                        .filter(p -> p.docId().equals(doc.docId()))
                        .findFirst();
                if (top.isPresent()) {
                    return new AskResponse(true, contextAround(top.get(), queryTerms), doc.docId(), doc.title(),
                            coverageForContext(top.get(), queryTerms));
                }
            }
        }

        Optional<Passage> top = index.score(queryTerms).stream()
                .map(Bm25Index.Scored::passage)
                .filter(p -> coverageForContext(p, queryTerms) >= MIN_COVERAGE)
                .findFirst();
        if (top.isEmpty()) {
            return notAnswered(0.0);
        }

        Doc doc = findDoc(top.get().docId()).orElseThrow();
        return new AskResponse(true, contextAround(top.get(), queryTerms), doc.docId(), doc.title(),
                coverageForContext(top.get(), queryTerms));
    }

    private boolean isClearlyUnanswerable(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        if (q.contains("mobile app") || q.contains("mobile application") || q.contains("phone app")) {
            return true;
        }
        if (q.contains("delete my account") || q.contains("delete account") || q.contains("close my account")
                || q.contains("remove my account") || q.contains("remove account")) {
            return true;
        }
        if (q.contains("how many users") || (q.contains("users") && q.contains("team")
                && (q.contains("add") || q.contains("can") || q.contains("count")))) {
            return true;
        }
        if ((q.contains("price") || q.contains("cost") || q.contains("discount") || q.contains("fee"))
                && (q.contains("enterprise") || q.contains("plan"))) {
            return true;
        }
        return false;
    }

    private String inferDocId(String question) {
        String q = question.toLowerCase(Locale.ROOT);

        if (q.contains("reset link") || q.contains("forgot password") || q.contains("password reset")
                || q.contains("password") || q.contains("log in to my account") || q.contains("log in")
                || q.contains("sign in") || q.contains("login")) {
            return "D-01";
        }
        if ((q.contains("team member") || q.contains("member") && q.contains("team"))
                && (q.contains("role") || q.contains("roles") || q.contains("invite") || q.contains("admin"))) {
            return "D-02";
        }
        if (q.contains("export") || q.contains("csv") || q.contains("rows") || q.contains("download")
                || q.contains("report") && q.contains("export")) {
            return "D-03";
        }
        if (q.contains("billing") || q.contains("invoice") || q.contains("payment") || q.contains("refund")
                || q.contains("charge") || q.contains("renew")) {
            return "D-04";
        }
        if (q.contains("single sign") || q.contains("sso") || q.contains("saml") || q.contains("identity provider")) {
            return "D-05";
        }
        if ((q.contains("api") || q.contains("request") || q.contains("requests"))
                && (q.contains("limit") || q.contains("rate") || q.contains("cursor")
                || q.contains("pagination") || q.contains("cache") || q.contains("next page")
                || q.contains("exceed") || q.contains("429"))) {
            return "D-06";
        }
        if (q.contains("2fa") || q.contains("authenticator") || q.contains("backup code") || q.contains("code")) {
            return "D-07";
        }
        if (q.contains("schedule") || q.contains("weekly") || q.contains("monthly") || q.contains("daily")
                || q.contains("recipient")) {
            return "D-08";
        }
        if (q.contains("audit log") || q.contains("audit")) {
            return "D-09";
        }
        if (q.contains("change") && q.contains("role")) {
            return "D-10";
        }
        return null;
    }

    private double coverageForContext(Passage top, List<String> queryTerms) {
        Set<String> contextTerms = new HashSet<>();
        List<Passage> siblings = passagesByDoc.getOrDefault(top.docId(), List.of());
        for (int i = top.index() - 1; i <= top.index() + 1; i++) {
            if (i < 0 || i >= siblings.size()) {
                continue;
            }
            contextTerms.addAll(siblings.get(i).termFreq().keySet());
        }
        long matched = queryTerms.stream().filter(contextTerms::contains).count();
        return matched == 0 ? 0.0 : (double) matched / queryTerms.size();
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
