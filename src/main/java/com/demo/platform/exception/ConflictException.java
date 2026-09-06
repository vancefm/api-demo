package com.demo.platform.exception;

/**
 * The operation conflicts with the current state of the resource for a reason
 * other than a duplicate identifier — e.g. deleting a system role. Mapped to
 * HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
