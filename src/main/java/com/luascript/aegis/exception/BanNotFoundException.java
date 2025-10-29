package com.luascript.aegis.exception;

/**
 * Exception thrown when a ban cannot be found.
 */
public class BanNotFoundException extends AegisException {

    public BanNotFoundException(String message) {
        super(message);
    }

    public BanNotFoundException(Long banId) {
        super("Ban not found with ID: " + banId);
    }
}
