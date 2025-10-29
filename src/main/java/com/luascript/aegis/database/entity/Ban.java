package com.luascript.aegis.database.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Ban entity representing a player ban (temporary or permanent).
 */
@Entity
@Table(name = "aegis_bans", indexes = {
        @Index(name = "idx_bans_player_id", columnList = "player_id"),
        @Index(name = "idx_bans_active", columnList = "active"),
        @Index(name = "idx_bans_expires_at", columnList = "expires_at"),
        @Index(name = "idx_bans_ip_address", columnList = "ip_address"),
        @Index(name = "idx_bans_active_player", columnList = "active, player_id"),
        @Index(name = "idx_bans_active_expires", columnList = "active, expires_at")
})
public class Ban {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "ban_type", nullable = false, length = 20)
    private BanType banType;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "server_name", nullable = false, length = 50)
    private String serverName;

    // Unban information
    @Column(name = "unban_reason", columnDefinition = "TEXT")
    private String unbanReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unbanned_by_id")
    private User unbannedBy;

    @Column(name = "unbanned_at")
    private Instant unbannedAt;

    // IP ban support
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // Constructors
    public Ban() {
    }

    public Ban(User player, String reason, User issuer, BanType banType, String serverName) {
        this.player = player;
        this.reason = reason;
        this.issuer = issuer;
        this.banType = banType;
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
     * Check if the ban has expired.
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if the ban is currently active (not expired and not removed).
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

    public BanType getBanType() {
        return banType;
    }

    public void setBanType(BanType banType) {
        this.banType = banType;
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

    public String getUnbanReason() {
        return unbanReason;
    }

    public void setUnbanReason(String unbanReason) {
        this.unbanReason = unbanReason;
    }

    public User getUnbannedBy() {
        return unbannedBy;
    }

    public void setUnbannedBy(User unbannedBy) {
        this.unbannedBy = unbannedBy;
    }

    public Instant getUnbannedAt() {
        return unbannedAt;
    }

    public void setUnbannedAt(Instant unbannedAt) {
        this.unbannedAt = unbannedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ban ban)) return false;
        return id != null && id.equals(ban.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Ban{" +
                "id=" + id +
                ", player=" + (player != null ? player.getUsername() : "null") +
                ", banType=" + banType +
                ", active=" + active +
                '}';
    }
}
