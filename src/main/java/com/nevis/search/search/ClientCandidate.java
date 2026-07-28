package com.nevis.search.search;

import java.util.UUID;

/** A client row returned by query A. */
public record ClientCandidate(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String description,
        boolean directMatch,
        double score
) {
}
