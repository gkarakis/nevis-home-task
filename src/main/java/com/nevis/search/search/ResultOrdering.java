package com.nevis.search.search;

import com.nevis.search.search.dto.SearchHit;

import java.util.List;
import java.util.stream.Stream;

/**
 * The cross-type rule. There is no principled way to compare "this client's email
 * contains your query" with "this document is semantically about your query", so
 * cross-type ordering is a stated product decision, not a calculation:
 *
 * <ol>
 *   <li>Clients matched directly (substring on name or email) — a direct identity
 *       match is a near-certain intent signal.</li>
 *   <li>Documents, ordered by fused relevance.</li>
 *   <li>Clients matched only fuzzily (trigram similarity above the floor).</li>
 * </ol>
 *
 * Ties inside a tier break on score, then on id — already applied by the source
 * queries and the fusion step, so this class only concatenates and limits.
 */
public final class ResultOrdering {

    private ResultOrdering() {
    }

    public static List<SearchHit> order(List<SearchHit> directClients,
                                        List<SearchHit> fusedDocuments,
                                        List<SearchHit> fuzzyClients,
                                        int limit) {
        return Stream.of(directClients, fusedDocuments, fuzzyClients)
                .flatMap(List::stream)
                .limit(limit)
                .toList();
    }
}
