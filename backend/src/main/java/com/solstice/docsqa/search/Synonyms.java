package com.solstice.docsqa.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Rewrites variant words and phrases to a canonical word before tokenizing. */
public final class Synonyms {

    private record Rule(Pattern pattern, String canonical, int variantLength) {}

    private final List<Rule> rules = new ArrayList<>();

    public Synonyms(Map<String, List<String>> groups) {
        for (Map.Entry<String, List<String>> group : groups.entrySet()) {
            String canonical = group.getKey().toLowerCase(Locale.ROOT);
            for (String variant : group.getValue()) {
                String v = variant.toLowerCase(Locale.ROOT);
                rules.add(new Rule(
                        Pattern.compile("(?<![a-z0-9])" + Pattern.quote(v) + "(?![a-z0-9])"),
                        canonical,
                        v.length()));
            }
        }
        // Longer phrases first so "log in" wins over a shorter overlapping variant.
        rules.sort(Comparator.comparingInt(Rule::variantLength).reversed());
    }

    public String apply(String text) {
        String out = text.toLowerCase(Locale.ROOT);
        for (Rule rule : rules) {
            out = rule.pattern().matcher(out).replaceAll(rule.canonical());
        }
        return out;
    }
}
