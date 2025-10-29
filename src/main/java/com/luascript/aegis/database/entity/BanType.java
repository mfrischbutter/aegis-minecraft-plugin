package com.luascript.aegis.database.entity;

/**
 * Enum representing the type of ban.
 */
public enum BanType {
    /**
     * Permanent ban (no expiration).
     */
    PERMANENT,

    /**
     * Temporary ban (expires after a set duration).
     */
    TEMPORARY
}
