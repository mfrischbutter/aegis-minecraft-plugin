package com.luascript.aegis.repository;

import com.luascript.aegis.database.entity.Kick;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for Kick entity operations.
 */
public interface KickRepository extends Repository<Kick, Long> {

    /**
     * Find all kicks for a player.
     *
     * @param uuid Player UUID
     * @return CompletableFuture with list of kicks
     */
    CompletableFuture<List<Kick>> findKickHistory(UUID uuid);

    /**
     * Find recent kicks for a player (within last N days).
     *
     * @param uuid Player UUID
     * @param days Number of days to look back
     * @return CompletableFuture with list of recent kicks
     */
    CompletableFuture<List<Kick>> findRecentKicks(UUID uuid, int days);

    /**
     * Count kicks for a player within a time period.
     *
     * @param uuid Player UUID
     * @param days Number of days to look back
     * @return CompletableFuture with count of kicks
     */
    CompletableFuture<Long> countRecentKicks(UUID uuid, int days);
}
