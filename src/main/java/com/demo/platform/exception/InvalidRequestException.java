package com.demo.platform.exception;

/**
 * The request is well-formed but semantically invalid in a way bean validation
 * cannot express — e.g. a permission naming an entity or field that does not
 * exist. Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
