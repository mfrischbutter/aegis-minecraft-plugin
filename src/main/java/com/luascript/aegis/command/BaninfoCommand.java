package com.luascript.aegis.command;

import com.luascript.aegis.database.entity.Ban;
import com.luascript.aegis.service.BanService;
import com.luascript.aegis.service.MessageManager;
import com.luascript.aegis.service.MessageService;
import com.luascript.aegis.service.UserService;
import com.luascript.aegis.util.StringUtil;
import com.luascript.aegis.util.TimeUtil;
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
 * Command for viewing ban information.
 * Usage: /baninfo <player>
 */
public class BaninfoCommand implements SimpleCommand {

    private final BanService banService;
    private final UserService userService;
    private final MessageService messageService;
    private final MessageManager messageManager;
    private final ProxyServer proxyServer;
    private final Logger logger;

    @Inject
    public BaninfoCommand(BanService banService, UserService userService, MessageService messageService,
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
        if (args.length < 1) {
            messageService.sendError(source, messageManager.getMessage("baninfo.usage"));
            return;
        }

        String targetName = args[0];

        // Find target player
        userService.findByUsername(targetName)
                .thenCompose(userOpt -> {
                    if (userOpt.isEmpty()) {
                        messageService.sendError(source, messageManager.getMessage("general.player_not_found",
                                MessageManager.placeholder("player", targetName)));
                        return CompletableFuture.completedFuture(null);
                    }

                    UUID targetUuid = userOpt.get().getUuid();

                    // Get active ban
                    return banService.getActiveBan(targetUuid);
                })
                .thenAccept(banOpt -> {
                    if (banOpt == null) {
                        return; // Player not found case
                    }

                    if (banOpt.isEmpty()) {
                        messageService.send(source, messageManager.getMessage("baninfo.not_banned",
                                MessageManager.placeholder("player", targetName)));
                        return;
                    }

                    Ban ban = banOpt.get();

                    // Display ban information
                    messageService.send(source, messageManager.getMessage("baninfo.header"));
                    messageService.send(source, messageManager.getMessage("baninfo.player_label") + ban.getPlayer().getUsername());
                    messageService.send(source, messageManager.getMessage("baninfo.uuid_label") + ban.getPlayer().getUuid());
                    messageService.send(source, messageManager.getMessage("baninfo.reason_label") + ban.getReason());
                    messageService.send(source, messageManager.getMessage("baninfo.type_label") + ban.getBanType().toString());
                    messageService.send(source, messageManager.getMessage("baninfo.issued_by_label") +
                            (ban.getIssuer() != null ? ban.getIssuer().getUsername() : messageManager.getMessage("baninfo.console")));
                    messageService.send(source, messageManager.getMessage("baninfo.issued_at_label") + TimeUtil.formatInstant(ban.getCreatedAt()));

                    if (ban.getExpiresAt() != null) {
                        messageService.send(source, messageManager.getMessage("baninfo.expires_at_label") + TimeUtil.formatInstant(ban.getExpiresAt()));
                        messageService.send(source, messageManager.getMessage("baninfo.time_remaining") +
                                TimeUtil.formatDuration(java.time.Duration.between(
                                        java.time.Instant.now(), ban.getExpiresAt())));
                    } else {
                        messageService.send(source, messageManager.getMessage("baninfo.expires_at_label") +
                                messageManager.getMessage("baninfo.never_expires"));
                    }

                    if (ban.getIpAddress() != null) {
                        messageService.send(source, messageManager.getMessage("baninfo.ip_ban_yes",
                                MessageManager.placeholder("ip", ban.getIpAddress())));
                    } else {
                        messageService.send(source, messageManager.getMessage("baninfo.ip_ban_no"));
                    }

                    messageService.send(source, messageManager.getMessage("baninfo.server_label") + ban.getServerName());
                    messageService.send(source, messageManager.getMessage("baninfo.ban_id_label") + ban.getId());
                    messageService.send(source, messageManager.getMessage("baninfo.footer"));
                })
                .exceptionally(e -> {
                    messageService.sendError(source, messageManager.getMessage("baninfo.failed",
                            MessageManager.placeholder("error", e.getMessage())));
                    logger.error("Error executing baninfo command", e);
                    return null;
                });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("aegis.baninfo") ||
               invocation.source().hasPermission("aegis.ban");
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
}
