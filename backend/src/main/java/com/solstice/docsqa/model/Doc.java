package com.solstice.docsqa.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Doc(@JsonProperty("doc_id") String docId, String title, String content) {}
