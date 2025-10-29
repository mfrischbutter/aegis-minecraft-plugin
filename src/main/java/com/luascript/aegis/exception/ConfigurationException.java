package com.luascript.aegis.exception;

/**
 * Exception thrown when configuration loading or validation fails.
 */
public class ConfigurationException extends AegisException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
