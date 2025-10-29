package com.luascript.aegis.exception;

/**
 * Exception thrown when database operations fail.
 */
public class DatabaseException extends AegisException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseException(Throwable cause) {
        super(cause);
    }
}
