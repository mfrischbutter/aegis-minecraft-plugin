package com.luascript.aegis.database.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Kick entity representing a player kick action.
 * Kicks are logged for audit purposes.
 */
@Entity
@Table(name = "aegis_kicks", indexes = {
        @Index(name = "idx_kicks_player_id", columnList = "player_id"),
        @Index(name = "idx_kicks_kicked_at", columnList = "kicked_at")
})
public class Kick {

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

    @Column(name = "kicked_at", nullable = false)
    private Instant kickedAt;

    @Column(name = "server_name", nullable = false, length = 50)
    private String serverName;

    // Constructors
    public Kick() {
    }

    public Kick(User player, String reason, User issuer, String serverName) {
        this.player = player;
        this.reason = reason;
        this.issuer = issuer;
        this.serverName = serverName;
        this.kickedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (kickedAt == null) {
            kickedAt = Instant.now();
        }
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

    public Instant getKickedAt() {
        return kickedAt;
    }

    public void setKickedAt(Instant kickedAt) {
        this.kickedAt = kickedAt;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Kick kick)) return false;
        return id != null && id.equals(kick.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Kick{" +
                "id=" + id +
                ", player=" + (player != null ? player.getUsername() : "null") +
                ", kickedAt=" + kickedAt +
                '}';
    }
}
