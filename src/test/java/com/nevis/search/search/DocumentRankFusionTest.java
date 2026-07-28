package com.nevis.search.search;

import com.nevis.search.search.dto.Channel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentRankFusionTest {

    private static final int K = 60;

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID c = UUID.randomUUID();

    private DocCandidate doc(UUID id, double score) {
        return new DocCandidate(id, UUID.randomUUID(), "title-" + id, score);
    }

    @Test
    void documentInBothListsOutranksHigherPlacedDocumentInOne() {
        // 'a' is #1 semantically; 'b' is #2 semantic and #1 lexical. 'b' wins on fusion.
        List<DocCandidate> semantic = List.of(doc(a, 0.9), doc(b, 0.8));
        List<DocCandidate> lexical = List.of(doc(b, 0.5));

        List<FusedDocument> fused = DocumentRankFusion.fuse(semantic, lexical, K);

        assertThat(fused).extracting(FusedDocument::documentId)
                .containsExactly(b, a);
        assertThat(fused.get(0).matchedBy())
                .containsExactly(Channel.SEMANTIC, Channel.LEXICAL);
    }

    @Test
    void singleSignalDocumentsRetainTheirScoreAndChannel() {
        List<FusedDocument> fused = DocumentRankFusion.fuse(
                List.of(doc(a, 0.71)), List.of(doc(c, 0.4)), K);

        FusedDocument onlyA = fused.stream().filter(f -> f.documentId().equals(a)).findFirst().orElseThrow();
        assertThat(onlyA.matchedBy()).containsExactly(Channel.SEMANTIC);
        assertThat(onlyA.displayScore()).isEqualTo(0.71);

        FusedDocument onlyC = fused.stream().filter(f -> f.documentId().equals(c)).findFirst().orElseThrow();
        assertThat(onlyC.matchedBy()).containsExactly(Channel.LEXICAL);
    }

    @Test
    void emptyListsAreHandled() {
        assertThat(DocumentRankFusion.fuse(List.of(), List.of(), K)).isEmpty();
    }

    @Test
    void orderingIsDeterministicForEqualFusedScores() {
        // Each doc appears once at rank 1 of its own single-element list -> equal scores.
        List<FusedDocument> first = DocumentRankFusion.fuse(List.of(doc(a, 0.5)), List.of(doc(b, 0.5)), K);
        List<FusedDocument> second = DocumentRankFusion.fuse(List.of(doc(a, 0.5)), List.of(doc(b, 0.5)), K);
        assertThat(first.stream().map(FusedDocument::documentId).toList())
                .isEqualTo(second.stream().map(FusedDocument::documentId).toList());
    }
}
