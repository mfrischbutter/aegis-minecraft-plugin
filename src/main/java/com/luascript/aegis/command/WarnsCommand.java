package com.luascript.aegis.command;

import com.luascript.aegis.database.entity.Warn;
import com.luascript.aegis.service.MessageManager;
import com.luascript.aegis.service.MessageService;
import com.luascript.aegis.service.UserService;
import com.luascript.aegis.service.WarnService;
import com.luascript.aegis.util.StringUtil;
import com.luascript.aegis.util.TimeUtil;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Command for viewing player warnings.
 * Usage: /warns <player> [page]
 */
public class WarnsCommand implements SimpleCommand {

    private static final int WARNS_PER_PAGE = 5;

    private final WarnService warnService;
    private final UserService userService;
    private final MessageService messageService;
    private final MessageManager messageManager;
    private final ProxyServer proxyServer;
    private final Logger logger;

    @Inject
    public WarnsCommand(WarnService warnService, UserService userService, MessageService messageService,
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
        if (args.length < 1) {
            messageService.sendError(source, messageManager.getMessage("warns.usage"));
            return;
        }

        String targetName = args[0];

        // Parse page number
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) {
                    messageService.sendError(source, messageManager.getMessage("warns.page_positive"));
                    return;
                }
            } catch (NumberFormatException e) {
                messageService.sendError(source, messageManager.getMessage("warns.invalid_page",
                        MessageManager.placeholder("page", args[1])));
                return;
            }
        }

        final int currentPage = page;

        // Find target player
        userService.findByUsername(targetName)
                .thenCompose(userOpt -> {
                    if (userOpt.isEmpty()) {
                        messageService.sendError(source, messageManager.getMessage("general.player_not_found",
                                MessageManager.placeholder("player", targetName)));
                        return CompletableFuture.completedFuture(null);
                    }

                    UUID targetUuid = userOpt.get().getUuid();

                    // Get paginated warnings and total count
                    return warnService.getActiveWarnsPaginated(targetUuid, currentPage - 1, WARNS_PER_PAGE)
                            .thenCombine(
                                    warnService.countActiveWarns(targetUuid),
                                    (warns, count) -> new Object[]{warns, count, targetName}
                            );
                })
                .thenAccept(result -> {
                    if (result == null) {
                        return; // Player not found case
                    }

                    @SuppressWarnings("unchecked")
                    List<Warn> warns = (List<Warn>) result[0];
                    Long count = (Long) result[1];
                    String playerName = (String) result[2];

                    // Reverse the list to show oldest first within the page
                    Collections.reverse(warns);

                    if (warns.isEmpty() && currentPage == 1) {
                        messageService.send(source, messageManager.getMessage("warns.no_warnings",
                                MessageManager.placeholder("player", playerName)));
                        return;
                    }

                    if (warns.isEmpty()) {
                        messageService.sendError(source, messageManager.getMessage("warns.page_not_exist",
                                MessageManager.placeholder("page", String.valueOf(currentPage))));
                        return;
                    }

                    // Display header
                    messageService.send(source, messageManager.getMessage("warns.header",
                            MessageManager.builder()
                                    .add("player", playerName)
                                    .add("page", String.valueOf(currentPage))
                                    .build()));
                    messageService.send(source, messageManager.getMessage("warns.total_active",
                            MessageManager.placeholder("count", String.valueOf(count))));
                    messageService.send(source, "");

                    // Display each warning
                    for (Warn warn : warns) {
                        String issuer = warn.getIssuer() != null ?
                                warn.getIssuer().getUsername() : messageManager.getMessage("labels.console");

                        messageService.send(source, messageManager.getMessage("warns.entry_format",
                                MessageManager.builder()
                                        .add("id", String.valueOf(warn.getId()))
                                        .add("issuer", issuer)
                                        .build()));

                        messageService.send(source, messageManager.getMessage("warns.entry_reason",
                                MessageManager.placeholder("reason", warn.getReason())));
                        messageService.send(source, messageManager.getMessage("warns.entry_date",
                                MessageManager.placeholder("date", TimeUtil.formatInstant(warn.getCreatedAt()))));

                        if (warn.getExpiresAt() != null) {
                            messageService.send(source, messageManager.getMessage("warns.entry_expires",
                                    MessageManager.placeholder("expires", TimeUtil.formatInstant(warn.getExpiresAt()))));
                        } else {
                            messageService.send(source, messageManager.getMessage("warns.entry_never_expires"));
                        }

                        messageService.send(source, "");
                    }

                    // Display footer with navigation info
                    messageService.send(source, messageManager.getMessage("warns.footer"));
                    messageService.send(source, "");

                    if (warns.size() == WARNS_PER_PAGE) {
                        messageService.send(source, messageManager.getMessage("warns.next_page",
                                MessageManager.builder()
                                        .add("player", playerName)
                                        .add("page", String.valueOf(currentPage + 1))
                                        .build()));
                    }

                    if (currentPage > 1) {
                        messageService.send(source, messageManager.getMessage("warns.previous_page",
                                MessageManager.builder()
                                        .add("player", playerName)
                                        .add("page", String.valueOf(currentPage - 1))
                                        .build()));
                    }

                    messageService.send(source, messageManager.getMessage("warns.use_unwarn"));
                })
                .exceptionally(e -> {
                    messageService.sendError(source, messageManager.getMessage("warns.failed",
                            MessageManager.placeholder("error", e.getMessage())));
                    logger.error("Error executing warns command", e);
                    return null;
                });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("aegis.warns") ||
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

        // Suggest page numbers for second argument
        if (args.length == 2) {
            return List.of("1", "2", "3");
        }

        return List.of();
    }
}
