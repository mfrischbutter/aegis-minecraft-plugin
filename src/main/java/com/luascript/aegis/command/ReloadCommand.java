package com.luascript.aegis.command;

import com.luascript.aegis.config.ConfigurationManager;
import com.luascript.aegis.service.MessageManager;
import com.luascript.aegis.service.MessageService;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;

/**
 * Command for reloading the plugin configuration.
 * Usage: /aegisreload
 */
public class ReloadCommand implements SimpleCommand {

    private final ConfigurationManager configurationManager;
    private final MessageService messageService;
    private final MessageManager messageManager;
    private final Logger logger;

    @Inject
    public ReloadCommand(ConfigurationManager configurationManager, MessageService messageService,
                         MessageManager messageManager, Logger logger) {
        this.configurationManager = configurationManager;
        this.messageService = messageService;
        this.messageManager = messageManager;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        try {
            // Reload configuration
            configurationManager.reload();

            // Send success message
            messageService.sendSuccess(source, messageManager.getMessage("reload.success"));

            logger.info("Configuration reloaded by {}",
                    source instanceof com.velocitypowered.api.proxy.Player p ? p.getUsername() : "Console");

        } catch (Exception e) {
            // Send error message
            messageService.sendError(source, messageManager.getMessage("reload.failed",
                    MessageManager.placeholder("error", e.getMessage())));

            logger.error("Failed to reload configuration", e);
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("aegis.reload");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }
}
