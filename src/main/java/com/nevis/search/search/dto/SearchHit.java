package com.nevis.search.search.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One element of the ranked result array — a discriminated union on {@code type}.
 * {@code score} is relevance <em>within its own type</em> (cosine similarity for
 * documents, trigram similarity for clients), so array order — which follows the
 * tier rule — is not globally monotonic. Null fields are omitted from the wire.
 */
public record SearchHit(
        ResultType type,
        UUID id,
        double score,
        List<Channel> matchedBy,
        ClientRef client,
        DocumentRef document
) {

    public record ClientRef(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String description
    ) {}

    public record DocumentRef(
            UUID id,
            String title,
            String snippet,
            Instant createdAt
    ) {}

    public static SearchHit client(UUID id, double score, List<Channel> matchedBy,
                                   ClientRef client) {
        return new SearchHit(ResultType.CLIENT, id, score, matchedBy, client, null);
    }

    public static SearchHit document(UUID id, double score, List<Channel> matchedBy,
                                     ClientRef owner, DocumentRef document) {
        return new SearchHit(ResultType.DOCUMENT, id, score, matchedBy, owner, document);
    }
}
