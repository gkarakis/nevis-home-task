package com.nevis.search.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Splits a compound query into a document topic and an optional client name with a
 * hosted model, so "address proof of John Doe" filters documents to John Doe instead of
 * surfacing address-proof documents for every client (README §7).
 *
 * <p>This runs on the read path, so it fails safe: a short query, a malformed response,
 * or an LLM outage all fall back to {@link ParsedQuery#plain} — the query is searched
 * literally and document search is never narrowed on a guess.
 */
public class AnthropicQueryParser implements QueryParser {

    private static final Logger log = LoggerFactory.getLogger(AnthropicQueryParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** A "{concept} of {client}" split needs at least this many whitespace tokens. */
    private static final int MIN_TOKENS_FOR_COMPOUND = 3;

    private final AnthropicClient client;
    private final LlmProperties props;

    public AnthropicQueryParser(AnthropicClient client, LlmProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public ParsedQuery parse(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return ParsedQuery.plain(rawQuery);
        }
        // A one- or two-token query ("NevisWealth", "John Doe", "ACC 889134") cannot
        // carry compound structure — skip the model call and the latency it adds.
        if (rawQuery.strip().split("\\s+").length < MIN_TOKENS_FOR_COMPOUND) {
            return ParsedQuery.plain(rawQuery);
        }
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(props.model())
                    .maxTokens(256L)
                    .addUserMessage(prompt(rawQuery.strip()))
                    .build();
            return fromJson(rawQuery, text(client.messages().create(params)));
        } catch (Exception e) {
            log.warn("LLM query parse failed ({}); using the query as-is", e.toString());
            return ParsedQuery.plain(rawQuery);
        }
    }

    /**
     * Interpret the model's JSON. Package-private and static so it is unit-tested
     * without a live client. Any missing/blank field — or no named client — yields a
     * plain query, so document search is only ever scoped on a confident parse.
     */
    static ParsedQuery fromJson(String rawQuery, String response) {
        try {
            String json = extractObject(response);
            if (json == null) {
                return ParsedQuery.plain(rawQuery);
            }
            JsonNode node = MAPPER.readTree(json);
            String concept = node.path("concept").asText("").strip();
            String clientName = node.path("client_name").asText("").strip();
            if (concept.isBlank() || clientName.isBlank()) {
                return ParsedQuery.plain(rawQuery);
            }
            return ParsedQuery.compound(concept, clientName);
        } catch (Exception e) {
            return ParsedQuery.plain(rawQuery);
        }
    }

    /** Tolerate a model that wraps the JSON in prose or ```json fences. */
    private static String extractObject(String s) {
        if (s == null) {
            return null;
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        return (start >= 0 && end > start) ? s.substring(start, end + 1) : null;
    }

    private static String text(Message response) {
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(t -> t.text())
                .reduce("", String::concat)
                .strip();
    }

    private static String prompt(String query) {
        return """
                Split a financial advisor's document search query into a topic and an \
                optional client name. Return ONLY a compact JSON object with keys \
                "concept" and "client_name", and no other text.
                - "concept": what to look for in documents (a topic or identifier), never a person's name.
                - "client_name": the specific person or account the query is about, or "" if none is named.
                Set "client_name" only when the query is clearly about one client's documents \
                (e.g. "X of <name>", "<name>'s X"). If the query is just a topic, or just a bare \
                name, set "client_name" to "".

                Query: %s""".formatted(query);
    }
}
