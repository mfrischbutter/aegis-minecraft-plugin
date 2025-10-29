package com.luascript.aegis.config;

/**
 * Configuration holder for cache settings.
 */
public class CacheConfig {
    private final boolean enabled;
    private final int banCacheTtl; // in minutes
    private final int userCacheTtl; // in minutes
    private final int maxCacheSize;

    public CacheConfig(boolean enabled, int banCacheTtl, int userCacheTtl, int maxCacheSize) {
        this.enabled = enabled;
        this.banCacheTtl = banCacheTtl;
        this.userCacheTtl = userCacheTtl;
        this.maxCacheSize = maxCacheSize;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getBanCacheTtl() {
        return banCacheTtl;
    }

    public int getUserCacheTtl() {
        return userCacheTtl;
    }

    public int getMaxCacheSize() {
        return maxCacheSize;
    }
}
