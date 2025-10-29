package com.luascript.aegis.service;

import com.luascript.aegis.database.entity.*;
import com.luascript.aegis.repository.UserRepository;
import com.luascript.aegis.repository.WarnRepository;
import com.luascript.aegis.util.TimeUtil;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of WarnService.
 */
@Singleton
public class WarnServiceImpl implements WarnService {

    private final WarnRepository warnRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final Logger logger;

    @Inject
    public WarnServiceImpl(WarnRepository warnRepository, UserRepository userRepository,
                           NotificationService notificationService, Logger logger) {
        this.warnRepository = warnRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Warn> createWarn(UUID playerUuid, String reason, UUID issuerUuid,
                                               String serverName, Duration duration) {
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

                    // Create warn entity
                    Warn warn = new Warn(player, reason, issuer, serverName);

                    if (duration != null) {
                        warn.setExpiresAt(TimeUtil.getExpirationInstant(duration));
                    }

                    // Save warn
                    return warnRepository.save(warn);
                })
                .thenCompose(warn -> {
                    logger.info("Created warning for {} by {} - Reason: {}",
                            playerUuid, issuerUuid, reason);

                    // Count active warnings (including this new one) and send Discord notification
                    return warnRepository.countActiveWarns(playerUuid)
                        .thenApply(count -> {
                            notificationService.notifyWarn(warn, count.intValue());
                            return warn;
                        });
                });
    }

    @Override
    public CompletableFuture<Boolean> removeWarn(Long warnId, UUID removedBy, String reason) {
        return warnRepository.findByIdWithAssociations(warnId)
                .thenCompose(optionalWarn -> {
                    if (optionalWarn.isEmpty()) {
                        return CompletableFuture.completedFuture(false);
                    }

                    Warn warn = optionalWarn.get();

                    // Fetch the user who is removing the warning
                    return userRepository.findByUuid(removedBy)
                            .thenCompose(issuerOpt -> {
                                User remover = issuerOpt.orElse(null);

                                // Deactivate warning
                                warn.setActive(false);
                                warn.setRemovedBy(remover);
                                warn.setRemovedAt(Instant.now());
                                warn.setRemovalReason(reason);

                                return warnRepository.save(warn);
                            })
                            .thenApply(savedWarn -> {
                                logger.info("Removed warning #{} by {} - Reason: {}",
                                        warnId, removedBy, reason);

                                // Send Discord notification
                                User remover = savedWarn.getRemovedBy();
                                String removerName = remover != null ? remover.getUsername() : "Console";
                                notificationService.notifyUnwarn(
                                    warnId,
                                    savedWarn.getPlayer().getUsername(),
                                    savedWarn.getPlayer().getUuid().toString(),
                                    removerName,
                                    reason
                                );

                                return true;
                            });
                });
    }

    @Override
    public CompletableFuture<Void> clearWarns(UUID playerUuid, UUID removedBy, String reason) {
        // First, count active warns and get player info for notification
        CompletableFuture<Long> countFuture = warnRepository.countActiveWarns(playerUuid);
        CompletableFuture<Optional<User>> playerFuture = userRepository.findByUuid(playerUuid);
        CompletableFuture<Optional<User>> removerFuture = userRepository.findByUuid(removedBy);

        return CompletableFuture.allOf(countFuture, playerFuture, removerFuture)
                .thenCompose(v -> {
                    long count = countFuture.join();
                    User player = playerFuture.join().orElse(null);
                    User remover = removerFuture.join().orElse(null);

                    // Clear warnings
                    return warnRepository.clearWarns(playerUuid, removedBy, reason)
                            .thenAccept(v2 -> {
                                logger.info("Cleared all warnings for {} by {}",
                                        playerUuid, removedBy);

                                // Send Discord notification
                                if (count > 0 && player != null) {
                                    String removerName = remover != null ? remover.getUsername() : "Console";
                                    notificationService.notifyClearWarns(
                                        player.getUsername(),
                                        player.getUuid().toString(),
                                        removerName,
                                        (int) count
                                    );
                                }
                            });
                });
    }

    @Override
    public CompletableFuture<List<Warn>> getActiveWarns(UUID playerUuid) {
        return warnRepository.findActiveWarnsByPlayer(playerUuid);
    }

    @Override
    public CompletableFuture<List<Warn>> getActiveWarnsPaginated(UUID playerUuid, int page, int pageSize) {
        return warnRepository.findActiveWarnsByPlayerPaginated(playerUuid, page, pageSize);
    }

    @Override
    public CompletableFuture<Long> countActiveWarns(UUID playerUuid) {
        return warnRepository.countActiveWarns(playerUuid);
    }

    @Override
    public CompletableFuture<List<Warn>> getWarnHistory(UUID playerUuid) {
        return warnRepository.findWarnHistory(playerUuid);
    }

    @Override
    public CompletableFuture<Integer> processExpiredWarns() {
        return warnRepository.findExpiredWarns()
                .thenCompose(expiredWarns -> {
                    if (expiredWarns.isEmpty()) {
                        return CompletableFuture.completedFuture(0);
                    }

                    logger.info("Processing {} expired warnings", expiredWarns.size());

                    // Deactivate each expired warning
                    List<CompletableFuture<Warn>> futures = expiredWarns.stream()
                            .map(warn -> {
                                warn.setActive(false);
                                return warnRepository.save(warn);
                            })
                            .toList();

                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> expiredWarns.size());
                })
                .thenApply(count -> {
                    if (count > 0) {
                        logger.info("Processed {} expired warnings", count);
                    }
                    return count;
                });
    }
}
