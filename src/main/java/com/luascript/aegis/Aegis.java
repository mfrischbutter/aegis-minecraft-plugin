package com.luascript.aegis;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.luascript.aegis.command.*;
import com.luascript.aegis.database.HibernateService;
import com.luascript.aegis.listener.PlayerConnectionListener;
import com.luascript.aegis.module.AegisModule;
import com.luascript.aegis.service.BanService;
import com.luascript.aegis.service.MessageManager;
import com.luascript.aegis.service.UserService;
import com.luascript.aegis.service.WarnService;
import com.luascript.aegis.util.ComponentUtil;
import com.luascript.aegis.util.Constants;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main plugin class for Aegis moderation system.
 */
@Plugin(
    id = "aegis",
    name = "Aegis",
    version = BuildConstants.VERSION,
    authors = {"Michael Frischbutter"},
    description = "Enterprise-grade moderation system for Velocity"
)
public class Aegis {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    private Injector injector;
    private HibernateService hibernateService;
    private ScheduledExecutorService scheduler;

    @Inject
    public Aegis(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Initializing Aegis v{}", BuildConstants.VERSION);

        try {
            // Create Guice injector
            injector = Guice.createInjector(new AegisModule(this, proxyServer, logger, dataDirectory));

            // Initialize MessageManager and load messages
            MessageManager messageManager = injector.getInstance(MessageManager.class);
            messageManager.loadMessages(dataDirectory);
            ComponentUtil.setMessageManager(messageManager);
            logger.info("Message system initialized");

            // Initialize database
            hibernateService = injector.getInstance(HibernateService.class);
            hibernateService.initialize();
            logger.info("Database connection established");

            // Initialize console user
            initializeConsoleUser();

            // Register event listeners
            registerListeners();

            // Register commands
            registerCommands();

            // Register plugin messaging channels
            registerPluginChannels();

            // Start scheduled tasks
            startScheduledTasks();

            logger.info("Aegis v{} has been enabled successfully!", BuildConstants.VERSION);
            logger.info("Type /aegis for help and information");

        } catch (Exception e) {
            logger.error("Failed to initialize Aegis", e);
            throw new RuntimeException("Aegis initialization failed", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Shutting down Aegis...");

        // Stop scheduled tasks
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Shutdown database
        if (hibernateService != null) {
            hibernateService.shutdown();
        }

        logger.info("Aegis has been disabled");
    }

    /**
     * Initialize the console user in the database.
     */
    private void initializeConsoleUser() {
        UserService userService = injector.getInstance(UserService.class);
        userService.getOrCreateUser(Constants.CONSOLE_UUID, Constants.CONSOLE_NAME)
                .thenAccept(user -> {
                    logger.info("Console user initialized (UUID: {})", Constants.CONSOLE_UUID);
                })
                .exceptionally(e -> {
                    logger.error("Failed to initialize console user", e);
                    return null;
                });
    }

    /**
     * Register event listeners.
     */
    private void registerListeners() {
        PlayerConnectionListener connectionListener = injector.getInstance(PlayerConnectionListener.class);
        proxyServer.getEventManager().register(this, connectionListener);

        logger.info("Event listeners registered");
    }

    /**
     * Register commands.
     */
    private void registerCommands() {
        CommandManager commandManager = proxyServer.getCommandManager();

        // Main command (no permission required)
        CommandMeta aegisMeta = commandManager.metaBuilder("aegis")
                .build();
        commandManager.register(aegisMeta, injector.getInstance(AegisCommand.class));

        // Ban commands
        CommandMeta banMeta = commandManager.metaBuilder("ban")
                .aliases("permban")
                .build();
        commandManager.register(banMeta, injector.getInstance(BanCommand.class));

        CommandMeta tempbanMeta = commandManager.metaBuilder("tempban")
                .aliases("tban")
                .build();
        commandManager.register(tempbanMeta, injector.getInstance(TempbanCommand.class));

        CommandMeta unbanMeta = commandManager.metaBuilder("unban")
                .aliases("pardon")
                .build();
        commandManager.register(unbanMeta, injector.getInstance(UnbanCommand.class));

        CommandMeta baninfoMeta = commandManager.metaBuilder("baninfo")
                .aliases("checkban")
                .build();
        commandManager.register(baninfoMeta, injector.getInstance(BaninfoCommand.class));

        CommandMeta banlistMeta = commandManager.metaBuilder("banlist")
                .aliases("bans")
                .build();
        commandManager.register(banlistMeta, injector.getInstance(BanlistCommand.class));

        // Warn commands
        CommandMeta warnMeta = commandManager.metaBuilder("warn")
                .build();
        commandManager.register(warnMeta, injector.getInstance(WarnCommand.class));

        CommandMeta warnsMeta = commandManager.metaBuilder("warns")
                .aliases("warnings")
                .build();
        commandManager.register(warnsMeta, injector.getInstance(WarnsCommand.class));

        CommandMeta unwarnMeta = commandManager.metaBuilder("unwarn")
                .aliases("removewarn")
                .build();
        commandManager.register(unwarnMeta, injector.getInstance(UnwarnCommand.class));

        CommandMeta clearwarnsMeta = commandManager.metaBuilder("clearwarns")
                .aliases("resetwarns")
                .build();
        commandManager.register(clearwarnsMeta, injector.getInstance(ClearwarnsCommand.class));

        // Kick command
        CommandMeta kickMeta = commandManager.metaBuilder("kick")
                .build();
        commandManager.register(kickMeta, injector.getInstance(KickCommand.class));

        // Reload command
        CommandMeta reloadMeta = commandManager.metaBuilder("aegisreload")
                .aliases("areload")
                .build();
        commandManager.register(reloadMeta, injector.getInstance(ReloadCommand.class));

        logger.info("Commands registered: aegis, ban, tempban, unban, baninfo, banlist, warn, warns, unwarn, clearwarns, kick, aegisreload");
    }

    /**
     * Register plugin messaging channels for backend server communication.
     */
    private void registerPluginChannels() {
        MinecraftChannelIdentifier channel = MinecraftChannelIdentifier.from(Constants.PLUGIN_CHANNEL);
        proxyServer.getChannelRegistrar().register(channel);
        logger.info("Plugin messaging channel registered: {}", Constants.PLUGIN_CHANNEL);
    }

    /**
     * Start scheduled tasks for expired ban/warn cleanup.
     */
    private void startScheduledTasks() {
        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r);
            thread.setName("aegis-scheduler");
            thread.setDaemon(true);
            return thread;
        });

