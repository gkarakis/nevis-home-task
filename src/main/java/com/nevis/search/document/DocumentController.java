package com.nevis.search.document;

import com.nevis.search.document.dto.CreateDocumentRequest;
import com.nevis.search.document.dto.DocumentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/clients/{clientId}/documents")
@Tag(name = "Documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    @Operation(summary = "Create a document for a client",
            description = "201 with Location header; 400 on validation; "
                    + "404 unknown client; 503 embedding unavailable")
    public ResponseEntity<DocumentResponse> create(@PathVariable UUID clientId,
                                                    @Valid @RequestBody CreateDocumentRequest req,
                                                    UriComponentsBuilder uriBuilder) {
        DocumentResponse created = documentService.create(clientId, req);
        URI location = uriBuilder.path("/clients/{clientId}/documents/{id}")
                .buildAndExpand(clientId, created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }
}
