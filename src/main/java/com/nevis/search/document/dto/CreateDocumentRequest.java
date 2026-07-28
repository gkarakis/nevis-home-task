package com.nevis.search.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDocumentRequest(
        @NotBlank @Size(max = 500) String title,
        @NotBlank @Size(max = 50_000) String content
) {
}
