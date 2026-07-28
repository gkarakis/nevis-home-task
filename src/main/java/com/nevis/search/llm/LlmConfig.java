package com.nevis.search.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Wires the LLM features at startup. The API key's presence is the flag: a key wires
 * the hosted implementations; no key wires the local fallbacks, so the app never
 * touches the network. One decision per feature, made once and logged so reviewers can
 * see which mode is live. Both features share a single client, built lazily only when
 * something is actually active.
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    private AnthropicClient summaryClient;
    private AnthropicClient queryParserClient;

    @Bean
    public Summarizer summarizer(LlmProperties props) {
        Summarizer extractive = new ExtractiveSummarizer(props.maxSummaryChars());
        if (!props.active()) {
            log.info("LLM summariser DISABLED (no ANTHROPIC_API_KEY) — using extractive summaries.");
            return extractive;
        }
        log.info("LLM summariser ENABLED (model={}).", props.model());
        return new AnthropicSummarizer(summaryClient(props), props, extractive);
    }

    @Bean
    public QueryParser queryParser(LlmProperties props) {
        if (!props.queryParsingActive()) {
            log.info("LLM query parsing DISABLED — queries are searched literally.");
            return new NoOpQueryParser();
        }
        log.info("LLM query parsing ENABLED (model={}, timeoutMs={}).",
                props.model(), props.queryTimeoutMs());
        return new AnthropicQueryParser(queryParserClient(props), props);
    }

    private AnthropicClient summaryClient(LlmProperties props) {
        if (summaryClient == null) {
            summaryClient = AnthropicOkHttpClient.builder()
                    .apiKey(props.apiKey())
                    .timeout(Duration.ofMillis(props.timeoutMs()))
                    .build();
        }
        return summaryClient;
    }

    private AnthropicClient queryParserClient(LlmProperties props) {
        if (queryParserClient == null) {
            queryParserClient = AnthropicOkHttpClient.builder()
                    .apiKey(props.apiKey())
                    .timeout(Duration.ofMillis(props.queryTimeoutMs()))
                    .build();
        }
        return queryParserClient;
    }
}
