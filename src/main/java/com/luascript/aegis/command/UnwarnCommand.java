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

/**
 * Command for removing specific warnings.
 * Usage: /unwarn <player> <warn-id>
 */
public class UnwarnCommand implements SimpleCommand {

    private final WarnService warnService;
    private final UserService userService;
    private final MessageService messageService;
    private final MessageManager messageManager;
    private final ProxyServer proxyServer;
    private final Logger logger;

    @Inject
    public UnwarnCommand(WarnService warnService, UserService userService, MessageService messageService,
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
        if (args.length < 3) {
            messageService.sendError(source, messageManager.getMessage("unwarn.usage"));
            messageService.send(source, messageManager.getMessage("unwarn.usage_help"));
            return;
        }

        String targetName = args[0];
        String warnIdStr = args[1];
        String reason = StringUtil.joinFrom(args, 2);

        // Parse warn ID
        long warnId;
        try {
            warnId = Long.parseLong(warnIdStr);
        } catch (NumberFormatException e) {
            messageService.sendError(source, messageManager.getMessage("unwarn.invalid_id",
                    MessageManager.placeholder("id", warnIdStr)));
            return;
        }

        // Get remover UUID
        UUID removerUuid = getIssuerUuid(source);
        if (removerUuid == null) {
            messageService.sendError(source, messageManager.getMessage("general.only_players"));
            return;
        }

        String removerName = source instanceof Player player ?
                player.getUsername() : "Console";

        // Remove the warning
        warnService.removeWarn(warnId, removerUuid, reason)
                .thenAccept(success -> {
                    if (success) {
                        messageService.sendSuccess(source, messageManager.getMessage("unwarn.success",
                                MessageManager.builder()
                                        .add("id", String.valueOf(warnId))
                                        .add("player", targetName)
                                        .build()));

                        logger.info("{} removed warning #{} from {}",
                                removerName, warnId, targetName);
                    } else {
                        messageService.sendError(source, messageManager.getMessage("unwarn.not_found",
                                MessageManager.placeholder("id", String.valueOf(warnId))));
                    }
                })
                .exceptionally(e -> {
                    messageService.sendError(source, messageManager.getMessage("unwarn.failed",
                            MessageManager.placeholder("error", e.getMessage())));
                    logger.error("Error executing unwarn command", e);
                    return null;
                });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("aegis.unwarn") ||
               invocation.source().hasPermission("aegis.warn");
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
