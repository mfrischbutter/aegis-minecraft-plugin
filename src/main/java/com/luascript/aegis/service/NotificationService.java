package com.luascript.aegis.service;

import com.luascript.aegis.database.entity.Ban;
import com.luascript.aegis.database.entity.Kick;
import com.luascript.aegis.database.entity.Warn;

/**
 * Service interface for sending notifications about moderation actions.
 * Implementations may send notifications via Discord, email, or other channels.
 */
public interface NotificationService {

    /**
     * Send notification for a permanent ban.
     *
     * @param ban The ban that was created
     * @param warningCount The number of active warnings the player has
     */
    void notifyBan(Ban ban, int warningCount);

    /**
     * Send notification for a temporary ban.
     *
     * @param ban The temporary ban that was created
     * @param warningCount The number of active warnings the player has
     */
    void notifyTempBan(Ban ban, int warningCount);

    /**
     * Send notification for an unban.
     *
     * @param playerName The name of the player who was unbanned
     * @param playerUuid The UUID of the player who was unbanned
     * @param unbannedBy The name of the moderator who removed the ban
     * @param reason The reason for the unban
     */
    void notifyUnban(String playerName, String playerUuid, String unbannedBy, String reason);

    /**
     * Send notification for a warning.
     *
     * @param warn The warning that was created
     * @param warningCount The total number of active warnings the player has (including this one)
     */
    void notifyWarn(Warn warn, int warningCount);

    /**
     * Send notification for a warning removal.
     *
     * @param warnId The ID of the warning that was removed
     * @param playerName The name of the player whose warning was removed
     * @param playerUuid The UUID of the player whose warning was removed
     * @param removedBy The name of the moderator who removed the warning
     * @param reason The reason for the removal
     */
    void notifyUnwarn(Long warnId, String playerName, String playerUuid, String removedBy, String reason);

    /**
     * Send notification for clearing all warnings.
     *
     * @param playerName The name of the player whose warnings were cleared
     * @param playerUuid The UUID of the player whose warnings were cleared
     * @param clearedBy The name of the moderator who cleared the warnings
     * @param count The number of warnings that were cleared
     */
    void notifyClearWarns(String playerName, String playerUuid, String clearedBy, int count);

    /**
     * Send notification for a kick.
     *
     * @param kick The kick that was created
     * @param warningCount The number of active warnings the player has
     */
    void notifyKick(Kick kick, int warningCount);
}
