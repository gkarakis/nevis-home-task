package com.nevis.search.search;

import java.util.UUID;

/** A document row returned by one of the two document channels (B1 semantic, B2 lexical). */
public record DocCandidate(
        UUID documentId,
        UUID clientId,
        String title,
        double score
) {
}
