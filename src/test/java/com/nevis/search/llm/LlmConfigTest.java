package com.nevis.search.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The feature flag's contract: the summariser picked at startup depends only on
 * whether the app is enabled and a key is present. No network is touched here —
 * building the client does not call the API.
 */
class LlmConfigTest {

    private final LlmConfig config = new LlmConfig();

    @Test
    void extractiveWhenNoApiKey() {
        LlmProperties props = new LlmProperties(true, "", null, 0, 0);
        assertThat(config.summarizer(props)).isInstanceOf(ExtractiveSummarizer.class);
    }

    @Test
    void extractiveWhenBlankApiKey() {
        LlmProperties props = new LlmProperties(true, "   ", null, 0, 0);
        assertThat(config.summarizer(props)).isInstanceOf(ExtractiveSummarizer.class);
    }

    @Test
    void extractiveWhenDisabledEvenWithKey() {
        LlmProperties props = new LlmProperties(false, "sk-ant-xxx", null, 0, 0);
        assertThat(config.summarizer(props)).isInstanceOf(ExtractiveSummarizer.class);
    }

    @Test
    void anthropicWhenEnabledAndKeyPresent() {
        LlmProperties props = new LlmProperties(true, "sk-ant-test", null, 0, 0);
        assertThat(config.summarizer(props)).isInstanceOf(AnthropicSummarizer.class);
    }
}
