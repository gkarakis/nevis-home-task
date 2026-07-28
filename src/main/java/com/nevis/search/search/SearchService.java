package com.nevis.search.search;

import com.nevis.search.client.Client;
import com.nevis.search.client.ClientRepository;
import com.nevis.search.common.SearchNormalizer;
import com.nevis.search.common.exception.BadRequestException;
import com.nevis.search.config.SearchProperties;
import com.nevis.search.document.Document;
import com.nevis.search.document.DocumentRepository;
import com.nevis.search.document.VectorLiterals;
import com.nevis.search.embedding.EmbeddingService;
import com.nevis.search.llm.LlmProperties;
import com.nevis.search.llm.SummaryText;
import com.nevis.search.search.dto.Channel;
import com.nevis.search.search.dto.SearchHit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    /** Shortest normalised query we treat as searchable; below this we reject the request. */
    private static final int MIN_SEARCHABLE_QUERY_LENGTH = 2;

    private final SearchNormalizer searchNormalizer;
    private final EmbeddingService embeddingService;
    private final SearchQueryRepository queryRepository;
    private final ClientRepository clientRepository;
    private final DocumentRepository documentRepository;
    private final SearchProperties props;
    private final LlmProperties llmProps;

    @Transactional(readOnly = true)
    public List<SearchHit> search(String rawQuery, Integer requestedLimit) {
        // Validate after normalising. A query of "---" passes @NotBlank but
        // normalises to empty, at which point search_blob LIKE '%%' matches every
        // client. Guarding here also skips a pointless model call for junk queries.
        String qnorm = searchNormalizer.normalize(rawQuery);
        if (qnorm.length() < MIN_SEARCHABLE_QUERY_LENGTH) {
            throw new BadRequestException("INVALID_QUERY",
                    "Query contains no searchable characters");
        }

        int limit = props.clampLimit(requestedLimit);
        int depth = props.channelDepth();

        String queryVector = VectorLiterals.toLiteral(embeddingService.embed(rawQuery));

        List<ClientCandidate> clients =
                queryRepository.searchClients(qnorm, props.clientFloor(), depth);
        List<DocCandidate> semantic =
                queryRepository.searchSemantic(queryVector, props.semanticFloor(), depth);
        List<DocCandidate> lexical =
                queryRepository.searchLexical(qnorm, rawQuery, depth);

        List<FusedDocument> fused = DocumentRankFusion.fuse(semantic, lexical, props.rrfK());

        return assemble(clients, fused, limit);
    }

    private List<SearchHit> assemble(List<ClientCandidate> clients,
                                     List<FusedDocument> fused,
                                     int limit) {
        // Hydrate the owning clients and document bodies for the documents we kept.
        List<UUID> ownerIds = fused.stream().map(FusedDocument::clientId).distinct().toList();
        Map<UUID, Client> ownerById = clientRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(Client::getId, Function.identity()));

        List<UUID> docIds = fused.stream().map(FusedDocument::documentId).toList();
        Map<UUID, Document> docById = documentRepository.findAllById(docIds).stream()
                .collect(Collectors.toMap(Document::getId, Function.identity()));

        List<SearchHit> directClients = new ArrayList<>();
        List<SearchHit> fuzzyClients = new ArrayList<>();
        for (ClientCandidate c : clients) {
            SearchHit hit = SearchHit.client(
                    c.id(),
                    round(c.score()),
                    List.of(c.directMatch() ? Channel.DIRECT : Channel.FUZZY),
                    new SearchHit.ClientRef(c.id(), c.firstName(), c.lastName(),
                            c.email(), c.description()));
            (c.directMatch() ? directClients : fuzzyClients).add(hit);
        }

        List<SearchHit> documents = new ArrayList<>(fused.size());
        for (FusedDocument f : fused) {
            Document doc = docById.get(f.documentId());
            if (doc == null) {
                continue; // Row vanished between query and hydration; skip defensively.
            }
            Client owner = ownerById.get(f.clientId());
            SearchHit.ClientRef ownerRef = owner == null ? null
                    : new SearchHit.ClientRef(owner.getId(), owner.getFirstName(),
                            owner.getLastName(), null, null);
            SearchHit.DocumentRef docRef = new SearchHit.DocumentRef(
                    doc.getId(), doc.getTitle(), summary(doc, llmProps.maxSummaryChars()),
                    doc.getCreatedAt());
            documents.add(SearchHit.document(f.documentId(), round(f.displayScore()),
                    f.matchedBy(), ownerRef, docRef));
        }

        return ResultOrdering.order(directClients, documents, fuzzyClients, limit);
    }

    /**
     * Prefer the stored write-time summary. Rows created before the summary
     * column existed may still be null, so preserve the previous snippet behavior.
     */
    static String summary(Document doc, int maxSummaryChars) {
        return doc.getSummary() == null
                ? SummaryText.fromContent(doc.getContent(), maxSummaryChars)
                : SummaryText.clamp(doc.getSummary(), maxSummaryChars);
    }

    private static double round(double v) {
        return Math.round(v * 10_000.0) / 10_000.0;
    }
}
