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
 * Chooses the {@link Summarizer} implementation at startup. The API key's presence is
 * the flag: a key wires the LLM-backed summariser (with the extractive one as its
 * fallback); no key wires the extractive summariser alone, so the app never touches
 * the network. One decision, made once, logged so reviewers can see which mode is live.
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Bean
    public Summarizer summarizer(LlmProperties props) {
        Summarizer extractive = new ExtractiveSummarizer(props.maxSummaryChars());
        if (!props.active()) {
            log.info("LLM summariser DISABLED (no ANTHROPIC_API_KEY) — using extractive summaries.");
            return extractive;
        }
        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(props.apiKey())
                .timeout(Duration.ofMillis(props.timeoutMs()))
                .build();
        log.info("LLM summariser ENABLED (model={}).", props.model());
        return new AnthropicSummarizer(client, props, extractive);
    }
}
