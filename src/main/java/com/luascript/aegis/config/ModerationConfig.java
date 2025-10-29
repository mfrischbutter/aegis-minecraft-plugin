package com.luascript.aegis.config;

/**
 * Configuration holder for moderation settings.
 */
public class ModerationConfig {
    private final boolean warnEscalationEnabled;
    private final int defaultWarnDuration; // in days, 0 for permanent
    private final boolean ipBanEnabled;
    private final boolean silentModeEnabled;
    private final String defaultBanReason;
    private final String defaultKickReason;

    public ModerationConfig(boolean warnEscalationEnabled, int defaultWarnDuration,
                            boolean ipBanEnabled, boolean silentModeEnabled,
                            String defaultBanReason, String defaultKickReason) {
        this.warnEscalationEnabled = warnEscalationEnabled;
        this.defaultWarnDuration = defaultWarnDuration;
        this.ipBanEnabled = ipBanEnabled;
        this.silentModeEnabled = silentModeEnabled;
        this.defaultBanReason = defaultBanReason;
        this.defaultKickReason = defaultKickReason;
    }

    public boolean isWarnEscalationEnabled() {
        return warnEscalationEnabled;
    }

    public int getDefaultWarnDuration() {
        return defaultWarnDuration;
    }

    public boolean isIpBanEnabled() {
        return ipBanEnabled;
    }

    public boolean isSilentModeEnabled() {
        return silentModeEnabled;
    }

    public String getDefaultBanReason() {
        return defaultBanReason;
    }

    public String getDefaultKickReason() {
        return defaultKickReason;
    }
}
