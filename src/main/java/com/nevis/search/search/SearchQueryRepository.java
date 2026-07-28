package com.nevis.search.search;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The three search queries, run with {@link NamedParameterJdbcTemplate}. Native SQL
 * is required because the queries use {@code <=>}, {@code similarity()} and
 * {@code ts_rank_cd}, none of which JPQL expresses. The {@code %…%} pattern is always
 * built from a bound parameter — the raw query is never concatenated into SQL.
 */
@Repository
public class SearchQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public SearchQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<ClientCandidate> CLIENT_MAPPER = (rs, i) -> new ClientCandidate(
            rs.getObject("id", UUID.class),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email"),
            rs.getString("description"),
            rs.getBoolean("direct_match"),
            rs.getDouble("score"));

    private static final RowMapper<DocCandidate> DOC_MAPPER = (rs, i) -> new DocCandidate(
            rs.getObject("id", UUID.class),
            rs.getObject("client_id", UUID.class),
            rs.getString("title"),
            rs.getDouble("score"));

    /** Query A — client candidates: direct substring matches and fuzzy trigram matches. */
    public List<ClientCandidate> searchClients(String qnorm, double clientFloor, int depth) {
        // direct_match is an identity signal, so it is computed over name + email
        // only — not search_blob, which also folds in description. A description
        // substring is a weak (fuzzy-tier) hit, not a tier-1 identity match. Retrieval
        // still uses the indexed search_blob, so description matches are found; they
        // just carry direct_match = false and sort below documents.
        //
        // Fuzzy retrieval uses the pg_trgm % operator, not similarity(...) >= x, so
        // PostgreSQL can use idx_clients_blob_trgm. set_config is statement-local here;
        // the threshold feeds the operator without leaking to later pooled-connection use.
        String sql = """
                WITH threshold AS (
                    SELECT set_config('pg_trgm.similarity_threshold', :clientFloorText, true)
                ),
                candidates AS (
                    SELECT c.id, c.first_name, c.last_name, c.email, c.description,
                           (search_normalize(c.first_name || ' ' || c.last_name || ' ' || c.email)
                                LIKE '%' || :qnorm || '%') AS direct_match,
                           similarity(c.search_blob, :qnorm) AS fuzzy_score
                    FROM clients c, threshold
                    WHERE c.search_blob LIKE '%' || :qnorm || '%'
                       OR c.search_blob % :qnorm
                )
                SELECT id, first_name, last_name, email, description,
                       direct_match,
                       fuzzy_score AS score
                FROM candidates
                ORDER BY direct_match DESC, score DESC, id
                LIMIT :depth
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("qnorm", qnorm)
                .addValue("clientFloorText", Double.toString(clientFloor))
                .addValue("depth", depth);
        return jdbc.query(sql, params, CLIENT_MAPPER);
    }

    /**
     * Query B1 — documents, semantic: a document's score is its best-scoring chunk.
     * When {@code clientIds} is non-empty the search is scoped to those clients — used
     * for compound "{concept} of {client}" queries; null/empty means all clients.
     */
    public List<DocCandidate> searchSemantic(String queryVector, double semanticFloor,
                                             int depth, Collection<UUID> clientIds) {
        String scope = scoped(clientIds) ? " WHERE d.client_id IN (:clientIds) " : "";
        String sql = """
                SELECT d.id, d.client_id, d.title,
                       MAX(1 - (c.embedding <=> CAST(:queryVector AS vector))) AS score
                FROM documents d
                JOIN document_chunks c ON c.document_id = d.id
                """ + scope + """
                GROUP BY d.id, d.client_id, d.title
                HAVING MAX(1 - (c.embedding <=> CAST(:queryVector AS vector))) >= :semanticFloor
                ORDER BY score DESC, d.id
                LIMIT :depth
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("queryVector", queryVector)
                .addValue("semanticFloor", semanticFloor)
                .addValue("depth", depth);
        if (scoped(clientIds)) {
            params.addValue("clientIds", clientIds);
        }
        return jdbc.query(sql, params, DOC_MAPPER);
    }

    /**
     * Query B2 — documents, lexical: full-text plus a digit-guarded identifier path.
     * Scoped to {@code clientIds} when non-empty, as {@link #searchSemantic}.
     */
    public List<DocCandidate> searchLexical(String qnorm, String rawQuery, int depth,
                                            Collection<UUID> clientIds) {
        String scope = scoped(clientIds) ? " AND client_id IN (:clientIds) " : "";
        // Identifier-like queries are intentionally absolute: any digit-bearing
        // substring hit sorts ahead of full-text rank because account/reference lookup
        // precision matters more than prose relevance for those searches.
        String sql = """
                SELECT id, client_id, title,
                       ts_rank_cd(search_tsv, websearch_to_tsquery('english', :q)) AS score
                FROM documents
                WHERE (search_tsv @@ websearch_to_tsquery('english', :q)
                       OR (:qnorm ~ '[0-9]' AND doc_blob LIKE '%' || :qnorm || '%'))
                """ + scope + """
                ORDER BY
                    (:qnorm ~ '[0-9]' AND doc_blob LIKE '%' || :qnorm || '%') DESC,
                    score DESC,
                    id
                LIMIT :depth
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("qnorm", qnorm)
                .addValue("q", rawQuery)
                .addValue("depth", depth);
        if (scoped(clientIds)) {
            params.addValue("clientIds", clientIds);
        }
        return jdbc.query(sql, params, DOC_MAPPER);
    }

    private static boolean scoped(Collection<UUID> clientIds) {
        return clientIds != null && !clientIds.isEmpty();
    }
}
