package com.luascript.aegis.service;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.luascript.aegis.config.DiscordConfig;
import com.luascript.aegis.database.entity.Ban;
import com.luascript.aegis.database.entity.BanType;
import com.luascript.aegis.database.entity.Kick;
import com.luascript.aegis.database.entity.Warn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Discord webhook notification service for moderation actions.
 * Sends rich embeds with player information, moderator details, and timestamps.
 */
@Singleton
public class DiscordNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(DiscordNotificationService.class);

    // Severity-based colors
    private static final int COLOR_BAN = 0xFF0000;        // Red - Bans
    private static final int COLOR_UNBAN = 0x00FF00;      // Green - Unbans
    private static final int COLOR_WARN = 0xFF8C00;       // Orange - Warns
    private static final int COLOR_UNWARN = 0x90EE90;     // Light Green - Warn removals
    private static final int COLOR_KICK = 0xFFFF00;       // Yellow - Kicks

    // mc-heads.net API for player heads
    private static final String CRAFATAR_AVATAR_URL = "https://mc-heads.net/avatar/%s";

    // Date formatter for timestamps
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
            .withZone(ZoneId.of("UTC"));

    private final DiscordConfig discordConfig;
    private final WebhookClient webhookClient;

    @Inject
    public DiscordNotificationService(DiscordConfig discordConfig) {
        this.discordConfig = discordConfig;

        // Initialize webhook client if Discord is enabled
        if (discordConfig.isEnabled() && !discordConfig.getWebhookUrl().isEmpty()) {
            this.webhookClient = WebhookClient.withUrl(discordConfig.getWebhookUrl());
            logger.info("Discord notification service initialized with webhook URL");
        } else {
            this.webhookClient = null;
            logger.info("Discord notifications are disabled");
        }
    }

    @Override
    public void notifyBan(Ban ban, int warningCount) {
        if (!shouldNotify(discordConfig.isNotifyBans())) {
            return;
        }

        WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
            .setColor(COLOR_BAN)
            .setTimestamp(Instant.now());

        // Set title with player name and thumbnail
        String playerName = ban.getPlayer().getUsername();
        if (ban.getBanType() == BanType.PERMANENT) {
            embed.setTitle(new WebhookEmbed.EmbedTitle("🚫 " + playerName + " wurde permanent gebannt", null));
        } else {
            embed.setTitle(new WebhookEmbed.EmbedTitle("⏱️ " + playerName + " wurde temporär gebannt", null));
        }

        // Add player head thumbnail
        embed.setThumbnailUrl(getCraftavartUrl(ban.getPlayer().getUuid().toString()));

        // Add fields
        embed.addField(new WebhookEmbed.EmbedField(false, "👤 Spieler",
            String.format("%s\n`%s`", playerName, ban.getPlayer().getUuid().toString())));

        embed.addField(new WebhookEmbed.EmbedField(false, "👮 Moderator",
            ban.getIssuer().getUsername()));

        embed.addField(new WebhookEmbed.EmbedField(false, "📝 Grund",
            ban.getReason()));

        // Add warning count
        embed.addField(new WebhookEmbed.EmbedField(true, "⚠️ Aktive Verwarnungen",
            String.valueOf(warningCount)));

        // Add duration for temporary bans
        if (ban.getBanType() == BanType.TEMPORARY && ban.getExpiresAt() != null) {
            String duration = formatDuration(ban.getCreatedAt(), ban.getExpiresAt());
            embed.addField(new WebhookEmbed.EmbedField(true, "⏳ Dauer", duration));
            embed.addField(new WebhookEmbed.EmbedField(true, "⏰ Läuft ab am",
                TIMESTAMP_FORMATTER.format(ban.getExpiresAt())));
        }

        // Add server name
        embed.addField(new WebhookEmbed.EmbedField(true, "🖥️ Server", ban.getServerName()));

        // Add timestamp
        embed.addField(new WebhookEmbed.EmbedField(true, "🕐 Zeitstempel",
            TIMESTAMP_FORMATTER.format(ban.getCreatedAt())));

        // Set footer
        embed.setFooter(new WebhookEmbed.EmbedFooter("Aegis Moderationssystem", null));

        sendEmbed(embed.build());
    }

    @Override
    public void notifyTempBan(Ban ban, int warningCount) {
        // Use the same method as notifyBan since it handles both types
        notifyBan(ban, warningCount);
    }

    @Override
    public void notifyUnban(String playerName, String playerUuid, String unbannedBy, String reason) {
        if (!shouldNotify(discordConfig.isNotifyBans())) {
            return;
        }

        WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
            .setColor(COLOR_UNBAN)
            .setTitle(new WebhookEmbed.EmbedTitle("✅ " + playerName + " wurde entbannt", null))
            .setThumbnailUrl(getCraftavartUrl(playerUuid))
            .setTimestamp(Instant.now());

        // Add fields
        embed.addField(new WebhookEmbed.EmbedField(false, "👤 Spieler",
            String.format("%s\n`%s`", playerName, playerUuid)));

        embed.addField(new WebhookEmbed.EmbedField(false, "👮 Entbannt von", unbannedBy));

        if (reason != null && !reason.isEmpty()) {
            embed.addField(new WebhookEmbed.EmbedField(false, "📝 Grund", reason));
        }

        embed.addField(new WebhookEmbed.EmbedField(true, "🕐 Zeitstempel",
            TIMESTAMP_FORMATTER.format(Instant.now())));

        // Set footer
        embed.setFooter(new WebhookEmbed.EmbedFooter("Aegis Moderationssystem", null));

        sendEmbed(embed.build());
    }

    @Override
    public void notifyWarn(Warn warn, int warningCount) {
        if (!shouldNotify(discordConfig.isNotifyWarns())) {
            return;
        }

        String playerName = warn.getPlayer().getUsername();

        WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
            .setColor(COLOR_WARN)
            .setTitle(new WebhookEmbed.EmbedTitle("⚠️ " + playerName + " wurde verwarnt", null))
            .setThumbnailUrl(getCraftavartUrl(warn.getPlayer().getUuid().toString()))
            .setTimestamp(Instant.now());

        // Add fields
        embed.addField(new WebhookEmbed.EmbedField(false, "👤 Spieler",
            String.format("%s\n`%s`", playerName, warn.getPlayer().getUuid().toString())));

        embed.addField(new WebhookEmbed.EmbedField(false, "👮 Moderator",
            warn.getIssuer().getUsername()));

        embed.addField(new WebhookEmbed.EmbedField(false, "📝 Grund",
            warn.getReason()));

        // Add warning count
        embed.addField(new WebhookEmbed.EmbedField(true, "⚠️ Aktive Verwarnungen",
            String.valueOf(warningCount)));

        // Add expiration if set
        if (warn.getExpiresAt() != null) {
            embed.addField(new WebhookEmbed.EmbedField(true, "⏰ Läuft ab am",
                TIMESTAMP_FORMATTER.format(warn.getExpiresAt())));
        }

        // Add server name
        embed.addField(new WebhookEmbed.EmbedField(true, "🖥️ Server", warn.getServerName()));

        // Add timestamp
        embed.addField(new WebhookEmbed.EmbedField(true, "🕐 Zeitstempel",
            TIMESTAMP_FORMATTER.format(warn.getCreatedAt())));

        // Add warn ID for reference
        embed.addField(new WebhookEmbed.EmbedField(true, "🔢 Verwarnungs-ID", "#" + warn.getId()));

        // Set footer
        embed.setFooter(new WebhookEmbed.EmbedFooter("Aegis Moderationssystem", null));

        sendEmbed(embed.build());
    }

    @Override
    public void notifyUnwarn(Long warnId, String playerName, String playerUuid, String removedBy, String reason) {
        if (!shouldNotify(discordConfig.isNotifyWarns())) {
            return;
        }

        WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
            .setColor(COLOR_UNWARN)
            .setTitle(new WebhookEmbed.EmbedTitle("🗑️ Verwarnung für " + playerName + " entfernt", null))
            .setThumbnailUrl(getCraftavartUrl(playerUuid))
            .setTimestamp(Instant.now());

        // Add fields
        embed.addField(new WebhookEmbed.EmbedField(false, "👤 Spieler",
            String.format("%s\n`%s`", playerName, playerUuid)));

        embed.addField(new WebhookEmbed.EmbedField(false, "👮 Entfernt von", removedBy));

        embed.addField(new WebhookEmbed.EmbedField(true, "🔢 Verwarnungs-ID", "#" + warnId));

        if (reason != null && !reason.isEmpty()) {
            embed.addField(new WebhookEmbed.EmbedField(false, "📝 Grund", reason));
        }

        embed.addField(new WebhookEmbed.EmbedField(true, "🕐 Zeitstempel",
            TIMESTAMP_FORMATTER.format(Instant.now())));

        // Set footer
        embed.setFooter(new WebhookEmbed.EmbedFooter("Aegis Moderationssystem", null));

        sendEmbed(embed.build());
    }

    @Override
    public void notifyClearWarns(String playerName, String playerUuid, String clearedBy, int count) {
        if (!shouldNotify(discordConfig.isNotifyWarns())) {
            return;
        }

        WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
            .setColor(COLOR_UNWARN)
            .setTitle(new WebhookEmbed.EmbedTitle("🧹 Verwarnungen für " + playerName + " gelöscht", null))
            .setThumbnailUrl(getCraftavartUrl(playerUuid))
            .setTimestamp(Instant.now());

        // Add fields
        embed.addField(new WebhookEmbed.EmbedField(false, "👤 Spieler",
            String.format("%s\n`%s`", playerName, playerUuid)));

        embed.addField(new WebhookEmbed.EmbedField(false, "👮 Gelöscht von", clearedBy));

        embed.addField(new WebhookEmbed.EmbedField(true, "🧹 Verwarnungen gelöscht",
            String.valueOf(count)));

        embed.addField(new WebhookEmbed.EmbedField(true, "🕐 Zeitstempel",
            TIMESTAMP_FORMATTER.format(Instant.now())));

        // Set footer
        embed.setFooter(new WebhookEmbed.EmbedFooter("Aegis Moderationssystem", null));

        sendEmbed(embed.build());
    }

    @Override
    public void notifyKick(Kick kick, int warningCount) {
        if (!shouldNotify(discordConfig.isNotifyKicks())) {
            return;
        }

        String playerName = kick.getPlayer().getUsername();

        WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
            .setColor(COLOR_KICK)
            .setTitle(new WebhookEmbed.EmbedTitle("👢 " + playerName + " wurde gekickt", null))
            .setThumbnailUrl(getCraftavartUrl(kick.getPlayer().getUuid().toString()))
            .setTimestamp(Instant.now());

        // Add fields
        embed.addField(new WebhookEmbed.EmbedField(false, "👤 Spieler",
            String.format("%s\n`%s`", playerName, kick.getPlayer().getUuid().toString())));

        embed.addField(new WebhookEmbed.EmbedField(false, "👮 Moderator",
            kick.getIssuer().getUsername()));

        embed.addField(new WebhookEmbed.EmbedField(false, "📝 Grund",
            kick.getReason()));

        // Add warning count
        embed.addField(new WebhookEmbed.EmbedField(true, "⚠️ Aktive Verwarnungen",
            String.valueOf(warningCount)));

        // Add server name
        embed.addField(new WebhookEmbed.EmbedField(true, "🖥️ Server", kick.getServerName()));

        // Add timestamp
        embed.addField(new WebhookEmbed.EmbedField(true, "🕐 Zeitstempel",
            TIMESTAMP_FORMATTER.format(kick.getKickedAt())));

        // Set footer
        embed.setFooter(new WebhookEmbed.EmbedFooter("Aegis Moderationssystem", null));

        sendEmbed(embed.build());
    }

    /**
     * Check if notifications should be sent based on Discord config.
     *
     * @param specificSetting The specific notification setting (e.g., notifyBans)
     * @return true if notifications should be sent
     */
    private boolean shouldNotify(boolean specificSetting) {
        return discordConfig.isEnabled() && specificSetting && webhookClient != null;
    }

    /**
     * Get Crafatar avatar URL for a player UUID.
     *
     * @param uuid The player UUID
     * @return The Crafatar URL
     */
    private String getCraftavartUrl(String uuid) {
        return String.format(CRAFATAR_AVATAR_URL, uuid);
    }

    /**
     * Format duration between two instants in human-readable German format.
     *
     * @param start Start instant
     * @param end End instant
     * @return Formatted duration string in German
     */
    private String formatDuration(Instant start, Instant end) {
        long seconds = end.getEpochSecond() - start.getEpochSecond();

        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;

        StringBuilder duration = new StringBuilder();

        if (days > 0) {
            duration.append(days).append(days == 1 ? " Tag" : " Tage");
        }
        if (hours > 0) {
            if (duration.length() > 0) duration.append(", ");
            duration.append(hours).append(hours == 1 ? " Stunde" : " Stunden");
        }
        if (minutes > 0 && days == 0) {
            if (duration.length() > 0) duration.append(", ");
            duration.append(minutes).append(minutes == 1 ? " Minute" : " Minuten");
        }

        return duration.length() > 0 ? duration.toString() : "Weniger als eine Minute";
    }

    /**
     * Send an embed to Discord asynchronously with error handling.
     *
     * @param embed The embed to send
     */
    private void sendEmbed(WebhookEmbed embed) {
        if (webhookClient == null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                webhookClient.send(embed).thenAccept(message -> {
                    logger.debug("Successfully sent Discord notification");
                }).exceptionally(throwable -> {
                    logger.error("Failed to send Discord webhook", throwable);
                    return null;
                });
            } catch (Exception e) {
                logger.error("Error sending Discord notification", e);
            }
        });
    }

    /**
     * Close the webhook client on shutdown.
     */
    public void shutdown() {
        if (webhookClient != null) {
            webhookClient.close();
            logger.info("Discord webhook client closed");
        }
    }
}
