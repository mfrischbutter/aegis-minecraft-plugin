package com.luascript.aegis.exception;

/**
 * Exception thrown when a player cannot be found.
 */
public class PlayerNotFoundException extends AegisException {

    public PlayerNotFoundException(String player) {
        super("Player not found: " + player);
    }

    public PlayerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
