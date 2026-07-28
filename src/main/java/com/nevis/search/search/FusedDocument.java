package com.nevis.search.search;

import com.nevis.search.search.dto.Channel;

import java.util.List;
import java.util.UUID;

/**
 * A document after reciprocal rank fusion of the two channels.
 *
 * @param fusedScore the RRF score used only to order documents against each other
 * @param displayScore relevance shown to the client — cosine similarity when the
 *                     document matched semantically, otherwise the lexical rank score
 * @param matchedBy    which channels found it, in a stable order (SEMANTIC, LEXICAL)
 */
public record FusedDocument(
        UUID documentId,
        UUID clientId,
        String title,
        double fusedScore,
        double displayScore,
        List<Channel> matchedBy
) {
}
