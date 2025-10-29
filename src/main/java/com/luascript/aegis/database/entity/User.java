package com.luascript.aegis.database.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * User entity representing a player in the moderation system.
 * Central entity that links to all moderation actions.
 */
@Entity
@Table(name = "aegis_users", indexes = {
        @Index(name = "idx_users_uuid", columnList = "uuid"),
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_last_ip", columnList = "last_ip")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "username", nullable = false, length = 16)
    private String username;

    @Column(name = "display_name", length = 16)
    private String displayName;

    @Column(name = "first_seen", nullable = false)
    private Long firstSeen;

    @Column(name = "last_seen", nullable = false)
    private Long lastSeen;

    @Column(name = "last_ip", length = 45)
    private String lastIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Bidirectional relationships
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Ban> bans = new ArrayList<>();

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Warn> warns = new ArrayList<>();

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Kick> kicks = new ArrayList<>();

    // Constructors
    public User() {
    }

    public User(UUID uuid, String username) {
        this.uuid = uuid.toString();
        this.username = username;
        long now = System.currentTimeMillis();
        this.firstSeen = now;
        this.lastSeen = now;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getUuid() {
        return UUID.fromString(uuid);
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid.toString();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Long getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(Long firstSeen) {
        this.firstSeen = firstSeen;
    }

    public Long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getLastIp() {
        return lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Ban> getBans() {
        return bans;
    }

    public void setBans(List<Ban> bans) {
        this.bans = bans;
    }

    public List<Warn> getWarns() {
        return warns;
    }

    public void setWarns(List<Warn> warns) {
        this.warns = warns;
    }

    public List<Kick> getKicks() {
        return kicks;
    }

    public void setKicks(List<Kick> kicks) {
        this.kicks = kicks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return uuid != null && uuid.equals(user.uuid);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
