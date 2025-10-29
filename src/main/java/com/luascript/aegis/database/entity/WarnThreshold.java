package com.luascript.aegis.database.entity;

import jakarta.persistence.*;

/**
 * WarnThreshold entity for configuring automatic warn escalation.
 * Defines actions to take when a player reaches a certain number of warnings.
 */
@Entity
@Table(name = "aegis_warn_thresholds", indexes = {
        @Index(name = "idx_thresholds_warn_count", columnList = "warn_count"),
        @Index(name = "idx_thresholds_enabled", columnList = "enabled")
})
public class WarnThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warn_count", nullable = false, unique = true)
    private Integer warnCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private ActionType actionType;

    @Column(name = "duration")
    private Long duration; // Duration in seconds for TEMPBAN

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    // Constructors
    public WarnThreshold() {
    }

    public WarnThreshold(Integer warnCount, ActionType actionType, Long duration, String message) {
        this.warnCount = warnCount;
        this.actionType = actionType;
        this.duration = duration;
        this.message = message;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getWarnCount() {
        return warnCount;
    }

    public void setWarnCount(Integer warnCount) {
        this.warnCount = warnCount;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WarnThreshold that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "WarnThreshold{" +
                "id=" + id +
                ", warnCount=" + warnCount +
                ", actionType=" + actionType +
                ", enabled=" + enabled +
                '}';
    }
}
