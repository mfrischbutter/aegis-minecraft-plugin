package com.luascript.aegis.service;

import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing localized messages from messages.yml.
 * Supports placeholder replacement using {placeholder} syntax.
 */
@Singleton
public class MessageManager {

    private final Logger logger;
    private ConfigurationNode messagesNode;
    private final Map<String, String> messageCache;

    @Inject
    public MessageManager(Logger logger) {
        this.logger = logger;
        this.messageCache = new HashMap<>();
    }

    /**
     * Load messages from the messages.yml file.
     *
     * @param dataDirectory Plugin data directory
     */
    public void loadMessages(Path dataDirectory) {
        try {
            // Ensure data directory exists
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            Path messagesFile = dataDirectory.resolve("messages.yml");

            // Copy default messages.yml if it doesn't exist
            if (!Files.exists(messagesFile)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("messages.yml")) {
                    if (in != null) {
                        Files.copy(in, messagesFile);
                        logger.info("Created default messages.yml file");
                    } else {
                        logger.error("Could not find default messages.yml in resources");
                        return;
                    }
                }
            }

            // Load YAML file
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(messagesFile)
                    .build();

            messagesNode = loader.load();
            logger.info("Loaded messages from messages.yml");

        } catch (IOException e) {
            logger.error("Failed to load messages.yml", e);
        }
    }

    /**
     * Get a message by its key path (e.g., "ban.usage").
     *
     * @param keyPath Dot-separated path to the message (e.g., "ban.usage")
     * @return The message string, or the key itself if not found
     */
    public String getMessage(String keyPath) {
        return getMessage(keyPath, Map.of());
    }

    /**
     * Get a message by its key path with placeholder replacement.
     *
     * @param keyPath      Dot-separated path to the message (e.g., "ban.success")
     * @param placeholders Map of placeholder names to their values
     * @return The formatted message string
     */
    public String getMessage(String keyPath, Map<String, String> placeholders) {
        // Check cache first
        String cacheKey = keyPath + placeholders.toString();
        if (messageCache.containsKey(cacheKey)) {
            return messageCache.get(cacheKey);
        }

        String message = getMessageFromNode(keyPath);

        // Replace placeholders
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            message = message.replace(placeholder, entry.getValue());
        }

        // Cache the result
        messageCache.put(cacheKey, message);

        return message;
    }

    /**
     * Get a message from the configuration node.
     *
     * @param keyPath Dot-separated key path
     * @return The message string, or the key path if not found
     */
    private String getMessageFromNode(String keyPath) {
        if (messagesNode == null) {
            logger.warn("Messages not loaded, returning key path: {}", keyPath);
            return keyPath;
        }

        try {
            String[] keys = keyPath.split("\\.");
            ConfigurationNode node = messagesNode;

            for (String key : keys) {
                node = node.node(key);
            }

            String value = node.getString();
            if (value == null) {
                logger.warn("Message key not found: {}", keyPath);
                return keyPath;
            }

            return value;

        } catch (Exception e) {
            logger.error("Error retrieving message for key: {}", keyPath, e);
            return keyPath;
        }
    }

    /**
     * Create a placeholder map with a single entry.
     *
     * @param key   Placeholder key
     * @param value Placeholder value
     * @return Map containing the placeholder
     */
    public static Map<String, String> placeholder(String key, String value) {
        return Map.of(key, value);
    }

    /**
     * Create a placeholder map with two entries.
     *
     * @param key1   First placeholder key
     * @param value1 First placeholder value
     * @param key2   Second placeholder key
     * @param value2 Second placeholder value
     * @return Map containing the placeholders
     */
    public static Map<String, String> placeholders(String key1, String value1, String key2, String value2) {
        return Map.of(key1, value1, key2, value2);
    }

    /**
     * Create a placeholder map with three entries.
     *
     * @param key1   First placeholder key
     * @param value1 First placeholder value
     * @param key2   Second placeholder key
     * @param value2 Second placeholder value
     * @param key3   Third placeholder key
     * @param value3 Third placeholder value
     * @return Map containing the placeholders
     */
    public static Map<String, String> placeholders(String key1, String value1, String key2, String value2,
                                                     String key3, String value3) {
        return Map.of(key1, value1, key2, value2, key3, value3);
    }

    /**
     * Create a placeholder map with four entries.
     *
     * @param key1   First placeholder key
     * @param value1 First placeholder value
     * @param key2   Second placeholder key
     * @param value2 Second placeholder value
     * @param key3   Third placeholder key
     * @param value3 Third placeholder value
     * @param key4   Fourth placeholder key
     * @param value4 Fourth placeholder value
     * @return Map containing the placeholders
     */
    public static Map<String, String> placeholders(String key1, String value1, String key2, String value2,
                                                     String key3, String value3, String key4, String value4) {
        return Map.of(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    /**
     * Builder for creating placeholder maps with any number of entries.
     *
     * @return New PlaceholderBuilder instance
     */
    public static PlaceholderBuilder builder() {
        return new PlaceholderBuilder();
    }

    /**
     * Builder class for creating placeholder maps.
     */
    public static class PlaceholderBuilder {
        private final Map<String, String> map = new HashMap<>();

        public PlaceholderBuilder add(String key, String value) {
            map.put(key, value);
            return this;
        }

        public Map<String, String> build() {
            return map;
        }
    }

    /**
     * Clear the message cache.
     */
    public void clearCache() {
        messageCache.clear();
        logger.debug("Message cache cleared");
    }
}
