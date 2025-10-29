package com.luascript.aegis.util;

import java.util.UUID;

/**
 * Constants used throughout the Aegis plugin.
 */
public final class Constants {

    /**
     * UUID representing the console as a command issuer.
     * This is used when moderation commands are executed from the server console.
     */
    public static final UUID CONSOLE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    /**
     * Display name for the console user.
     */
    public static final String CONSOLE_NAME = "Console";

    /**
     * Plugin messaging channel identifier for communication with backend servers.
     */
    public static final String PLUGIN_CHANNEL = "aegis:main";

    private Constants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
