package com.luascript.aegis.command;

import com.luascript.aegis.database.entity.Ban;
import com.luascript.aegis.service.BanService;
import com.luascript.aegis.service.MessageManager;
import com.luascript.aegis.service.MessageService;
import com.luascript.aegis.util.TimeUtil;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;

/**
 * Command for viewing a list of all active bans.
 * Usage: /banlist [page]
 */
public class BanlistCommand implements SimpleCommand {

    private static final int BANS_PER_PAGE = 10;

    private final BanService banService;
    private final MessageService messageService;
    private final MessageManager messageManager;
    private final Logger logger;

    @Inject
    public BanlistCommand(BanService banService, MessageService messageService, MessageManager messageManager, Logger logger) {
        this.banService = banService;
        this.messageService = messageService;
        this.messageManager = messageManager;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // Parse page number
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) {
                    messageService.sendError(source, messageManager.getMessage("banlist.page_positive"));
                    return;
                }
            } catch (NumberFormatException e) {
                messageService.sendError(source, messageManager.getMessage("banlist.invalid_page",
                        MessageManager.placeholder("page", args[0])));
                return;
            }
        }

        final int currentPage = page;

        // Get paginated bans (page is 0-indexed in service)
        banService.getActiveBans(page - 1, BANS_PER_PAGE)
                .thenAccept(bans -> {
                    if (bans.isEmpty() && currentPage == 1) {
                        messageService.send(source, messageManager.getMessage("banlist.no_bans"));
                        return;
                    }

                    if (bans.isEmpty()) {
                        messageService.sendError(source, messageManager.getMessage("banlist.page_not_exist",
                                MessageManager.placeholder("page", String.valueOf(currentPage))));
                        return;
                    }

                    // Display header
                    messageService.send(source, messageManager.getMessage("banlist.header",
                            MessageManager.placeholder("page", String.valueOf(currentPage))));

                    // Display each ban
                    for (Ban ban : bans) {
                        String banType = ban.getBanType().toString();
                        String expiration = ban.getExpiresAt() != null ?
                                TimeUtil.formatInstant(ban.getExpiresAt()) : "Never";
                        String issuer = ban.getIssuer() != null ?
                                ban.getIssuer().getUsername() : messageManager.getMessage("labels.console");

                        messageService.send(source, messageManager.getMessage("banlist.entry_format",
                                MessageManager.builder()
                                        .add("id", String.valueOf(ban.getId()))
                                        .add("player", ban.getPlayer().getUsername())
                                        .add("type", banType)
                                        .add("issuer", issuer)
                                        .build()));

                        messageService.send(source, messageManager.getMessage("banlist.entry_reason",
                                MessageManager.placeholder("reason", ban.getReason())));

                        if (ban.getExpiresAt() != null) {
                            messageService.send(source, messageManager.getMessage("banlist.entry_expires",
                                    MessageManager.placeholder("expires", expiration)));
                        }

                        if (ban.getIpAddress() != null) {
                            messageService.send(source, messageManager.getMessage("banlist.entry_ip",
                                    MessageManager.placeholder("ip", ban.getIpAddress())));
                        }
                    }

                    // Display footer with navigation info
                    messageService.send(source, messageManager.getMessage("banlist.footer"));

                    if (bans.size() == BANS_PER_PAGE) {
                        messageService.send(source, messageManager.getMessage("banlist.next_page",
                                MessageManager.placeholder("page", String.valueOf(currentPage + 1))));
                    }

                    if (currentPage > 1) {
                        messageService.send(source, messageManager.getMessage("banlist.previous_page",
                                MessageManager.placeholder("page", String.valueOf(currentPage - 1))));
                    }
                })
                .exceptionally(e -> {
                    messageService.sendError(source, messageManager.getMessage("banlist.failed",
                            MessageManager.placeholder("error", e.getMessage())));
                    logger.error("Error executing banlist command", e);
                    return null;
                });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("aegis.banlist") ||
               invocation.source().hasPermission("aegis.ban");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        // Suggest page numbers
        if (args.length <= 1) {
            return List.of("1", "2", "3");
        }

        return List.of();
    }
}
