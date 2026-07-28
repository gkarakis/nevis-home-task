package com.nevis.search.llm;

/**
 * A search query after optional LLM parsing.
 *
 * <p>{@code documentQuery} is the topic/identifier to search documents for;
 * {@code clientQuery} is the text used to find the client(s). For a plain query both
 * equal the original text and {@code scopedToClient} is false, so search behaves exactly
 * as it does without the LLM. For a compound query like "address proof of John Doe" the
 * two are split ("address proof" / "John Doe") and documents are scoped to the named
 * client — the fix for the limitation in README §7.
 */
public record ParsedQuery(String documentQuery, String clientQuery, boolean scopedToClient) {

    /** No compound structure: the whole query drives both channels, with no client scope. */
    public static ParsedQuery plain(String query) {
        String q = query == null ? "" : query;
        return new ParsedQuery(q, q, false);
    }

    /** A compound "{concept} of {client}" query, scoping documents to the named client. */
    public static ParsedQuery compound(String concept, String clientName) {
        return new ParsedQuery(concept, clientName, true);
    }
}
