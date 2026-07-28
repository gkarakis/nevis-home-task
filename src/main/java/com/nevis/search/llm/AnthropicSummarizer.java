package com.nevis.search.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Summarises with a hosted Claude model in a single message request. Because a
 * document summary is <em>optional</em>, any failure (network, rate limit, outage)
 * degrades to the extractive fallback rather than failing the document write — unlike
 * embeddings, whose failure returns 503. So three states all behave sanely: no key →
 * extractive; key but API unreachable → extractive; key and API up → LLM summary.
 */
public class AnthropicSummarizer implements Summarizer {

    private static final Logger log = LoggerFactory.getLogger(AnthropicSummarizer.class);
    private static final int MAX_PROMPT_CONTENT_CHARS = 12_000;

    private final AnthropicClient client;
    private final LlmProperties props;
    private final Summarizer fallback;

    public AnthropicSummarizer(AnthropicClient client, LlmProperties props, Summarizer fallback) {
        this.client = client;
        this.props = props;
        this.fallback = fallback;
    }

    @Override
    public String summarize(String title, String content) {
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(props.model())
                    .maxTokens(512L)
                    .addUserMessage(prompt(title, content))
                    .build();

            Message response = client.messages().create(params);
            String summary = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(text -> text.text())
                    .reduce("", String::concat)
                    .strip();

            return summary.isBlank()
                    ? fallback.summarize(title, content)
                    : SummaryText.clamp(summary, props.maxSummaryChars());
        } catch (RuntimeException e) {
            log.warn("LLM summary failed ({}); falling back to extractive summary", e.toString());
            return fallback.summarize(title, content);
        }
    }

    private static String prompt(String title, String content) {
        return "Summarise the following client document in one or two plain sentences. "
                + "Return only the summary, with no preamble or leading label.\n\n"
                + "Title: " + title + "\n\nContent:\n" + promptContent(content);
    }

    private static String promptContent(String content) {
        if (content == null) {
            return "";
        }
        String stripped = content.strip();
        if (stripped.length() <= MAX_PROMPT_CONTENT_CHARS) {
            return stripped;
        }
        return stripped.substring(0, MAX_PROMPT_CONTENT_CHARS).stripTrailing();
    }
}
