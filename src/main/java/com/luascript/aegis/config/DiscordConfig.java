package com.luascript.aegis.config;

/**
 * Configuration holder for Discord integration settings.
 */
public class DiscordConfig {
    private final boolean enabled;
    private final String webhookUrl;
    private final boolean notifyBans;
    private final boolean notifyWarns;
    private final boolean notifyKicks;

    public DiscordConfig(boolean enabled, String webhookUrl, boolean notifyBans,
                         boolean notifyWarns, boolean notifyKicks) {
        this.enabled = enabled;
        this.webhookUrl = webhookUrl;
        this.notifyBans = notifyBans;
        this.notifyWarns = notifyWarns;
        this.notifyKicks = notifyKicks;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public boolean isNotifyBans() {
        return notifyBans;
    }

    public boolean isNotifyWarns() {
        return notifyWarns;
    }

    public boolean isNotifyKicks() {
        return notifyKicks;
    }
}
