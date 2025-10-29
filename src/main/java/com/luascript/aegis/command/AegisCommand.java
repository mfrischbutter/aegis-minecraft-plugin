package com.luascript.aegis.command;

import com.luascript.aegis.BuildConstants;
import com.luascript.aegis.service.MessageManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import javax.inject.Inject;

/**
 * Main Aegis command for plugin information and diagnostics.
 * Usage: /aegis
 */
public class AegisCommand implements SimpleCommand {

    private final Logger logger;
    private final MessageManager messageManager;

    @Inject
    public AegisCommand(Logger logger, MessageManager messageManager) {
        this.logger = logger;
        this.messageManager = messageManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        source.sendMessage(Component.text("=".repeat(50), NamedTextColor.GRAY));
        source.sendMessage(Component.text(messageManager.getMessage("aegis.header"), NamedTextColor.RED).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        source.sendMessage(Component.text(messageManager.getMessage("aegis.version"), NamedTextColor.GRAY)
                .append(Component.text(BuildConstants.VERSION, NamedTextColor.WHITE)));
        source.sendMessage(Component.text(messageManager.getMessage("aegis.author"), NamedTextColor.GRAY)
                .append(Component.text("Michael Frischbutter", NamedTextColor.WHITE)));
        source.sendMessage(Component.empty());

        source.sendMessage(Component.text(messageManager.getMessage("aegis.available_commands"), NamedTextColor.YELLOW));
        source.sendMessage(Component.text("  /ban <player> <reason>", NamedTextColor.WHITE));
        source.sendMessage(Component.text("  /tempban <player> <duration> <reason>", NamedTextColor.WHITE));
        source.sendMessage(Component.text("  /unban <player>", NamedTextColor.WHITE));
        source.sendMessage(Component.text("  /baninfo <player>", NamedTextColor.WHITE));
        source.sendMessage(Component.text("  /banlist [page]", NamedTextColor.WHITE));
        source.sendMessage(Component.text("  /warn <player> <reason>", NamedTextColor.WHITE));
        source.sendMessage(Component.text("  /warns <player>", NamedTextColor.WHITE));
        source.sendMessage(Component.text("  /unwarn <player> <warn-id>", NamedTextColor.WHITE));
        source.sendMessage(Component.text("  /clearwarns <player>", NamedTextColor.WHITE));
        source.sendMessage(Component.text("  /kick <player> <reason>", NamedTextColor.WHITE));
        source.sendMessage(Component.empty());

        source.sendMessage(Component.text(messageManager.getMessage("aegis.permissions"), NamedTextColor.YELLOW));
        source.sendMessage(Component.text("  " + messageManager.getMessage("aegis.permissions_all"), NamedTextColor.WHITE));
        source.sendMessage(Component.text("  " + messageManager.getMessage("aegis.permissions_individual"), NamedTextColor.WHITE));
        source.sendMessage(Component.text("=".repeat(50), NamedTextColor.GRAY));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        // No permission required - anyone can view help
        return true;
    }
}
