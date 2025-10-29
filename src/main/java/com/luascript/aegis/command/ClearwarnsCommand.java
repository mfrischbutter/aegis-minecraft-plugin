package com.luascript.aegis.command;

import com.luascript.aegis.service.MessageManager;
import com.luascript.aegis.service.MessageService;
import com.luascript.aegis.service.UserService;
import com.luascript.aegis.service.WarnService;
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
 * Command for clearing all warnings for a player.
 * Usage: /clearwarns <player>
 */
public class ClearwarnsCommand implements SimpleCommand {

    private final WarnService warnService;
    private final UserService userService;
    private final MessageService messageService;
    private final MessageManager messageManager;
    private final ProxyServer proxyServer;
    private final Logger logger;

    @Inject
    public ClearwarnsCommand(WarnService warnService, UserService userService, MessageService messageService,
                             MessageManager messageManager, ProxyServer proxyServer, Logger logger) {
        this.warnService = warnService;
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
            messageService.sendError(source, messageManager.getMessage("clearwarns.usage"));
            return;
        }

        String targetName = args[0];
        String reason = StringUtil.joinFrom(args, 1);

        // Get clearer UUID
        UUID clearerUuid = getIssuerUuid(source);
        if (clearerUuid == null) {
            messageService.sendError(source, messageManager.getMessage("general.only_players"));
            return;
        }

        String clearerName = source instanceof Player player ?
                player.getUsername() : "Console";

        // Find target player
        userService.findByUsername(targetName)
                .thenCompose(userOpt -> {
                    if (userOpt.isEmpty()) {
                        messageService.sendError(source, messageManager.getMessage("general.player_not_found",
                                MessageManager.placeholder("player", targetName)));
                        return CompletableFuture.completedFuture(null);
                    }

                    UUID targetUuid = userOpt.get().getUuid();

                    // Get current warn count before clearing
                    return warnService.countActiveWarns(targetUuid)
                            .thenCompose(count -> {
                                if (count == 0) {
                                    messageService.send(source, messageManager.getMessage("clearwarns.no_warnings",
                                            MessageManager.placeholder("player", targetName)));
                                    return CompletableFuture.completedFuture(null);
                                }

                                // Clear warnings
                                return warnService.clearWarns(targetUuid, clearerUuid, reason)
                                        .thenApply(v -> count);
                            });
                })
                .thenAccept(count -> {
                    if (count != null && count > 0) {
                        messageService.sendSuccess(source, messageManager.getMessage("clearwarns.success",
                                MessageManager.builder()
                                        .add("count", String.valueOf(count))
                                        .add("player", targetName)
                                        .build()));

                        // Notify the player if online
                        proxyServer.getPlayer(targetName).ifPresent(player -> {
                            messageService.sendSuccess(player, messageManager.getMessage("clearwarns.player_notification",
                                    MessageManager.placeholder("clearer", clearerName)));
                        });

                        logger.info("{} cleared {} warning(s) from {}",
                                clearerName, count, targetName);
                    }
                })
                .exceptionally(e -> {
                    messageService.sendError(source, messageManager.getMessage("clearwarns.failed",
                            MessageManager.placeholder("error", e.getMessage())));
                    logger.error("Error executing clearwarns command", e);
                    return null;
                });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("aegis.clearwarns") ||
               invocation.source().hasPermission("aegis.admin");
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
        return null;
    }
}
