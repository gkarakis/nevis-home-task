package com.nevis.search.llm;

/**
 * The no-LLM summary: a whitespace-collapsed leading slice of the content. Used both
 * as the fallback when no API key is configured and as the safety net when a live LLM
 * call fails (see {@link AnthropicSummarizer}). Deterministic and dependency-free.
 */
public class ExtractiveSummarizer implements Summarizer {

    private final int maxChars;

    public ExtractiveSummarizer(int maxChars) {
        this.maxChars = maxChars;
    }

    @Override
    public String summarize(String title, String content) {
        return SummaryText.fromContent(content, maxChars);
    }
}
