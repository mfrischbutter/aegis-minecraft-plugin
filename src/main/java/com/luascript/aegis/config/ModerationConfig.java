package com.luascript.aegis.config;

/**
 * Configuration holder for moderation settings.
 */
public class ModerationConfig {
    private final boolean ipBanEnabled;
    private final String defaultBanReason;
    private final String defaultKickReason;
    private final boolean soundsEnabled;
    private final String warningSoundName;
    private final float warningSoundVolume;
    private final float warningSoundPitch;

    public ModerationConfig(boolean ipBanEnabled, String defaultBanReason, String defaultKickReason,
                            boolean soundsEnabled, String warningSoundName,
                            float warningSoundVolume, float warningSoundPitch) {
        this.ipBanEnabled = ipBanEnabled;
        this.defaultBanReason = defaultBanReason;
        this.defaultKickReason = defaultKickReason;
        this.soundsEnabled = soundsEnabled;
        this.warningSoundName = warningSoundName;
        this.warningSoundVolume = warningSoundVolume;
        this.warningSoundPitch = warningSoundPitch;
    }

    public boolean isIpBanEnabled() {
        return ipBanEnabled;
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
