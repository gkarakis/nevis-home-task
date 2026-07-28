package com.nevis.search.llm;

/**
 * Turns a raw search query into a {@link ParsedQuery}. One interface, two
 * implementations selected at startup by {@link LlmConfig}: an LLM-backed one that can
 * split compound queries, and a no-op that takes every query literally.
 */
public interface QueryParser {

    ParsedQuery parse(String rawQuery);
}
