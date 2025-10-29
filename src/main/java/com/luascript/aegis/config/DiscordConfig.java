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
    private final boolean notifyMutes;
    private final boolean notifyReports;

    public DiscordConfig(boolean enabled, String webhookUrl, boolean notifyBans,
                         boolean notifyWarns, boolean notifyKicks, boolean notifyMutes,
                         boolean notifyReports) {
        this.enabled = enabled;
        this.webhookUrl = webhookUrl;
        this.notifyBans = notifyBans;
        this.notifyWarns = notifyWarns;
        this.notifyKicks = notifyKicks;
        this.notifyMutes = notifyMutes;
        this.notifyReports = notifyReports;
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

    public boolean isNotifyMutes() {
        return notifyMutes;
    }

    public boolean isNotifyReports() {
        return notifyReports;
    }
}
