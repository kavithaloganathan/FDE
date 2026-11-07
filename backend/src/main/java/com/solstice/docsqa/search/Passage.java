package com.solstice.docsqa.search;

import java.util.Map;

/** A sentence-level retrieval unit; index is its position within the doc. */
public record Passage(String docId, String title, int index, String text, Map<String, Integer> termFreq, int length) {}
