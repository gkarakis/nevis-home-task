package com.nevis.search.client.dto;

import com.nevis.search.client.Client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String description,
        List<String> socialLinks,
        Instant createdAt
) {
    public static ClientResponse from(Client c) {
        return new ClientResponse(
                c.getId(),
                c.getFirstName(),
                c.getLastName(),
                c.getEmail(),
                c.getDescription(),
                c.getSocialLinks() == null ? List.of() : List.of(c.getSocialLinks()),
                c.getCreatedAt());
    }
}
