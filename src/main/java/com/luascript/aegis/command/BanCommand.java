package com.luascript.aegis.command;

import com.luascript.aegis.config.ModerationConfig;
import com.luascript.aegis.database.entity.Ban;
import com.luascript.aegis.service.BanService;
import com.luascript.aegis.service.MessageManager;
import com.luascript.aegis.service.MessageService;
import com.luascript.aegis.service.UserService;
import com.luascript.aegis.util.Constants;
import com.luascript.aegis.util.StringUtil;
import com.luascript.aegis.util.TimeUtil;
import com.luascript.aegis.util.ValidationUtil;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Command for banning players permanently.
 * Usage: /ban <player> <reason...>
 */
public class BanCommand implements SimpleCommand {

    private final BanService banService;
    private final UserService userService;
    private final MessageService messageService;
    private final MessageManager messageManager;
    private final ModerationConfig moderationConfig;
    private final ProxyServer proxyServer;
    private final Logger logger;

    @Inject
    public BanCommand(BanService banService, UserService userService, MessageService messageService,
                      MessageManager messageManager, ModerationConfig moderationConfig, ProxyServer proxyServer, Logger logger) {
        this.banService = banService;
        this.userService = userService;
        this.messageService = messageService;
        this.messageManager = messageManager;
        this.moderationConfig = moderationConfig;
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // Check arguments
        if (args.length < 1) {
            messageService.sendError(source, messageManager.getMessage("ban.usage"));
            return;
        }

        String targetName = args[0];
        String reason = args.length > 1 ?
                StringUtil.joinFrom(args, 1) :
                moderationConfig.getDefaultBanReason();

        // Validate reason if provided by user
        if (args.length > 1) {
            try {
                ValidationUtil.validateReason(reason);
            } catch (Exception e) {
                messageService.sendError(source, e.getMessage());
                return;
            }
        }

        // Get issuer UUID
        UUID issuerUuid = getIssuerUuid(source);

        // Find target player
        userService.findByUsername(targetName)
                .thenCompose(userOpt -> {
                    if (userOpt.isEmpty()) {
                        messageService.sendError(source, messageManager.getMessage("general.player_not_found",
                                MessageManager.placeholder("player", targetName)));
                        return CompletableFuture.completedFuture(null);
                    }

                    UUID targetUuid = userOpt.get().getUuid();

                    // Check if already banned
                    return banService.isBanned(targetUuid).thenCompose(isBanned -> {
                        if (isBanned) {
                            messageService.sendError(source, messageManager.getMessage("ban.already_banned",
                                    MessageManager.placeholder("player", targetName)));
                            return CompletableFuture.completedFuture(null);
                        }

                        // Get IP address if IP banning is enabled
                        String ipAddress = null;
                        if (moderationConfig.isIpBanEnabled()) {
                            Optional<Player> onlinePlayer = proxyServer.getPlayer(targetUuid);
                            if (onlinePlayer.isPresent()) {
                                ipAddress = onlinePlayer.get().getRemoteAddress().getAddress().getHostAddress();
                            }
                        }

                        // Create permanent ban
                        String serverName = source instanceof Player player ?
                                player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("Unknown") :
                                "Console";

                        return banService.createPermanentBan(targetUuid, reason, issuerUuid, serverName, ipAddress);
                    });
                })
                .thenAccept(ban -> {
                    if (ban != null) {
                        messageService.sendSuccess(source, messageManager.getMessage("ban.success",
                                MessageManager.placeholder("player", targetName)));
                        messageService.send(source, messageManager.getMessage("ban.reason_label") + reason);
                        messageService.send(source, messageManager.getMessage("ban.type_label") +
                                messageManager.getMessage("ban.type_permanent"));

                        // Kick the player if they're online
                        proxyServer.getPlayer(ban.getPlayer().getUuid()).ifPresent(player -> {
                            player.disconnect(net.kyori.adventure.text.Component.text(
                                    messageManager.getMessage("ban.kick_message_permanent",
                                            MessageManager.placeholder("reason", reason))));
                        });

                        logger.info("{} banned {} permanently: {}",
                                source instanceof Player p ? p.getUsername() : "Console",
                                targetName, reason);
                    }
                })
                .exceptionally(e -> {
                    messageService.sendError(source, messageManager.getMessage("ban.failed",
                            MessageManager.placeholder("error", e.getMessage())));
                    logger.error("Error executing ban command", e);
                    return null;
                });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("aegis.ban");
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
