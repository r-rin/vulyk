package com.github.rrin.vulyk.domain.entity.post;

public enum PostState {
    DRAFT,
    PUBLISHED,
    DELETED,
    HIDDEN,
    REDACTED;

    private static final String DRAFT_DISC = "DRAFT";
    private static final String PUBLISHED_DISC = "PUBLISHED";
    private static final String DELETED_DISC = "DELETED";
    private static final String HIDDEN_DISC = "HIDDEN";
    private static final String REDACTED_DISC = "REDACTED";
}
