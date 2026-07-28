package com.nevis.search.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateClientRequest(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @NotBlank @Email @Size(max = 320) String email,
        @Size(max = 5000) String description,
        List<@Pattern(regexp = "^https?://.*", message = "must be an http(s) URL") String> socialLinks
) {
}
