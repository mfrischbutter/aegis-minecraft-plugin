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
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Command for temporarily banning players.
 * Usage: /tempban <player> <hours> [reason...]
 */
public class TempbanCommand implements SimpleCommand {

    private final BanService banService;
    private final UserService userService;
    private final MessageService messageService;
    private final MessageManager messageManager;
    private final ModerationConfig moderationConfig;
    private final ProxyServer proxyServer;
    private final Logger logger;

    @Inject
    public TempbanCommand(BanService banService, UserService userService, MessageService messageService,
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
        if (args.length < 2) {
            messageService.sendError(source, messageManager.getMessage("tempban.usage"));
            messageService.send(source, messageManager.getMessage("tempban.usage_example"));
            messageService.send(source, messageManager.getMessage("tempban.usage_format"));
            return;
        }

        String targetName = args[0];
        String hoursStr = args[1];
        String reason = args.length > 2 ?
                StringUtil.joinFrom(args, 2) :
                moderationConfig.getDefaultBanReason();

        // Parse hours as integer
        int hours;
        try {
            hours = Integer.parseInt(hoursStr);
        } catch (NumberFormatException e) {
            messageService.sendError(source, messageManager.getMessage("tempban.invalid_duration",
                    MessageManager.placeholder("duration", hoursStr)));
            messageService.send(source, messageManager.getMessage("tempban.valid_formats"));
            return;
        }

        // Validate hours is not too short or too long
        if (hours < 1) {
            messageService.sendError(source, messageManager.getMessage("tempban.too_short"));
            return;
        }

        if (hours > 8760) { // 8760 = 365 days
            messageService.sendError(source, messageManager.getMessage("tempban.too_long"));
            return;
        }

        // Convert hours to Duration
        Duration duration = Duration.ofHours(hours);

        // Validate reason if provided by user
        if (args.length > 2) {
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
                            messageService.sendError(source, messageManager.getMessage("tempban.already_banned",
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

                        // Get server name
                        String serverName = source instanceof Player player ?
                                player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("Unknown") :
                                "Console";

                        // Create temporary ban
                        return banService.createTemporaryBan(targetUuid, reason, issuerUuid, duration, serverName, ipAddress);
                    });
                })
                .thenAccept(ban -> {
                    if (ban != null) {
                        messageService.sendSuccess(source, messageManager.getMessage("tempban.success",
                                MessageManager.placeholder("player", targetName)));
                        messageService.send(source, messageManager.getMessage("ban.reason_label") + reason);
                        messageService.send(source, messageManager.getMessage("tempban.duration_label") + hours + " Stunden");
                        messageService.send(source, messageManager.getMessage("tempban.expires_label") +
                                TimeUtil.formatInstant(ban.getExpiresAt()));

                        // Kick the player if they're online
                        proxyServer.getPlayer(ban.getPlayer().getUuid()).ifPresent(player -> {
                            player.disconnect(Component.text(messageManager.getMessage("tempban.kick_message",
                                    MessageManager.builder()
                                            .add("hours", String.valueOf(hours))
                                            .add("reason", reason)
                                            .add("expires", TimeUtil.formatInstant(ban.getExpiresAt()))
                                            .build())));
                        });

                        String issuerName = source instanceof Player p ? p.getUsername() : "Console";
                        logger.info("{} temporarily banned {} for {} hours: {}",
                                issuerName, targetName, hours, reason);
                    }
                })
                .exceptionally(e -> {
                    messageService.sendError(source, messageManager.getMessage("tempban.failed",
                            MessageManager.placeholder("error", e.getMessage())));
                    logger.error("Error executing tempban command", e);
                    return null;
                });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("aegis.tempban") ||
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

        // Suggest hour examples for second argument
        if (args.length == 2) {
            return List.of("1", "6", "12", "24", "48", "168", "720");
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
