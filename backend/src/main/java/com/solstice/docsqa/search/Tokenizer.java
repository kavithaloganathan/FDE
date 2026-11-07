package com.solstice.docsqa.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Applies synonyms, drops stopwords, and stems lightly (-ed, -s). */
public final class Tokenizer {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "before", "by", "can", "do", "does", "for", "from",
            "get", "how", "i", "if", "in", "is", "it", "its", "long", "many", "me", "much", "my", "need",
            "of", "on", "or", "our", "so", "that", "the", "their", "there", "this", "to", "us", "want",
            "we", "what", "when", "where", "which", "who", "why", "will", "with", "would", "you", "your",
            "ve", "ll", "re", "should", "could", "any", "all", "about");

    private final Synonyms synonyms;

    public Tokenizer(Synonyms synonyms) {
        this.synonyms = synonyms;
    }

    public List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        for (String raw : NON_ALNUM.split(synonyms.apply(text))) {
            if (!raw.isEmpty() && !STOPWORDS.contains(raw)) {
                tokens.add(stem(raw));
            }
        }
        return tokens;
    }

    static String stem(String token) {
        if (token.length() > 4 && token.endsWith("ed")) {
            return token.substring(0, token.length() - 2);
        }
        if (token.length() > 3 && token.endsWith("s")
                && !token.endsWith("ss") && !token.endsWith("us") && !token.endsWith("is")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }
}
