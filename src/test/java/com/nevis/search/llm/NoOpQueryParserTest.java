package com.nevis.search.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpQueryParserTest {

    @Test
    void takesEveryQueryLiterallyWithNoScope() {
        ParsedQuery pq = new NoOpQueryParser().parse("address proof of John Doe");

        assertThat(pq.scopedToClient()).isFalse();
        assertThat(pq.documentQuery()).isEqualTo("address proof of John Doe");
        assertThat(pq.clientQuery()).isEqualTo("address proof of John Doe");
    }
}
