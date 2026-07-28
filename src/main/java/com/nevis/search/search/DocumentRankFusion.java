package com.nevis.search.search;

import com.nevis.search.search.dto.Channel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reciprocal rank fusion over the semantic and lexical document channels.
 *
 * <pre>
 *   fused(doc) = Σ over the lists containing it of  1 / (k + rank)
 * </pre>
 *
 * rank is 1-based within its list; {@code k = 60} is the conventional default.
 * A document found by both signals outranks one found by a single signal —
 * agreement between independent methods is real evidence. This ordering applies
 * only among documents, never against clients.
 *
 * <p>Pure function, no Spring. Cosine similarity and {@code ts_rank_cd} are not
 * comparable numbers, so we fuse by rank position rather than by raw score.
 */
public final class DocumentRankFusion {

    private DocumentRankFusion() {
    }

    public static List<FusedDocument> fuse(List<DocCandidate> semantic,
                                           List<DocCandidate> lexical,
                                           int k) {
        // Preserve first-seen order for a stable, deterministic result.
        Map<UUID, Accumulator> byDoc = new LinkedHashMap<>();

        accumulate(byDoc, semantic, k, Channel.SEMANTIC);
        accumulate(byDoc, lexical, k, Channel.LEXICAL);

        List<FusedDocument> fused = new ArrayList<>(byDoc.size());
        for (Accumulator a : byDoc.values()) {
            List<Channel> matchedBy = new ArrayList<>(2);
            if (a.semanticScore != null) matchedBy.add(Channel.SEMANTIC);
            if (a.lexicalScore != null) matchedBy.add(Channel.LEXICAL);
            double displayScore = a.semanticScore != null ? a.semanticScore : a.lexicalScore;
            fused.add(new FusedDocument(a.documentId, a.clientId, a.title,
                    a.fusedScore, displayScore, List.copyOf(matchedBy)));
        }

        // Ties break on documentId for determinism.
        fused.sort(Comparator
                .comparingDouble(FusedDocument::fusedScore).reversed()
                .thenComparing(FusedDocument::documentId));
        return fused;
    }

    private static void accumulate(Map<UUID, Accumulator> byDoc, List<DocCandidate> list,
                                   int k, Channel channel) {
        for (int i = 0; i < list.size(); i++) {
            DocCandidate c = list.get(i);
            Accumulator acc = byDoc.computeIfAbsent(c.documentId(),
                    id -> new Accumulator(c.documentId(), c.clientId(), c.title()));
            acc.fusedScore += 1.0 / (k + i + 1);
            if (channel == Channel.SEMANTIC) {
                acc.semanticScore = c.score();
            } else {
                acc.lexicalScore = c.score();
            }
        }
    }

    private static final class Accumulator {
        final UUID documentId;
        final UUID clientId;
        final String title;
        double fusedScore;
        Double semanticScore;
        Double lexicalScore;

        Accumulator(UUID documentId, UUID clientId, String title) {
            this.documentId = documentId;
            this.clientId = clientId;
            this.title = title;
        }
    }
}
