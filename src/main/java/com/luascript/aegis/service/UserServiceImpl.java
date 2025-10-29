package com.luascript.aegis.service;

import com.luascript.aegis.database.entity.User;
import com.luascript.aegis.repository.UserRepository;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of UserService.
 */
@Singleton
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CacheService cacheService;
    private final Logger logger;

    @Inject
    public UserServiceImpl(UserRepository userRepository, CacheService cacheService, Logger logger) {
        this.userRepository = userRepository;
        this.cacheService = cacheService;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<User> getOrCreateUser(UUID uuid, String username) {
        // Try cache first
        Optional<User> cachedUser = cacheService.getCachedUser(uuid);
        if (cachedUser.isPresent()) {
            return CompletableFuture.completedFuture(cachedUser.get());
        }

        // Check database
        return userRepository.findByUuid(uuid)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        User user = optionalUser.get();
                        cacheService.cacheUser(user);
                        return CompletableFuture.completedFuture(user);
                    } else {
                        // Create new user
                        User newUser = new User(uuid, username);
                        return userRepository.save(newUser)
                                .thenApply(savedUser -> {
                                    cacheService.cacheUser(savedUser);
                                    logger.info("Created new user: {} ({})", username, uuid);
                                    return savedUser;
                                });
                    }
                });
    }

    @Override
    public CompletableFuture<Optional<User>> findByUuid(UUID uuid) {
        // Try cache first
        Optional<User> cachedUser = cacheService.getCachedUser(uuid);
        if (cachedUser.isPresent()) {
            return CompletableFuture.completedFuture(cachedUser);
        }

        // Query database
        return userRepository.findByUuid(uuid)
                .thenApply(optionalUser -> {
                    optionalUser.ifPresent(cacheService::cacheUser);
                    return optionalUser;
                });
    }

    @Override
    public CompletableFuture<Optional<User>> findByUsername(String username) {
        // Try to get UUID from cache
        Optional<UUID> cachedUuid = cacheService.getCachedUuidFromUsername(username);
        if (cachedUuid.isPresent()) {
            return findByUuid(cachedUuid.get());
        }

        // Query database
        return userRepository.findByUsername(username)
                .thenApply(optionalUser -> {
                    optionalUser.ifPresent(cacheService::cacheUser);
                    return optionalUser;
                });
    }

    @Override
    public CompletableFuture<Void> updateLastSeen(UUID uuid, String ipAddress) {
        long timestamp = System.currentTimeMillis();

        return userRepository.findByUuid(uuid)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        User user = optionalUser.get();
                        user.setLastSeen(timestamp);
                        if (ipAddress != null) {
                            user.setLastIp(ipAddress);
                        }

                        return userRepository.save(user)
                                .thenAccept(savedUser -> {
                                    cacheService.cacheUser(savedUser);
                                    logger.debug("Updated last seen for user: {}", user.getUsername());
                                });
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }
                });
    }

    @Override
    public CompletableFuture<User> updateUsername(UUID uuid, String newUsername) {
        return userRepository.findByUuid(uuid)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        User user = optionalUser.get();
                        String oldUsername = user.getUsername();
                        user.setUsername(newUsername);

                        return userRepository.save(user)
                                .thenApply(savedUser -> {
                                    cacheService.invalidateUserCache(uuid);
                                    cacheService.cacheUser(savedUser);
                                    logger.info("Updated username: {} -> {}", oldUsername, newUsername);
                                    return savedUser;
                                });
                    } else {
                        throw new IllegalArgumentException("User not found: " + uuid);
                    }
                });
    }
}
