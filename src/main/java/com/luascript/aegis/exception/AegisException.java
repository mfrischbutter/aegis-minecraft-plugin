package com.luascript.aegis.exception;

/**
 * Base exception for all Aegis-related errors.
 * Extends RuntimeException to avoid forcing exception handling in async contexts.
 */
public class AegisException extends RuntimeException {

    public AegisException(String message) {
        super(message);
    }

    public AegisException(String message, Throwable cause) {
        super(message, cause);
    }

    public AegisException(Throwable cause) {
        super(cause);
    }
}
