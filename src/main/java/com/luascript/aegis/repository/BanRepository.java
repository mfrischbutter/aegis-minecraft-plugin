package com.luascript.aegis.repository;

import com.luascript.aegis.database.entity.Ban;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for Ban entity operations.
 */
public interface BanRepository extends Repository<Ban, Long> {

    /**
     * Find the active ban for a player by UUID.
     *
     * @param uuid Player UUID
     * @return CompletableFuture with Optional containing active ban if found
     */
    CompletableFuture<Optional<Ban>> findActiveBanByUuid(UUID uuid);

    /**
     * Find the active ban for an IP address.
     *
     * @param ipAddress IP address
     * @return CompletableFuture with Optional containing active ban if found
     */
    CompletableFuture<Optional<Ban>> findActiveBanByIp(String ipAddress);

    /**
     * Find all bans for a player (including expired and removed bans).
     *
     * @param uuid Player UUID
     * @return CompletableFuture with list of bans
     */
    CompletableFuture<List<Ban>> findBanHistory(UUID uuid);

    /**
     * Find all currently active bans.
     *
     * @return CompletableFuture with list of active bans
     */
    CompletableFuture<List<Ban>> findAllActiveBans();

    /**
     * Find all expired bans that are still marked as active.
     *
     * @return CompletableFuture with list of expired bans
     */
    CompletableFuture<List<Ban>> findExpiredBans();

    /**
     * Check if a player is currently banned.
     *
     * @param uuid Player UUID
     * @return CompletableFuture with boolean indicating ban status
     */
    CompletableFuture<Boolean> isPlayerBanned(UUID uuid);

    /**
     * Find all active bans with pagination.
     *
     * @param page Page number (0-indexed)
     * @param pageSize Number of results per page
     * @return CompletableFuture with list of bans
     */
    CompletableFuture<List<Ban>> findActiveBansPaginated(int page, int pageSize);
}
