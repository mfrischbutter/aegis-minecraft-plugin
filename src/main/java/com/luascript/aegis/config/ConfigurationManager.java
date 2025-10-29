package com.luascript.aegis.config;

import com.luascript.aegis.exception.ConfigurationException;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Main configuration manager for Aegis.
 * Uses Configurate (HOCON format) for configuration management.
 */
public class ConfigurationManager {
    private final Path dataDirectory;
    private final Logger logger;
    private CommentedConfigurationNode rootNode;

    private DatabaseConfig databaseConfig;
    private ModerationConfig moderationConfig;
    private CacheConfig cacheConfig;
    private DiscordConfig discordConfig;

    public ConfigurationManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    /**
     * Load configuration from file. Creates default if doesn't exist.
     */
    public void load() throws ConfigurationException {
        try {
            // Create data directory if it doesn't exist
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            Path configPath = dataDirectory.resolve("config.conf");

            // Copy default config if it doesn't exist
            if (!Files.exists(configPath)) {
                logger.info("Creating default configuration file...");
                try (InputStream in = getClass().getResourceAsStream("/config.conf")) {
                    if (in != null) {
                        Files.copy(in, configPath);
                    } else {
                        createDefaultConfig(configPath);
                    }
                }
            }

            // Load configuration
            HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                    .path(configPath)
                    .build();

            rootNode = loader.load();

            // Parse configuration sections
            loadDatabaseConfig();
            loadModerationConfig();
            loadCacheConfig();
            loadDiscordConfig();

            logger.info("Configuration loaded successfully");

        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration", e);
        }
    }

    private void loadDatabaseConfig() {
        CommentedConfigurationNode dbNode = rootNode.node("database");

        databaseConfig = new DatabaseConfig(
                dbNode.node("type").getString("mysql"),
                dbNode.node("host").getString("localhost"),
                dbNode.node("port").getInt(3306),
                dbNode.node("database").getString("aegis"),
                dbNode.node("username").getString("root"),
                dbNode.node("password").getString("password"),
                dbNode.node("pool-size").getInt(10),
                dbNode.node("minimum-idle").getInt(5),
                dbNode.node("connection-timeout").getLong(30000),
                dbNode.node("idle-timeout").getLong(600000),
                dbNode.node("max-lifetime").getLong(1800000)
        );
    }

    private void loadModerationConfig() {
        CommentedConfigurationNode modNode = rootNode.node("moderation");
        CommentedConfigurationNode soundsNode = rootNode.node("sounds");

        moderationConfig = new ModerationConfig(
                modNode.node("default-warn-duration-days").getInt(30),
                modNode.node("ip-ban-enabled").getBoolean(false),
                modNode.node("silent-mode").getBoolean(false),
                modNode.node("default-ban-reason").getString("Banned by an administrator"),
                modNode.node("default-kick-reason").getString("Kicked by an administrator"),
                soundsNode.node("enabled").getBoolean(true),
                soundsNode.node("warning", "sound").getString("ENTITY_VILLAGER_NO"),
                soundsNode.node("warning", "volume").getFloat(1.0f),
                soundsNode.node("warning", "pitch").getFloat(0.8f)
        );
    }

    private void loadCacheConfig() {
        CommentedConfigurationNode cacheNode = rootNode.node("cache");

        cacheConfig = new CacheConfig(
                cacheNode.node("enabled").getBoolean(true),
                cacheNode.node("ban-cache-ttl-minutes").getInt(5),
                cacheNode.node("user-cache-ttl-minutes").getInt(10),
                cacheNode.node("max-cache-size").getInt(1000)
        );
    }

    private void loadDiscordConfig() {
        CommentedConfigurationNode discordNode = rootNode.node("discord");

        discordConfig = new DiscordConfig(
                discordNode.node("enabled").getBoolean(false),
                discordNode.node("webhook-url").getString(""),
                discordNode.node("notify-bans").getBoolean(true),
                discordNode.node("notify-warns").getBoolean(true),
                discordNode.node("notify-kicks").getBoolean(true),
                discordNode.node("notify-mutes").getBoolean(true),
                discordNode.node("notify-reports").getBoolean(true)
        );
    }

    private void createDefaultConfig(Path configPath) throws IOException {
        // Create a basic default configuration programmatically
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .path(configPath)
                .build();

        CommentedConfigurationNode node = loader.createNode();

        // Database section
        node.node("database", "type").set("mysql");
        node.node("database", "host").set("localhost");
        node.node("database", "port").set(3306);
        node.node("database", "database").set("aegis");
        node.node("database", "username").set("root");
        node.node("database", "password").set("password");
        node.node("database", "pool-size").set(10);
        node.node("database", "minimum-idle").set(5);
        node.node("database", "connection-timeout").set(30000);
        node.node("database", "idle-timeout").set(600000);
        node.node("database", "max-lifetime").set(1800000);

        // Moderation section
        node.node("moderation", "warn-escalation-enabled").set(true);
        node.node("moderation", "default-warn-duration-days").set(30);
        node.node("moderation", "ip-ban-enabled").set(false);
        node.node("moderation", "silent-mode").set(false);
        node.node("moderation", "default-ban-reason").set("Banned by an administrator");
        node.node("moderation", "default-kick-reason").set("Kicked by an administrator");

        // Cache section
        node.node("cache", "enabled").set(true);
        node.node("cache", "ban-cache-ttl-minutes").set(5);
        node.node("cache", "user-cache-ttl-minutes").set(10);
        node.node("cache", "max-cache-size").set(1000);

        // Discord section
        node.node("discord", "enabled").set(false);
        node.node("discord", "webhook-url").set("");
        node.node("discord", "notify-bans").set(true);
        node.node("discord", "notify-warns").set(true);
        node.node("discord", "notify-kicks").set(true);
        node.node("discord", "notify-mutes").set(true);
        node.node("discord", "notify-reports").set(true);

        // Sounds section
        node.node("sounds", "enabled").set(true);
        node.node("sounds", "warning", "sound").set("ENTITY_VILLAGER_NO");
        node.node("sounds", "warning", "volume").set(1.0f);
        node.node("sounds", "warning", "pitch").set(0.8f);

        loader.save(node);
    }

    /**
     * Reload configuration from disk.
     */
    public void reload() throws ConfigurationException {
        load();
    }

    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    public ModerationConfig getModerationConfig() {
        return moderationConfig;
    }

    public CacheConfig getCacheConfig() {
        return cacheConfig;
    }

    public DiscordConfig getDiscordConfig() {
        return discordConfig;
    }
}
