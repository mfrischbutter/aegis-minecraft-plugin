package com.luascript.aegis.exception;

/**
 * Exception thrown when input validation fails.
 */
public class ValidationException extends AegisException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
