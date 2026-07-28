package com.nevis.search.common.exception;

import org.springframework.http.HttpStatus;

/** Base for exceptions that map to a stable API error {@code code} and status. */
public abstract class ApiException extends RuntimeException {

    private final String code;

    protected ApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public abstract HttpStatus status();
}
