package com.nevis.search.embedding;

import com.nevis.search.common.exception.EmbeddingUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.stereotype.Service;

/** Wraps the Spring AI transformer model, translating failures to a 503. */
@Service
@RequiredArgsConstructor
public class LocalEmbeddingService implements EmbeddingService {

    public static final int DIMENSIONS = 384;

    private final TransformersEmbeddingModel model;

    @Override
    public float[] embed(String text) {
        try {
            float[] vector = model.embed(text);
            if (vector == null || vector.length != DIMENSIONS) {
                throw new IllegalStateException(
                        "Embedding model returned an unexpected dimension: "
                                + (vector == null ? "null" : vector.length));
            }
            return vector;
        } catch (Exception e) {
            throw new EmbeddingUnavailableException(
                    "Failed to compute embedding: " + e.getMessage(), e);
        }
    }
}
