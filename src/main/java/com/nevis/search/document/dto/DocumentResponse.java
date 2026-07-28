package com.nevis.search.document.dto;

import com.nevis.search.document.Document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID clientId,
        String title,
        int chunkCount,
        Instant createdAt
) {
    public static DocumentResponse from(Document d, int chunkCount) {
        return new DocumentResponse(
                d.getId(), d.getClientId(), d.getTitle(), chunkCount, d.getCreatedAt());
    }
}
