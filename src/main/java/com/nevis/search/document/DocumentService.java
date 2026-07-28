package com.nevis.search.document;

import com.nevis.search.client.Client;
import com.nevis.search.client.ClientRepository;
import com.nevis.search.common.exception.NotFoundException;
import com.nevis.search.document.dto.CreateDocumentRequest;
import com.nevis.search.document.dto.DocumentResponse;
import com.nevis.search.embedding.EmbeddingService;
import com.nevis.search.embedding.TextChunker;
import com.nevis.search.llm.Summarizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final ClientRepository clientRepository;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;
    private final Summarizer summarizer;
    private final DocumentWriter documentWriter;

    public DocumentResponse create(UUID clientId, CreateDocumentRequest req) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND",
                        "Client " + clientId + " does not exist"));

        // Chunk and embed BEFORE opening a transaction. Holding a pooled DB
        // connection across model inference exhausts the pool under load. Each
        // chunk's embedding text is prefixed with the title so title semantics
        // stay visible in every chunk.
        List<TextChunker.Chunk> chunks = textChunker.chunk(req.content());
        List<EmbeddedChunk> embedded = chunks.stream()
                .map(c -> new EmbeddedChunk(
                        c.index(),
                        c.text(),
                        embeddingService.embed(req.title() + "\n" + c.text())))
                .toList();

        // Summary is generated after embedding (so an embedding 503 skips the LLM call)
        // and before the write opens, keeping model calls off a held DB connection. It
        // never fails the write: the summariser falls back to an extractive summary.
        String summary = summarizer.summarize(req.title(), req.content());

        // Separate bean, not a self-invoked method, so @Transactional applies.
        return documentWriter.save(client, req, embedded, summary);
    }
}
