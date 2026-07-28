package com.nevis.search.document;

import com.nevis.search.client.Client;
import com.nevis.search.document.dto.CreateDocumentRequest;
import com.nevis.search.document.dto.DocumentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Persists a document and all its chunks in one transaction, so they commit
 * atomically. Kept as a separate bean (not a self-invoked method on
 * {@code DocumentService}) because Spring's proxy would not apply
 * {@code @Transactional} to an internal call.
 *
 * <p>The invariant <em>document exists ⇒ its chunks are embedded</em> always holds:
 * embedding is done by the caller before this method opens the transaction, and if
 * it failed nothing reaches here.
 */
@Component
@RequiredArgsConstructor
public class DocumentWriter {

    private final DocumentRepository documentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public DocumentResponse save(Client client, CreateDocumentRequest req,
                                 List<EmbeddedChunk> chunks) {
        Document document = documentRepository.saveAndFlush(
                new Document(client.getId(), req.title().strip(), req.content()));

        UUID documentId = document.getId();
        // The embedding is passed as a pgvector literal and cast in SQL; Hibernate
        // does not model the vector type, so chunks are written with JdbcTemplate.
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO document_chunks (id, document_id, chunk_index, content, embedding)
                VALUES (?, ?, ?, ?, CAST(? AS vector))
                """,
                chunks,
                chunks.size(),
                (ps, chunk) -> {
                    ps.setObject(1, UUID.randomUUID());
                    ps.setObject(2, documentId);
                    ps.setInt(3, chunk.index());
                    ps.setString(4, chunk.text());
                    ps.setString(5, VectorLiterals.toLiteral(chunk.embedding()));
                });

        return DocumentResponse.from(document, chunks.size());
    }
}
