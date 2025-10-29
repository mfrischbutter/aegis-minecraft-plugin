package com.luascript.aegis.command;

import com.luascript.aegis.service.BanService;
import com.luascript.aegis.service.MessageManager;
import com.luascript.aegis.service.MessageService;
import com.luascript.aegis.service.UserService;
import com.luascript.aegis.util.Constants;
import com.luascript.aegis.util.StringUtil;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Command for unbanning players.
 * Usage: /unban <player>
 */
public class UnbanCommand implements SimpleCommand {

    private final BanService banService;
    private final UserService userService;
    private final MessageService messageService;
    private final MessageManager messageManager;
    private final ProxyServer proxyServer;
    private final Logger logger;

    @Inject
    public UnbanCommand(BanService banService, UserService userService, MessageService messageService,
                        MessageManager messageManager, ProxyServer proxyServer, Logger logger) {
        this.banService = banService;
        this.userService = userService;
        this.messageService = messageService;
        this.messageManager = messageManager;
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // Check arguments
        if (args.length < 2) {
            messageService.sendError(source, messageManager.getMessage("unban.usage"));
            return;
        }

        String targetName = args[0];
        String reason = StringUtil.joinFrom(args, 1);

        // Find target player
        userService.findByUsername(targetName)
                .thenCompose(userOpt -> {
                    if (userOpt.isEmpty()) {
                        messageService.sendError(source, messageManager.getMessage("general.player_not_found",
                                MessageManager.placeholder("player", targetName)));
                        return CompletableFuture.completedFuture(null);
                    }

                    UUID targetUuid = userOpt.get().getUuid();

                    // Check if player is actually banned
                    return banService.isBanned(targetUuid).thenCompose(isBanned -> {
                        if (!isBanned) {
                            messageService.sendError(source, messageManager.getMessage("unban.not_banned",
                                    MessageManager.placeholder("player", targetName)));
                            return CompletableFuture.completedFuture(null);
                        }

                        // Get unbanner UUID
                        UUID unbannerUuid = getIssuerUuid(source);

                        // Remove the ban
                        return banService.removeBan(targetUuid, unbannerUuid, reason);
                    });
                })
                .thenAccept(success -> {
                    if (success != null && success) {
                        messageService.sendSuccess(source, messageManager.getMessage("unban.success",
                                MessageManager.placeholder("player", targetName)));

                        String issuerName = source instanceof Player player ?
                                player.getUsername() : "Console";

                        logger.info("{} unbanned {}", issuerName, targetName);
                    }
                })
                .exceptionally(e -> {
                    messageService.sendError(source, messageManager.getMessage("unban.failed",
                            MessageManager.placeholder("error", e.getMessage())));
                    logger.error("Error executing unban command", e);
                    return null;
                });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("aegis.unban");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        // Suggest online player names for first argument
        if (args.length <= 1) {
            List<String> playerNames = new ArrayList<>();
            proxyServer.getAllPlayers().forEach(player -> playerNames.add(player.getUsername()));
            return StringUtil.getMatches(args.length == 1 ? args[0] : "", playerNames);
        }

        return List.of();
    }

    private UUID getIssuerUuid(CommandSource source) {
        if (source instanceof Player player) {
            return player.getUniqueId();
        }
        // For console, use the special console UUID
        return Constants.CONSOLE_UUID;
    }
}
