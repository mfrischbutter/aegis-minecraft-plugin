package com.luascript.aegis.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.luascript.aegis.Aegis;
import com.luascript.aegis.config.*;
import com.luascript.aegis.database.HibernateService;
import com.luascript.aegis.exception.ConfigurationException;
import com.luascript.aegis.repository.*;
import com.luascript.aegis.service.*;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Main Guice module for Aegis dependency injection configuration.
 */
public class AegisModule extends AbstractModule {

    private final Aegis plugin;
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    public AegisModule(Aegis plugin, ProxyServer proxyServer, Logger logger, Path dataDirectory) {
        this.plugin = plugin;
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Override
    protected void configure() {
        // Bind plugin instance
        bind(Aegis.class).toInstance(plugin);
        bind(ProxyServer.class).toInstance(proxyServer);
        bind(Logger.class).toInstance(logger);

        // Bind repositories
        bind(UserRepository.class).to(HibernateUserRepository.class).in(Singleton.class);
        bind(BanRepository.class).to(HibernateBanRepository.class).in(Singleton.class);
        bind(WarnRepository.class).to(HibernateWarnRepository.class).in(Singleton.class);
        bind(KickRepository.class).to(HibernateKickRepository.class).in(Singleton.class);

        // Bind services
        bind(UserService.class).to(UserServiceImpl.class).in(Singleton.class);
        bind(BanService.class).to(BanServiceImpl.class).in(Singleton.class);
        bind(WarnService.class).to(WarnServiceImpl.class).in(Singleton.class);
        bind(KickService.class).to(KickServiceImpl.class).in(Singleton.class);
        bind(NotificationService.class).to(DiscordNotificationService.class).in(Singleton.class);
        bind(SoundNotificationService.class).to(SoundNotificationServiceImpl.class).in(Singleton.class);

        // Utility services
        bind(CacheService.class).in(Singleton.class);
        bind(MessageService.class).in(Singleton.class);

        // Infrastructure
        bind(HibernateService.class).in(Singleton.class);
    }

    /**
     * Provide ConfigurationManager as a singleton.
     */
    @Provides
    @Singleton
    public ConfigurationManager provideConfigurationManager() {
        ConfigurationManager configManager = new ConfigurationManager(dataDirectory, logger);
        try {
            configManager.load();
        } catch (ConfigurationException e) {
            logger.error("Failed to load configuration", e);
            throw new RuntimeException("Configuration loading failed", e);
        }
        return configManager;
    }

    /**
     * Provide DatabaseConfig from ConfigurationManager.
     */
    @Provides
    @Singleton
    public DatabaseConfig provideDatabaseConfig(ConfigurationManager configManager) {
        return configManager.getDatabaseConfig();
    }

    /**
     * Provide ModerationConfig from ConfigurationManager.
     */
    @Provides
    @Singleton
    public ModerationConfig provideModerationConfig(ConfigurationManager configManager) {
        return configManager.getModerationConfig();
    }

    /**
     * Provide CacheConfig from ConfigurationManager.
     */
    @Provides
    @Singleton
    public CacheConfig provideCacheConfig(ConfigurationManager configManager) {
        return configManager.getCacheConfig();
    }

    /**
     * Provide DiscordConfig from ConfigurationManager.
     */
    @Provides
    @Singleton
    public DiscordConfig provideDiscordConfig(ConfigurationManager configManager) {
        return configManager.getDiscordConfig();
    }

    /**
     * Provide data directory path.
     */
    @Provides
    @Singleton
    public Path provideDataDirectory() {
        return dataDirectory;
    }
}
