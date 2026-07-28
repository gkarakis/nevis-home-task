package com.nevis.search.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the model-response interpretation ({@code fromJson}) without a live client.
 * The rule: only scope to a client on a confident parse; anything else stays plain.
 */
class AnthropicQueryParserTest {

    private static final String RAW = "address proof of John Doe";

    @Test
    void compoundJsonProducesScopedQuery() {
        ParsedQuery pq = AnthropicQueryParser.fromJson(
                RAW, "{\"concept\": \"address proof\", \"client_name\": \"John Doe\"}");

        assertThat(pq.scopedToClient()).isTrue();
        assertThat(pq.documentQuery()).isEqualTo("address proof");
        assertThat(pq.clientQuery()).isEqualTo("John Doe");
    }

    @Test
    void jsonWrappedInCodeFencesIsTolerated() {
        ParsedQuery pq = AnthropicQueryParser.fromJson(
                RAW, "```json\n{\"concept\":\"address proof\",\"client_name\":\"John Doe\"}\n```");

        assertThat(pq.scopedToClient()).isTrue();
        assertThat(pq.documentQuery()).isEqualTo("address proof");
    }

    @Test
    void blankClientNameFallsBackToPlain() {
        ParsedQuery pq = AnthropicQueryParser.fromJson(
                RAW, "{\"concept\": \"address proof\", \"client_name\": \"\"}");

        assertThat(pq.scopedToClient()).isFalse();
        assertThat(pq.documentQuery()).isEqualTo(RAW);
        assertThat(pq.clientQuery()).isEqualTo(RAW);
    }

    @Test
    void missingConceptFallsBackToPlain() {
        ParsedQuery pq = AnthropicQueryParser.fromJson(RAW, "{\"client_name\": \"John Doe\"}");

        assertThat(pq.scopedToClient()).isFalse();
        assertThat(pq.documentQuery()).isEqualTo(RAW);
    }

    @Test
    void malformedResponseFallsBackToPlain() {
        ParsedQuery pq = AnthropicQueryParser.fromJson(RAW, "sorry, I can't help with that");

        assertThat(pq.scopedToClient()).isFalse();
        assertThat(pq.documentQuery()).isEqualTo(RAW);
    }
}
