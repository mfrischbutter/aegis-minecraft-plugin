package com.luascript.aegis.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.luascript.aegis.config.CacheConfig;
import com.luascript.aegis.database.entity.Ban;
import com.luascript.aegis.database.entity.User;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for caching frequently accessed data.
 * Uses Caffeine for high-performance in-memory caching.
 */
@Singleton
public class CacheService {

    private final Logger logger;
    private final CacheConfig cacheConfig;

    private final Cache<UUID, Boolean> banStatusCache;
    private final Cache<UUID, User> userCache;
    private final Cache<String, UUID> usernameToUuidCache;

    @Inject
    public CacheService(Logger logger, CacheConfig cacheConfig) {
        this.logger = logger;
        this.cacheConfig = cacheConfig;

        if (cacheConfig.isEnabled()) {
            // Ban status cache - short TTL for accuracy
            this.banStatusCache = Caffeine.newBuilder()
                    .expireAfterWrite(cacheConfig.getBanCacheTtl(), TimeUnit.MINUTES)
                    .maximumSize(cacheConfig.getMaxCacheSize())
                    .recordStats()
                    .build();

            // User cache - longer TTL as user data changes less frequently
            this.userCache = Caffeine.newBuilder()
                    .expireAfterWrite(cacheConfig.getUserCacheTtl(), TimeUnit.MINUTES)
                    .maximumSize(cacheConfig.getMaxCacheSize())
                    .recordStats()
                    .build();

            // Username to UUID cache
            this.usernameToUuidCache = Caffeine.newBuilder()
                    .expireAfterWrite(cacheConfig.getUserCacheTtl(), TimeUnit.MINUTES)
                    .maximumSize(cacheConfig.getMaxCacheSize())
                    .recordStats()
                    .build();

            logger.info("Cache service initialized with TTL: Ban={}m, User={}m",
                    cacheConfig.getBanCacheTtl(),
                    cacheConfig.getUserCacheTtl());
        } else {
            this.banStatusCache = null;
            this.userCache = null;
            this.usernameToUuidCache = null;
            logger.info("Cache service disabled by configuration");
        }
    }

    // Ban Status Cache Methods

    /**
     * Get cached ban status for a player.
     *
     * @param uuid Player UUID
     * @return Optional containing ban status if cached
     */
    public Optional<Boolean> getCachedBanStatus(UUID uuid) {
        if (!cacheConfig.isEnabled() || banStatusCache == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(banStatusCache.getIfPresent(uuid));
    }

    /**
     * Cache ban status for a player.
     *
     * @param uuid Player UUID
     * @param isBanned Ban status
     */
    public void cacheBanStatus(UUID uuid, boolean isBanned) {
        if (cacheConfig.isEnabled() && banStatusCache != null) {
            banStatusCache.put(uuid, isBanned);
            logger.debug("Cached ban status for {}: {}", uuid, isBanned);
        }
    }

    /**
     * Invalidate ban status cache for a player.
     *
     * @param uuid Player UUID
     */
    public void invalidateBanCache(UUID uuid) {
        if (cacheConfig.isEnabled() && banStatusCache != null) {
            banStatusCache.invalidate(uuid);
            logger.debug("Invalidated ban cache for {}", uuid);
        }
    }

    // User Cache Methods

    /**
     * Get cached user data.
     *
     * @param uuid Player UUID
     * @return Optional containing user if cached
     */
    public Optional<User> getCachedUser(UUID uuid) {
        if (!cacheConfig.isEnabled() || userCache == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(userCache.getIfPresent(uuid));
    }

    /**
     * Cache user data.
     *
     * @param user User to cache
     */
    public void cacheUser(User user) {
        if (cacheConfig.isEnabled() && userCache != null && user != null) {
            userCache.put(user.getUuid(), user);
            usernameToUuidCache.put(user.getUsername().toLowerCase(), user.getUuid());
            logger.debug("Cached user: {}", user.getUsername());
        }
    }

    /**
     * Invalidate user cache.
     *
     * @param uuid Player UUID
     */
    public void invalidateUserCache(UUID uuid) {
        if (cacheConfig.isEnabled() && userCache != null) {
            User user = userCache.getIfPresent(uuid);
            if (user != null) {
                usernameToUuidCache.invalidate(user.getUsername().toLowerCase());
            }
            userCache.invalidate(uuid);
            logger.debug("Invalidated user cache for {}", uuid);
        }
    }

    /**
     * Get UUID from cached username.
     *
     * @param username Player username
     * @return Optional containing UUID if cached
     */
    public Optional<UUID> getCachedUuidFromUsername(String username) {
        if (!cacheConfig.isEnabled() || usernameToUuidCache == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usernameToUuidCache.getIfPresent(username.toLowerCase()));
    }

    /**
     * Clear all caches.
     */
    public void clearAll() {
        if (cacheConfig.isEnabled()) {
            if (banStatusCache != null) {
                banStatusCache.invalidateAll();
            }
            if (userCache != null) {
                userCache.invalidateAll();
            }
            if (usernameToUuidCache != null) {
                usernameToUuidCache.invalidateAll();
            }
            logger.info("All caches cleared");
        }
    }

    /**
     * Get cache statistics.
     *
     * @return Cache statistics as string
     */
    public String getStats() {
        if (!cacheConfig.isEnabled()) {
            return "Cache is disabled";
        }

        StringBuilder stats = new StringBuilder("Cache Statistics:\n");

        if (banStatusCache != null) {
            stats.append("Ban Cache: ").append(banStatusCache.stats()).append("\n");
        }

        if (userCache != null) {
            stats.append("User Cache: ").append(userCache.stats()).append("\n");
        }

        if (usernameToUuidCache != null) {
            stats.append("Username Cache: ").append(usernameToUuidCache.stats()).append("\n");
        }

        return stats.toString();
    }
}
