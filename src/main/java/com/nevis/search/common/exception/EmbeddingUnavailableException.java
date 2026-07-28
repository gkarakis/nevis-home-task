package com.nevis.search.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when the embedding model cannot produce a vector. Because embedding
 * happens before the write transaction, a failure means nothing is persisted —
 * the invariant "document exists ⇒ its chunks are embedded" is preserved.
 */
public class EmbeddingUnavailableException extends ApiException {

    public EmbeddingUnavailableException(String message, Throwable cause) {
        super("EMBEDDING_UNAVAILABLE", message);
        initCause(cause);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }
}
