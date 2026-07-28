package com.nevis.search.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The feature flag's contract: the implementations picked at startup depend only on
 * whether the app is enabled, a key is present, and (for parsing) the parse-queries
 * switch. No network is touched here — building the client does not call the API.
 */
class LlmConfigTest {

    private final LlmConfig config = new LlmConfig();

    private static LlmProperties props(boolean enabled, String apiKey, boolean parseQueries) {
        return new LlmProperties(enabled, apiKey, null, 0, 0, 0, parseQueries);
    }

    @Test
    void extractiveSummariserWhenNoApiKey() {
        assertThat(config.summarizer(props(true, "", true))).isInstanceOf(ExtractiveSummarizer.class);
    }

    @Test
    void extractiveSummariserWhenBlankApiKey() {
        assertThat(config.summarizer(props(true, "   ", true))).isInstanceOf(ExtractiveSummarizer.class);
    }

    @Test
    void extractiveSummariserWhenDisabledEvenWithKey() {
        assertThat(config.summarizer(props(false, "sk-ant-xxx", true))).isInstanceOf(ExtractiveSummarizer.class);
    }

    @Test
    void anthropicSummariserWhenEnabledAndKeyPresent() {
        assertThat(config.summarizer(props(true, "sk-ant-test", true))).isInstanceOf(AnthropicSummarizer.class);
    }

    @Test
    void noOpParserWhenNoApiKey() {
        assertThat(config.queryParser(props(true, "", true))).isInstanceOf(NoOpQueryParser.class);
    }

    @Test
    void noOpParserWhenParsingDisabledEvenWithKey() {
        assertThat(config.queryParser(props(true, "sk-ant-test", false))).isInstanceOf(NoOpQueryParser.class);
    }

    @Test
    void anthropicParserWhenEnabledAndKeyPresentAndParsingOn() {
        assertThat(config.queryParser(props(true, "sk-ant-test", true))).isInstanceOf(AnthropicQueryParser.class);
    }
}
