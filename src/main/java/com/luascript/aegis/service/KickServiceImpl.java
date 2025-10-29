package com.luascript.aegis.service;

import com.luascript.aegis.database.entity.Kick;
import com.luascript.aegis.database.entity.User;
import com.luascript.aegis.repository.KickRepository;
import com.luascript.aegis.repository.UserRepository;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of KickService.
 */
@Singleton
public class KickServiceImpl implements KickService {

    private final KickRepository kickRepository;
    private final UserRepository userRepository;
    private final Logger logger;

    @Inject
    public KickServiceImpl(KickRepository kickRepository, UserRepository userRepository, Logger logger) {
        this.kickRepository = kickRepository;
        this.userRepository = userRepository;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Kick> createKick(UUID playerUuid, String reason, UUID issuerUuid, String serverName) {
        // Fetch player and issuer
        CompletableFuture<User> playerFuture = userRepository.findByUuid(playerUuid)
                .thenApply(opt -> opt.orElseThrow(() ->
                        new IllegalArgumentException("Player not found: " + playerUuid)));

        CompletableFuture<User> issuerFuture = userRepository.findByUuid(issuerUuid)
                .thenApply(opt -> opt.orElseThrow(() ->
                        new IllegalArgumentException("Issuer not found: " + issuerUuid)));

        return CompletableFuture.allOf(playerFuture, issuerFuture)
                .thenCompose(v -> {
                    User player = playerFuture.join();
                    User issuer = issuerFuture.join();

                    // Create kick entity
                    Kick kick = new Kick(player, reason, issuer, serverName);

                    // Save kick
                    return kickRepository.save(kick);
                })
                .thenApply(kick -> {
                    logger.info("Created kick for {} by {} - Reason: {}",
                            playerUuid, issuerUuid, reason);
                    return kick;
                });
    }

    @Override
    public CompletableFuture<List<Kick>> getKickHistory(UUID playerUuid) {
        return kickRepository.findKickHistory(playerUuid);
    }

    @Override
    public CompletableFuture<List<Kick>> getRecentKicks(UUID playerUuid, int days) {
        return kickRepository.findRecentKicks(playerUuid, days);
    }

    @Override
    public CompletableFuture<Long> countRecentKicks(UUID playerUuid, int days) {
        return kickRepository.countRecentKicks(playerUuid, days);
    }
}
