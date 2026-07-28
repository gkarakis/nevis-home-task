package com.nevis.search.search;

import com.nevis.search.client.ClientRepository;
import com.nevis.search.common.SearchNormalizer;
import com.nevis.search.common.exception.BadRequestException;
import com.nevis.search.config.SearchProperties;
import com.nevis.search.document.Document;
import com.nevis.search.document.DocumentRepository;
import com.nevis.search.embedding.EmbeddingService;
import com.nevis.search.llm.LlmProperties;
import com.nevis.search.llm.ParsedQuery;
import com.nevis.search.llm.QueryParser;
import com.nevis.search.search.dto.SearchHit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers the snippet-summary fallback and the Phase-2 query-parsing wiring: the parser
 * decides which text drives each channel and whether documents are scoped to a client.
 * DB behaviour is covered by the integration suite; here the repository is mocked to
 * assert what SearchService asks it for.
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock EmbeddingService embeddingService;
    @Mock SearchQueryRepository queryRepository;
    @Mock ClientRepository clientRepository;
    @Mock DocumentRepository documentRepository;
    @Mock QueryParser queryParser;

    private final SearchNormalizer normalizer = new SearchNormalizer();
    private final SearchProperties props = new SearchProperties(20, 100, 50, 60, 0.2, 0.3);
    private final LlmProperties llmProps = new LlmProperties(true, "k", null, 0, 0, 0, true);

    private SearchService service;

    @BeforeEach
    void setUp() {
        service = new SearchService(normalizer, embeddingService, queryRepository,
                clientRepository, documentRepository, props, llmProps, queryParser);
    }

    @Test
    void nullStoredSummaryFallsBackToLegacyContentSnippet() {
        Document doc = new Document(UUID.randomUUID(), "Title", " alpha   beta \n gamma ", null);

        assertThat(SearchService.summary(doc, 160)).isEqualTo("alpha beta gamma");
    }

    @Test
    void storedSummaryIsCappedBeforeReturningAsSnippet() {
        Document doc = new Document(UUID.randomUUID(), "Title", "content", "abcdefghijklmnop");

        assertThat(SearchService.summary(doc, 10)).isEqualTo("abcdefghij…");
    }

    @Test
    void compoundQueryScopesDocumentsToTheNamedClient() {
        UUID johnId = UUID.randomUUID();
        when(queryParser.parse("address proof of John Doe"))
                .thenReturn(ParsedQuery.compound("address proof", "John Doe"));
        when(embeddingService.embed("address proof")).thenReturn(new float[] {0.1f});
        when(queryRepository.searchClients(eq("johndoe"), anyDouble(), anyInt()))
                .thenReturn(List.of(new ClientCandidate(
                        johnId, "John", "Doe", "john@x.com", null, true, 0.9)));
        when(queryRepository.searchSemantic(anyString(), anyDouble(), anyInt(), any()))
                .thenReturn(List.of());
        when(queryRepository.searchLexical(anyString(), anyString(), anyInt(), any()))
                .thenReturn(List.of());
        when(clientRepository.findAllById(any())).thenReturn(List.of());
        when(documentRepository.findAllById(any())).thenReturn(List.of());

        service.search("address proof of John Doe", null);

        // Document channels search the concept, scoped to the directly-matched client.
        verify(queryRepository).searchSemantic(anyString(), anyDouble(), anyInt(), eq(List.of(johnId)));
        verify(queryRepository).searchLexical(eq("addressproof"), eq("address proof"),
                anyInt(), eq(List.of(johnId)));
    }

    @Test
    void plainQueryIsUnscopedAndUsesTheWholeQuery() {
        when(queryParser.parse("address proof")).thenReturn(ParsedQuery.plain("address proof"));
        when(embeddingService.embed("address proof")).thenReturn(new float[] {0.1f});
        when(queryRepository.searchClients(eq("addressproof"), anyDouble(), anyInt()))
                .thenReturn(List.of());
        when(queryRepository.searchSemantic(anyString(), anyDouble(), anyInt(), isNull()))
                .thenReturn(List.of());
        when(queryRepository.searchLexical(anyString(), anyString(), anyInt(), isNull()))
                .thenReturn(List.of());
        when(clientRepository.findAllById(any())).thenReturn(List.of());
        when(documentRepository.findAllById(any())).thenReturn(List.of());

        service.search("address proof", null);

        verify(queryRepository).searchSemantic(anyString(), anyDouble(), anyInt(), isNull());
        verify(queryRepository).searchLexical(eq("addressproof"), eq("address proof"),
                anyInt(), isNull());
    }

    @Test
    void invalidParsedClientFallsBackToLiteralUnscopedSearch() {
        when(queryParser.parse("address proof of John Doe"))
                .thenReturn(ParsedQuery.compound("address proof", "---"));
        when(embeddingService.embed("address proof of John Doe")).thenReturn(new float[] {0.1f});
        when(queryRepository.searchClients(eq("addressproofofjohndoe"), anyDouble(), anyInt()))
                .thenReturn(List.of());
        when(queryRepository.searchSemantic(anyString(), anyDouble(), anyInt(), isNull()))
                .thenReturn(List.of());
        when(queryRepository.searchLexical(anyString(), anyString(), anyInt(), isNull()))
                .thenReturn(List.of());
        when(clientRepository.findAllById(any())).thenReturn(List.of());
        when(documentRepository.findAllById(any())).thenReturn(List.of());

        service.search("address proof of John Doe", null);

        verify(queryRepository).searchSemantic(anyString(), anyDouble(), anyInt(), isNull());
        verify(queryRepository).searchLexical(eq("addressproofofjohndoe"),
                eq("address proof of John Doe"), anyInt(), isNull());
    }

    @Test
    void blankQueryIsRejectedBeforeAnyParsingOrModelCall() {
        assertThatThrownBy(() -> service.search("--", null))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(queryParser, embeddingService, queryRepository);
    }

    @Test
    void directClientHitsReportPerfectScore() {
        UUID johnId = UUID.randomUUID();
        when(queryParser.parse("John")).thenReturn(ParsedQuery.plain("John"));
        when(embeddingService.embed("John")).thenReturn(new float[] {0.1f});
        when(queryRepository.searchClients(eq("john"), anyDouble(), anyInt()))
                .thenReturn(List.of(new ClientCandidate(
                        johnId, "John", "Doe", "john@x.com", null, true, 0.15)));
        when(queryRepository.searchSemantic(anyString(), anyDouble(), anyInt(), isNull()))
                .thenReturn(List.of());
        when(queryRepository.searchLexical(anyString(), anyString(), anyInt(), isNull()))
                .thenReturn(List.of());
        when(clientRepository.findAllById(any())).thenReturn(List.of());
        when(documentRepository.findAllById(any())).thenReturn(List.of());

        List<SearchHit> hits = service.search("John", null);

        assertThat(hits).hasSize(1);
        assertThat(hits.getFirst().score()).isEqualTo(1.0);
    }
}
