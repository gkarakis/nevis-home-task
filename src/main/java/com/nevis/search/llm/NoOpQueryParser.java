package com.nevis.search.llm;

/**
 * The no-LLM parser: every query is taken literally, with no client scoping. This is
 * the default, and it makes search byte-for-byte identical to the pre-LLM behaviour.
 */
public class NoOpQueryParser implements QueryParser {

    @Override
    public ParsedQuery parse(String rawQuery) {
        return ParsedQuery.plain(rawQuery);
    }
}
