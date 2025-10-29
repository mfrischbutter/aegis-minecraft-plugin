package com.luascript.aegis.service;

import com.luascript.aegis.database.entity.Warn;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for warning management operations.
 */
public interface WarnService {

    /**
     * Create a warning for a player.
     * This will trigger escalation if thresholds are met.
     *
     * @param playerUuid UUID of player to warn
     * @param reason Warning reason
     * @param issuerUuid UUID of staff member issuing the warning
     * @param serverName Server where warning was issued
     * @param duration Optional duration for temporary warning (null for permanent)
     * @return CompletableFuture with created Warn
     */
    CompletableFuture<Warn> createWarn(UUID playerUuid, String reason, UUID issuerUuid,
                                        String serverName, Duration duration);

    /**
     * Remove a specific warning.
     *
     * @param warnId ID of warning to remove
     * @param removedBy UUID of staff member removing the warning
     * @param reason Reason for removal
     * @return CompletableFuture with boolean indicating if warning was removed
     */
    CompletableFuture<Boolean> removeWarn(Long warnId, UUID removedBy, String reason);

    /**
     * Clear all active warnings for a player.
     *
     * @param playerUuid Player UUID
     * @param removedBy UUID of staff member clearing warnings
     * @param reason Reason for clearing
     * @return CompletableFuture completing when warnings are cleared
     */
    CompletableFuture<Void> clearWarns(UUID playerUuid, UUID removedBy, String reason);

    /**
     * Get all active warnings for a player.
     *
     * @param playerUuid Player UUID
     * @return CompletableFuture with list of active warnings
     */
    CompletableFuture<List<Warn>> getActiveWarns(UUID playerUuid);

    /**
     * Count active warnings for a player.
     *
     * @param playerUuid Player UUID
     * @return CompletableFuture with count
     */
    CompletableFuture<Long> countActiveWarns(UUID playerUuid);

    /**
     * Get warning history for a player.
     *
     * @param playerUuid Player UUID
     * @return CompletableFuture with list of all warnings
     */
    CompletableFuture<List<Warn>> getWarnHistory(UUID playerUuid);

    /**
     * Process expired warnings (deactivate them).
     * Should be called periodically by a scheduler.
     *
     * @return CompletableFuture with number of warnings processed
     */
    CompletableFuture<Integer> processExpiredWarns();
}
