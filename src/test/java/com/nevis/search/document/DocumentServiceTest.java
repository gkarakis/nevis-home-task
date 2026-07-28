package com.nevis.search.document;

import com.nevis.search.client.Client;
import com.nevis.search.client.ClientRepository;
import com.nevis.search.common.exception.EmbeddingUnavailableException;
import com.nevis.search.common.exception.NotFoundException;
import com.nevis.search.document.dto.CreateDocumentRequest;
import com.nevis.search.embedding.EmbeddingService;
import com.nevis.search.embedding.TextChunker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock ClientRepository clientRepository;
    @Spy TextChunker textChunker = new TextChunker();
    @Mock EmbeddingService embeddingService;
    @Mock DocumentWriter documentWriter;

    @InjectMocks DocumentService documentService;

    private final CreateDocumentRequest request =
            new CreateDocumentRequest("Title", "Some document content to embed.");

    @Test
    void unknownClientThrowsBeforeAnyEmbeddingCall() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.create(clientId, request))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(embeddingService, documentWriter);
    }

    @Test
    void embeddingFailurePersistsNothing() {
        UUID clientId = UUID.randomUUID();
        Client client = new Client("John", "Doe", "john@example.com", null, new String[0]);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(embeddingService.embed(anyString()))
                .thenThrow(new EmbeddingUnavailableException("model down", new RuntimeException()));

        assertThatThrownBy(() -> documentService.create(clientId, request))
                .isInstanceOf(EmbeddingUnavailableException.class);

        verify(documentWriter, never()).save(any(), any(), any());
    }
}
