package com.luascript.aegis.service;

import com.luascript.aegis.database.entity.Ban;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for ban management operations.
 */
public interface BanService {

    /**
     * Create a permanent ban.
     *
     * @param playerUuid UUID of player to ban
     * @param reason Ban reason
     * @param issuerUuid UUID of staff member issuing the ban
     * @param serverName Server where ban was issued
     * @param ipAddress Optional IP address to ban (can be null)
     * @return CompletableFuture with created Ban
     */
    CompletableFuture<Ban> createPermanentBan(UUID playerUuid, String reason, UUID issuerUuid,
                                                String serverName, String ipAddress);

    /**
     * Create a temporary ban.
     *
     * @param playerUuid UUID of player to ban
     * @param reason Ban reason
     * @param issuerUuid UUID of staff member issuing the ban
     * @param duration Duration of the ban
     * @param serverName Server where ban was issued
     * @param ipAddress Optional IP address to ban (can be null)
     * @return CompletableFuture with created Ban
     */
    CompletableFuture<Ban> createTemporaryBan(UUID playerUuid, String reason, UUID issuerUuid,
                                               Duration duration, String serverName, String ipAddress);

    /**
     * Remove (unban) an active ban.
     *
     * @param playerUuid UUID of player to unban
     * @param removedBy UUID of staff member removing the ban
     * @param reason Reason for unbanning
     * @return CompletableFuture with boolean indicating if a ban was removed
     */
    CompletableFuture<Boolean> removeBan(UUID playerUuid, UUID removedBy, String reason);

    /**
     * Check if a player is currently banned.
     *
     * @param playerUuid Player UUID
     * @return CompletableFuture with boolean indicating ban status
     */
    CompletableFuture<Boolean> isBanned(UUID playerUuid);

    /**
     * Check if an IP address is currently banned.
     *
     * @param ipAddress IP address
     * @return CompletableFuture with boolean indicating ban status
     */
    CompletableFuture<Boolean> isIpBanned(String ipAddress);

    /**
     * Get an active ban by IP address.
     *
     * @param ipAddress IP address
     * @return CompletableFuture with Optional containing active ban if found
     */
    CompletableFuture<Optional<Ban>> getBanByIpAddress(String ipAddress);

    /**
     * Get the active ban for a player.
     *
     * @param playerUuid Player UUID
     * @return CompletableFuture with Optional containing active ban if found
     */
    CompletableFuture<Optional<Ban>> getActiveBan(UUID playerUuid);

    /**
     * Get ban history for a player.
     *
     * @param playerUuid Player UUID
     * @return CompletableFuture with list of all bans
     */
    CompletableFuture<List<Ban>> getBanHistory(UUID playerUuid);

    /**
     * Get all active bans with pagination.
     *
     * @param page Page number (0-indexed)
     * @param pageSize Number of results per page
     * @return CompletableFuture with list of active bans
     */
    CompletableFuture<List<Ban>> getActiveBans(int page, int pageSize);

    /**
     * Process expired bans (deactivate them).
     * Should be called periodically by a scheduler.
     *
     * @return CompletableFuture with number of bans processed
     */
    CompletableFuture<Integer> processExpiredBans();
}
