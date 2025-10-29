package com.luascript.aegis.repository;

import com.luascript.aegis.database.entity.User;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for User entity operations.
 */
public interface UserRepository extends Repository<User, Long> {

    /**
     * Find a user by their UUID.
     *
     * @param uuid Player UUID
     * @return CompletableFuture with Optional containing user if found
     */
    CompletableFuture<Optional<User>> findByUuid(UUID uuid);

    /**
     * Find a user by their username (case-insensitive).
     *
     * @param username Player username
     * @return CompletableFuture with Optional containing user if found
     */
    CompletableFuture<Optional<User>> findByUsername(String username);

    /**
     * Find a user by their last known IP address.
     *
     * @param ipAddress IP address
     * @return CompletableFuture with Optional containing user if found
     */
    CompletableFuture<Optional<User>> findByIpAddress(String ipAddress);

    /**
     * Update user's last seen timestamp.
     *
     * @param uuid Player UUID
     * @param timestamp Last seen timestamp
     * @return CompletableFuture completing when update is done
     */
    CompletableFuture<Void> updateLastSeen(UUID uuid, long timestamp);

    /**
     * Check if a user exists by UUID.
     *
     * @param uuid Player UUID
     * @return CompletableFuture with boolean indicating existence
     */
    CompletableFuture<Boolean> existsByUuid(UUID uuid);
}
