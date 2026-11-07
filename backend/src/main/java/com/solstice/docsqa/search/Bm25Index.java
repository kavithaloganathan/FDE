package com.solstice.docsqa.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Okapi BM25 scoring over passages. */
public final class Bm25Index {

    private static final double K1 = 1.2;
    private static final double B = 0.75;

    private final List<Passage> passages;
    private final Map<String, Integer> docFreq = new HashMap<>();
    private final double avgLength;

    public Bm25Index(List<Passage> passages) {
        this.passages = List.copyOf(passages);
        for (Passage p : this.passages) {
            for (String term : p.termFreq().keySet()) {
                docFreq.merge(term, 1, Integer::sum);
            }
        }
        this.avgLength = this.passages.stream().mapToInt(Passage::length).average().orElse(1.0);
    }

    /** Returns passages with a positive score, highest first. */
    public List<Scored> score(List<String> queryTerms) {
        int n = passages.size();
        List<Scored> results = new ArrayList<>();
        for (Passage p : passages) {
            double score = 0;
            for (String term : queryTerms) {
                int tf = p.termFreq().getOrDefault(term, 0);
                if (tf == 0) {
                    continue;
                }
                int df = docFreq.getOrDefault(term, 0);
                double idf = Math.log(1 + (n - df + 0.5) / (df + 0.5));
                double norm = tf * (K1 + 1) / (tf + K1 * (1 - B + B * p.length() / avgLength));
                score += idf * norm;
            }
            if (score > 0) {
                results.add(new Scored(p, score));
            }
        }
        results.sort(Comparator.comparingDouble(Scored::score).reversed());
        return results;
    }

    public record Scored(Passage passage, double score) {}
}
