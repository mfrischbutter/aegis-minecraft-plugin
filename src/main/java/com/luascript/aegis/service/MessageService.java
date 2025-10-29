package com.luascript.aegis.service;

import com.luascript.aegis.util.ComponentUtil;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import javax.inject.Singleton;

/**
 * Service for sending formatted messages to players and command sources.
 */
@Singleton
public class MessageService {

    /**
     * Send a success message.
     *
     * @param source Command source
     * @param message Message text
     */
    public void sendSuccess(CommandSource source, String message) {
        source.sendMessage(ComponentUtil.success(message));
    }

    /**
     * Send an error message.
     *
     * @param source Command source
     * @param message Message text
     */
    public void sendError(CommandSource source, String message) {
        source.sendMessage(ComponentUtil.error(message));
    }

    /**
     * Send a warning message.
     *
     * @param source Command source
     * @param message Message text
     */
    public void sendWarning(CommandSource source, String message) {
        source.sendMessage(ComponentUtil.warning(message));
    }

    /**
     * Send an info message.
     *
     * @param source Command source
     * @param message Message text
     */
    public void sendInfo(CommandSource source, String message) {
        source.sendMessage(ComponentUtil.info(message));
    }

    /**
     * Send a plain message.
     *
     * @param source Command source
     * @param message Message text
     */
    public void send(CommandSource source, String message) {
        source.sendMessage(ComponentUtil.message(message));
    }

    /**
     * Send a component directly.
     *
     * @param source Command source
     * @param component Component to send
     */
    public void send(CommandSource source, Component component) {
        source.sendMessage(component);
    }

    /**
     * Send a parsed MiniMessage.
     *
     * @param source Command source
     * @param miniMessage MiniMessage formatted string
     */
    public void sendParsed(CommandSource source, String miniMessage) {
        source.sendMessage(ComponentUtil.parse(miniMessage));
    }

    /**
     * Disconnect a player with a kick message.
     *
     * @param player Player to disconnect
     * @param reason Kick reason
     * @param issuer Staff member who issued the kick
     */
    public void kickPlayer(Player player, String reason, String issuer) {
        player.disconnect(ComponentUtil.kickMessage(reason, issuer));
    }
}