        BanService banService = injector.getInstance(BanService.class);
        WarnService warnService = injector.getInstance(WarnService.class);

        // Check for expired bans every 5 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                banService.processExpiredBans().thenAccept(count -> {
                    if (count > 0) {
                        logger.info("Processed {} expired bans", count);
                    }
                }).exceptionally(e -> {
                    logger.error("Error processing expired bans", e);
                    return null;
                });
            } catch (Exception e) {
                logger.error("Error in ban cleanup task", e);
            }
        }, 1, 5, TimeUnit.MINUTES);

        // Check for expired warnings every 5 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                warnService.processExpiredWarns().thenAccept(count -> {
                    if (count > 0) {
                        logger.info("Processed {} expired warnings", count);
                    }
                }).exceptionally(e -> {
                    logger.error("Error processing expired warnings", e);
                    return null;
                });
            } catch (Exception e) {
                logger.error("Error in warning cleanup task", e);
            }
        }, 1, 5, TimeUnit.MINUTES);

        logger.info("Scheduled tasks started");
    }

    /**
     * Get the Guice injector.
     *
     * @return Injector instance
     */
    public Injector getInjector() {
        return injector;
    }

    /**
     * Get the proxy server instance.
     *
     * @return ProxyServer instance
     */
    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    /**
     * Get the logger instance.
     *
     * @return Logger instance
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Get the data directory path.
     *
     * @return Path to data directory
     */
    public Path getDataDirectory() {
        return dataDirectory;
    }
}
