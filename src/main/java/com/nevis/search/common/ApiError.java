package com.nevis.search.common;

import java.time.Instant;
import java.util.List;

/** One error shape everywhere. {@code details} carries field violations for 400s. */
public record ApiError(
        String code,
        String message,
        Instant timestamp,
        List<FieldViolation> details
) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Instant.now(), List.of());
    }

    public static ApiError of(String code, String message, List<FieldViolation> details) {
        return new ApiError(code, message, Instant.now(), details);
    }

    public record FieldViolation(String field, String message) {}
}
