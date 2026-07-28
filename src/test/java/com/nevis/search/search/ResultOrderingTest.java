package com.nevis.search.search;

import com.nevis.search.search.dto.Channel;
import com.nevis.search.search.dto.ResultType;
import com.nevis.search.search.dto.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ResultOrderingTest {

    private SearchHit client(String label) {
        UUID id = UUID.randomUUID();
        return SearchHit.client(id, 0.5, List.of(Channel.DIRECT),
                new SearchHit.ClientRef(id, label, "x", "e@x.com", null));
    }

    private SearchHit document(String label) {
        UUID id = UUID.randomUUID();
        return SearchHit.document(id, 0.7, List.of(Channel.SEMANTIC),
                new SearchHit.ClientRef(UUID.randomUUID(), "owner", "x", null, null),
                new SearchHit.DocumentRef(id, label, "snippet", null));
    }

    @Test
    void directClientsPrecedeDocumentsPrecedeFuzzyClients() {
        SearchHit direct = client("direct");
        SearchHit doc = document("doc");
        SearchHit fuzzy = client("fuzzy");

        List<SearchHit> ordered = ResultOrdering.order(
                List.of(direct), List.of(doc), List.of(fuzzy), 100);

        assertThat(ordered).containsExactly(direct, doc, fuzzy);
        assertThat(ordered.get(0).type()).isEqualTo(ResultType.CLIENT);
        assertThat(ordered.get(1).type()).isEqualTo(ResultType.DOCUMENT);
        assertThat(ordered.get(2).type()).isEqualTo(ResultType.CLIENT);
    }

    @Test
    void limitIsRespectedAcrossTiers() {
        List<SearchHit> ordered = ResultOrdering.order(
                List.of(client("d1")),
                List.of(document("x1"), document("x2")),
                List.of(client("f1")),
                2);
        assertThat(ordered).hasSize(2);
    }

    @Test
    void emptyTiersProduceEmptyResult() {
        assertThat(ResultOrdering.order(List.of(), List.of(), List.of(), 20)).isEmpty();
    }
}
