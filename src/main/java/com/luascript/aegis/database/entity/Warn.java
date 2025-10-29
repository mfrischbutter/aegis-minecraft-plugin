package com.luascript.aegis.database.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Warn entity representing a player warning.
 * Warnings can escalate to bans/kicks based on thresholds.
 */
@Entity
@Table(name = "aegis_warns", indexes = {
        @Index(name = "idx_warns_player_id", columnList = "player_id"),
        @Index(name = "idx_warns_active", columnList = "active"),
        @Index(name = "idx_warns_player_active", columnList = "player_id, active"),
        @Index(name = "idx_warns_expires_at", columnList = "expires_at")
})
public class Warn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private User player;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_id", nullable = false)
    private User issuer;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "server_name", nullable = false, length = 50)
    private String serverName;

    // Removal tracking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "removed_by_id")
    private User removedBy;

    @Column(name = "removed_at")
    private Instant removedAt;

    @Column(name = "removal_reason", columnDefinition = "TEXT")
    private String removalReason;

    // Constructors
    public Warn() {
    }

    public Warn(User player, String reason, User issuer, String serverName) {
        this.player = player;
        this.reason = reason;
        this.issuer = issuer;
        this.serverName = serverName;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Check if the warning has expired.
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if the warning is currently active (not expired and not removed).
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return active && !isExpired();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getPlayer() {
        return player;
    }

    public void setPlayer(User player) {
        this.player = player;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public User getIssuer() {
        return issuer;
    }

    public void setIssuer(User issuer) {
        this.issuer = issuer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActiveFlag() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public User getRemovedBy() {
        return removedBy;
    }

    public void setRemovedBy(User removedBy) {
        this.removedBy = removedBy;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(Instant removedAt) {
        this.removedAt = removedAt;
    }

    public String getRemovalReason() {
        return removalReason;
    }

    public void setRemovalReason(String removalReason) {
        this.removalReason = removalReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Warn warn)) return false;
        return id != null && id.equals(warn.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Warn{" +
                "id=" + id +
                ", player=" + (player != null ? player.getUsername() : "null") +
                ", active=" + active +
                '}';
    }
}
