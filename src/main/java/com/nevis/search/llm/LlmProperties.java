package com.nevis.search.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM configuration. The presence of an API key is the feature flag: with a key the
 * app summarises documents with a hosted model; without one it falls back to an
 * extractive summary and makes no network calls. The same build works both ways, so
 * a clean clone runs with no key and no external dependency.
 */
@ConfigurationProperties(prefix = "nevis.llm")
public record LlmProperties(
        boolean enabled,
        String apiKey,
        String model,
        long timeoutMs,
        int maxSummaryChars,
        boolean parseQueries
) {
    public LlmProperties {
        if (model == null || model.isBlank()) model = "claude-opus-4-8";
        if (timeoutMs <= 0) timeoutMs = 20_000;
        if (maxSummaryChars <= 0) maxSummaryChars = 160;
    }

    /** The LLM path is used only when explicitly enabled <em>and</em> a key is present. */
    public boolean active() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /**
     * Compound-query parsing runs on the latency-sensitive read path, so it has its own
     * switch on top of {@link #active()} — an LLM deployment can keep it off and pay the
     * per-search model call only for summaries.
     */
    public boolean queryParsingActive() {
        return active() && parseQueries;
    }
}
