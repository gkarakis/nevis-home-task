package com.nevis.search.llm;

/**
 * Produces a short, human-readable summary of a document's content. One interface,
 * two implementations selected at startup by {@link LlmConfig}: an LLM-backed one
 * when an API key is present, and an extractive fallback otherwise.
 */
public interface Summarizer {

    /** @return a quick summary of the document; never {@code null}. */
    String summarize(String title, String content);
}
