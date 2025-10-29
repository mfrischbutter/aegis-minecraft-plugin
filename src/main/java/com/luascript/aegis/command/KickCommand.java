package com.luascript.aegis.command;

import com.luascript.aegis.config.ModerationConfig;
import com.luascript.aegis.service.KickService;
import com.luascript.aegis.service.MessageManager;
import com.luascript.aegis.service.MessageService;
import com.luascript.aegis.service.UserService;
import com.luascript.aegis.util.ComponentUtil;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Command for kicking players.
 * Usage: /kick <player> <reason...>
 */
public class KickCommand implements SimpleCommand {

    private final KickService kickService;
    private final UserService userService;
    private final MessageService messageService;
    private final MessageManager messageManager;
    private final ModerationConfig moderationConfig;
    private final ProxyServer proxyServer;
    private final Logger logger;

    @Inject
    public KickCommand(KickService kickService, UserService userService, MessageService messageService,
                       MessageManager messageManager, ModerationConfig moderationConfig,
                       ProxyServer proxyServer, Logger logger) {
        this.kickService = kickService;
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
            messageService.sendError(source, messageManager.getMessage("kick.usage"));
            return;
        }

        String targetName = args[0];
        String reason = args.length > 1 ?
                StringUtil.joinFrom(args, 1) :
                moderationConfig.getDefaultKickReason();

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

        // Check if target is online
        Optional<Player> targetPlayerOpt = proxyServer.getPlayer(targetName);
        if (targetPlayerOpt.isEmpty()) {
            messageService.sendError(source, messageManager.getMessage("general.player_not_online",
                    MessageManager.placeholder("player", targetName)));
            return;
        }

        Player targetPlayer = targetPlayerOpt.get();
        UUID targetUuid = targetPlayer.getUniqueId();

        // Get issuer name for kick message
        String issuerName = source instanceof Player player ?
                player.getUsername() : "Console";

        // Get server name
        String serverName = source instanceof Player player ?
                player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("Unknown") :
                "Console";

        // Create kick record
        kickService.createKick(targetUuid, reason, issuerUuid, serverName)
                .thenAccept(kick -> {
                    messageService.sendSuccess(source, messageManager.getMessage("kick.success",
                            MessageManager.placeholder("player", targetName)));
                    messageService.send(source, messageManager.getMessage("labels.reason") + reason);

                    // Disconnect the player
                    targetPlayer.disconnect(ComponentUtil.kickMessage(reason, issuerName));

                    logger.info("{} kicked {}: {}",
                            issuerName, targetName, reason);
                })
                .exceptionally(e -> {
                    messageService.sendError(source, messageManager.getMessage("kick.failed",
                            MessageManager.placeholder("error", e.getMessage())));
                    logger.error("Error executing kick command", e);
                    return null;
                });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("aegis.kick");
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
