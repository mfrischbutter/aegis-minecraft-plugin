package com.luascript.aegis.repository;

import com.luascript.aegis.database.entity.Warn;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for Warn entity operations.
 */
public interface WarnRepository extends Repository<Warn, Long> {

    /**
     * Find all active warnings for a player.
     *
     * @param uuid Player UUID
     * @return CompletableFuture with list of active warnings
     */
    CompletableFuture<List<Warn>> findActiveWarnsByPlayer(UUID uuid);

    /**
     * Find active warnings for a player with pagination.
     *
     * @param uuid Player UUID
     * @param page Page number (0-indexed)
     * @param pageSize Number of results per page
     * @return CompletableFuture with list of active warnings
     */
    CompletableFuture<List<Warn>> findActiveWarnsByPlayerPaginated(UUID uuid, int page, int pageSize);

    /**
     * Count active warnings for a player.
     *
     * @param uuid Player UUID
     * @return CompletableFuture with count of active warnings
     */
    CompletableFuture<Long> countActiveWarns(UUID uuid);

    /**
     * Find all warnings for a player (including expired and removed).
     *
     * @param uuid Player UUID
     * @return CompletableFuture with list of all warnings
     */
    CompletableFuture<List<Warn>> findWarnHistory(UUID uuid);

    /**
     * Find all expired warnings that are still marked as active.
     *
     * @return CompletableFuture with list of expired warnings
     */
    CompletableFuture<List<Warn>> findExpiredWarns();

    /**
     * Deactivate all active warnings for a player.
     *
     * @param uuid Player UUID
     * @param removedBy UUID of the staff member removing warnings
     * @param reason Reason for removal
     * @return CompletableFuture completing when update is done
     */
    CompletableFuture<Void> clearWarns(UUID uuid, UUID removedBy, String reason);

    /**
     * Find a warning by ID with player and issuer eagerly loaded.
     * This is useful for operations that need to access player data after the session closes.
     *
     * @param id Warning ID
     * @return CompletableFuture with Optional containing the warn with associations loaded
     */
    CompletableFuture<java.util.Optional<Warn>> findByIdWithAssociations(Long id);
}
