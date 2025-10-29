package com.luascript.aegis.config;

/**
 * Configuration holder for moderation settings.
 */
public class ModerationConfig {
    private final int defaultWarnDuration; // in days, 0 for permanent
    private final boolean ipBanEnabled;
    private final boolean silentModeEnabled;
    private final String defaultBanReason;
    private final String defaultKickReason;
    private final boolean soundsEnabled;
    private final String warningSoundName;
    private final float warningSoundVolume;
    private final float warningSoundPitch;

    public ModerationConfig(int defaultWarnDuration, boolean ipBanEnabled,
                            boolean silentModeEnabled, String defaultBanReason, String defaultKickReason,
                            boolean soundsEnabled, String warningSoundName,
                            float warningSoundVolume, float warningSoundPitch) {
        this.defaultWarnDuration = defaultWarnDuration;
        this.ipBanEnabled = ipBanEnabled;
        this.silentModeEnabled = silentModeEnabled;
        this.defaultBanReason = defaultBanReason;
        this.defaultKickReason = defaultKickReason;
        this.soundsEnabled = soundsEnabled;
        this.warningSoundName = warningSoundName;
        this.warningSoundVolume = warningSoundVolume;
        this.warningSoundPitch = warningSoundPitch;
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

    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    public String getWarningSoundName() {
        return warningSoundName;
    }

    public float getWarningSoundVolume() {
        return warningSoundVolume;
    }

    public float getWarningSoundPitch() {
        return warningSoundPitch;
    }
}
