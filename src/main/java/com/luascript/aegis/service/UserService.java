package com.luascript.aegis.service;

import com.luascript.aegis.database.entity.User;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for user management operations.
 */
public interface UserService {

    /**
     * Get or create a user by UUID.
     * If the user doesn't exist, creates a new one.
     *
     * @param uuid Player UUID
     * @param username Player username
     * @return CompletableFuture with User
     */
    CompletableFuture<User> getOrCreateUser(UUID uuid, String username);

    /**
     * Find a user by UUID.
     *
     * @param uuid Player UUID
     * @return CompletableFuture with Optional containing user if found
     */
    CompletableFuture<Optional<User>> findByUuid(UUID uuid);

    /**
     * Find a user by username.
     *
     * @param username Player username
     * @return CompletableFuture with Optional containing user if found
     */
    CompletableFuture<Optional<User>> findByUsername(String username);

    /**
     * Update user's last seen timestamp and IP address.
     *
     * @param uuid Player UUID
     * @param ipAddress Current IP address
     * @return CompletableFuture completing when update is done
     */
    CompletableFuture<Void> updateLastSeen(UUID uuid, String ipAddress);

    /**
     * Update user's username (in case of name change).
     *
     * @param uuid Player UUID
     * @param newUsername New username
     * @return CompletableFuture with updated User
     */
    CompletableFuture<User> updateUsername(UUID uuid, String newUsername);
}
