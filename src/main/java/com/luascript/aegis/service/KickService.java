package com.luascript.aegis.service;

import com.luascript.aegis.database.entity.Kick;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for kick operations.
 */
public interface KickService {

    /**
     * Create a kick record.
     *
     * @param playerUuid UUID of player to kick
     * @param reason Kick reason
     * @param issuerUuid UUID of staff member issuing the kick
     * @param serverName Server where kick was issued
     * @return CompletableFuture with created Kick
     */
    CompletableFuture<Kick> createKick(UUID playerUuid, String reason, UUID issuerUuid, String serverName);

    /**
     * Get kick history for a player.
     *
     * @param playerUuid Player UUID
     * @return CompletableFuture with list of kicks
     */
    CompletableFuture<List<Kick>> getKickHistory(UUID playerUuid);

    /**
     * Get recent kicks for a player.
     *
     * @param playerUuid Player UUID
     * @param days Number of days to look back
     * @return CompletableFuture with list of recent kicks
     */
    CompletableFuture<List<Kick>> getRecentKicks(UUID playerUuid, int days);

    /**
     * Count kicks for a player within a time period.
     *
     * @param playerUuid Player UUID
     * @param days Number of days to look back
     * @return CompletableFuture with count
     */
    CompletableFuture<Long> countRecentKicks(UUID playerUuid, int days);
}
