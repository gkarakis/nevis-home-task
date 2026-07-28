package com.nevis.search.search;

import com.nevis.search.document.Document;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SearchServiceTest {

    @Test
    void nullStoredSummaryFallsBackToLegacyContentSnippet() {
        Document doc = new Document(UUID.randomUUID(), "Title", " alpha   beta \n gamma ", null);

        assertThat(SearchService.summary(doc, 160)).isEqualTo("alpha beta gamma");
    }

    @Test
    void storedSummaryIsCappedBeforeReturningAsSnippet() {
        Document doc = new Document(UUID.randomUUID(), "Title", "content", "abcdefghijklmnop");

        assertThat(SearchService.summary(doc, 10)).isEqualTo("abcdefghij…");
    }
}
