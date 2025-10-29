package com.luascript.aegis.service;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.luascript.aegis.config.ModerationConfig;
import com.luascript.aegis.util.Constants;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of SoundNotificationService using Velocity plugin messaging.
 */
@Singleton
public class SoundNotificationServiceImpl implements SoundNotificationService {

    private final ModerationConfig config;
    private final Logger logger;
    private final MinecraftChannelIdentifier channel;

    @Inject
    public SoundNotificationServiceImpl(ModerationConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.channel = MinecraftChannelIdentifier.from(Constants.PLUGIN_CHANNEL);
    }

    @Override
    public void playSound(Player player, String soundName, float volume, float pitch) {
        // Check if sounds are enabled
        if (!config.isSoundsEnabled()) {
            return;
        }

        // Check if player is connected to a backend server
        if (!player.getCurrentServer().isPresent()) {
            logger.debug("Cannot play sound for {}: not connected to a server", player.getUsername());
            return;
        }

        ServerConnection server = player.getCurrentServer().get();

        try {
            // Create plugin message data
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PLAY_SOUND"); // Message type
            out.writeUTF(player.getUniqueId().toString()); // Player UUID
            out.writeUTF(soundName); // Sound name
            out.writeFloat(volume); // Volume
            out.writeFloat(pitch); // Pitch

            // Send plugin message to backend server
            server.sendPluginMessage(channel, out.toByteArray());

            logger.debug("Sent sound notification to {} on server {}: {} (vol: {}, pitch: {})",
                    player.getUsername(), server.getServerInfo().getName(), soundName, volume, pitch);
        } catch (Exception e) {
            logger.error("Failed to send sound notification to backend server for player {}",
                    player.getUsername(), e);
        }
    }
}
