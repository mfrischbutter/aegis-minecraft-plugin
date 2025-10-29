package com.luascript.aegis.command;

import com.luascript.aegis.config.ModerationConfig;
import com.luascript.aegis.database.entity.Warn;
import com.luascript.aegis.service.MessageManager;
import com.luascript.aegis.service.MessageService;
import com.luascript.aegis.service.SoundNotificationService;
import com.luascript.aegis.service.UserService;
import com.luascript.aegis.service.WarnService;
import com.luascript.aegis.util.Constants;
import com.luascript.aegis.util.StringUtil;
import com.luascript.aegis.util.ValidationUtil;
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
 * Command for warning players.
 * Usage: /warn <player> <reason...>
 */
public class WarnCommand implements SimpleCommand {

    private final WarnService warnService;
    private final UserService userService;
    private final MessageService messageService;
    private final MessageManager messageManager;
    private final SoundNotificationService soundNotificationService;
    private final ModerationConfig moderationConfig;
    private final ProxyServer proxyServer;
    private final Logger logger;

    @Inject
    public WarnCommand(WarnService warnService, UserService userService, MessageService messageService,
                       MessageManager messageManager, SoundNotificationService soundNotificationService,
                       ModerationConfig moderationConfig, ProxyServer proxyServer, Logger logger) {
        this.warnService = warnService;
        this.userService = userService;
        this.messageService = messageService;
        this.messageManager = messageManager;
        this.soundNotificationService = soundNotificationService;
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
            messageService.sendError(source, messageManager.getMessage("warn.usage"));
            return;
        }

        String targetName = args[0];
        String reason = StringUtil.joinFrom(args, 1);

        // Validate reason
        try {
            ValidationUtil.validateReason(reason);
        } catch (Exception e) {
            messageService.sendError(source, e.getMessage());
            return;
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

                    // Get server name
                    String serverName = source instanceof Player player ?
                            player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("Unknown") :
                            "Console";

                    // Create warning (will automatically escalate if threshold is met)
                    return warnService.createWarn(targetUuid, reason, issuerUuid, serverName, null);
                })
                .thenCompose(warn -> {
                    if (warn != null) {
                        UUID targetUuid = warn.getPlayer().getUuid();

                        // Get updated warn count
                        return warnService.countActiveWarns(targetUuid)
                                .thenAccept(count -> {
                                    messageService.sendSuccess(source, messageManager.getMessage("warn.success",
                                            MessageManager.placeholder("player", targetName)));
                                    messageService.send(source, messageManager.getMessage("labels.reason") + reason);
                                    messageService.send(source, messageManager.getMessage("warn.total_warnings",
                                            MessageManager.placeholder("count", String.valueOf(count))));

                                    // Notify the player if online
                                    proxyServer.getPlayer(targetUuid).ifPresent(player -> {
                                        messageService.sendWarning(player, messageManager.getMessage("warn.player_notification",
                                                MessageManager.placeholder("reason", reason)));
                                        messageService.send(player, messageManager.getMessage("warn.player_total",
                                                MessageManager.placeholder("count", String.valueOf(count))));
                                        messageService.send(player, messageManager.getMessage("warn.player_consequence"));

                                        // Play warning sound
                                        soundNotificationService.playSound(
                                                player,
                                                moderationConfig.getWarningSoundName(),
                                                moderationConfig.getWarningSoundVolume(),
                                                moderationConfig.getWarningSoundPitch()
                                        );
                                    });

                                    logger.info("{} warned {}: {} (total warnings: {})",
                                            source instanceof Player p ? p.getUsername() : "Console",
                                            targetName, reason, count);
                                });
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .exceptionally(e -> {
                    messageService.sendError(source, messageManager.getMessage("warn.failed",
                            MessageManager.placeholder("error", e.getMessage())));
                    logger.error("Error executing warn command", e);
                    return null;
                });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("aegis.warn");
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
