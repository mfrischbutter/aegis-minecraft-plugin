package com.luascript.aegis.service;

import com.velocitypowered.api.proxy.Player;

/**
 * Service for sending sound notification requests to backend servers.
 */
public interface SoundNotificationService {

    /**
     * Play a sound to a player on their backend server.
     *
     * @param player Player to play sound for
     * @param soundName Minecraft sound name (e.g., "ENTITY_VILLAGER_NO")
     * @param volume Sound volume (0.0 to 1.0)
     * @param pitch Sound pitch (0.5 to 2.0)
     */
    void playSound(Player player, String soundName, float volume, float pitch);
}
