package com.nevis.search.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractiveSummarizerTest {

    @Test
    void collapsesWhitespaceAndReturnsShortContentWhole() {
        Summarizer s = new ExtractiveSummarizer(160);
        assertThat(s.summarize("Title", "  hello   world \n foo ")).isEqualTo("hello world foo");
    }

    @Test
    void truncatesLongContentWithEllipsis() {
        Summarizer s = new ExtractiveSummarizer(10);
        assertThat(s.summarize("Title", "abcdefghijklmnop")).isEqualTo("abcdefghij…");
    }

    @Test
    void nullContentYieldsEmptyString() {
        assertThat(new ExtractiveSummarizer(160).summarize("Title", null)).isEmpty();
    }

    @Test
    void sharedClampAlsoBoundsLlmOutput() {
        assertThat(SummaryText.clamp("abcdefghijklmnop", 10)).isEqualTo("abcdefghij…");
    }
}
