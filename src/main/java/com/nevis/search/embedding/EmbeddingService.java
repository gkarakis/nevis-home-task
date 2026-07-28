package com.nevis.search.embedding;

/**
 * One interface, one implementation, one test double. No provider-abstraction
 * framework: switching providers would change the vector dimension and require a
 * migration plus a full re-index, so it is a deployment concern, not a runtime one.
 */
public interface EmbeddingService {

    /**
     * @return a 384-dimension embedding for the given text.
     * @throws com.nevis.search.common.exception.EmbeddingUnavailableException
     *         if the model cannot produce a vector.
     */
    float[] embed(String text);
}
