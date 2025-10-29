package com.luascript.aegis.service;

import com.luascript.aegis.config.ModerationConfig;
import com.luascript.aegis.database.entity.*;
import com.luascript.aegis.repository.UserRepository;
import com.luascript.aegis.repository.WarnRepository;
import com.luascript.aegis.repository.WarnThresholdRepository;
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
 * Implementation of WarnService with escalation logic.
 */
@Singleton
public class WarnServiceImpl implements WarnService {

    private final WarnRepository warnRepository;
    private final UserRepository userRepository;
    private final WarnThresholdRepository warnThresholdRepository;
    private final BanService banService;
    private final KickService kickService;
    private final ModerationConfig moderationConfig;
    private final Logger logger;

    @Inject
    public WarnServiceImpl(WarnRepository warnRepository, UserRepository userRepository,
                           WarnThresholdRepository warnThresholdRepository,
                           BanService banService, KickService kickService,
                           ModerationConfig moderationConfig, Logger logger) {
        this.warnRepository = warnRepository;
        this.userRepository = userRepository;
        this.warnThresholdRepository = warnThresholdRepository;
        this.banService = banService;
        this.kickService = kickService;
        this.moderationConfig = moderationConfig;
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

                    // Check for escalation if enabled
                    if (moderationConfig.isWarnEscalationEnabled()) {
                        return handleEscalation(warn);
                    } else {
                        return CompletableFuture.completedFuture(warn);
                    }
                });
    }

    /**
     * Handle automatic escalation based on warning count.
     */
    private CompletableFuture<Warn> handleEscalation(Warn warn) {
        UUID playerUuid = warn.getPlayer().getUuid();

        return warnRepository.countActiveWarns(playerUuid)
                .thenCompose(warnCount -> {
                    int count = warnCount.intValue();

                    logger.debug("Player {} now has {} active warnings", playerUuid, count);

                    // Check if there's a threshold for this warn count
                    return warnThresholdRepository.findByWarnCount(count)
                            .thenCompose(thresholdOpt -> {
                                if (thresholdOpt.isEmpty()) {
                                    return CompletableFuture.completedFuture(warn);
                                }

                                WarnThreshold threshold = thresholdOpt.get();
                                return executeEscalationAction(warn, threshold, count);
                            });
                });
    }

    /**
     * Execute the escalation action (kick, tempban, or permban).
     */
    private CompletableFuture<Warn> executeEscalationAction(Warn warn, WarnThreshold threshold, int warnCount) {
        UUID playerUuid = warn.getPlayer().getUuid();
        UUID issuerUuid = warn.getIssuer().getUuid();
        String serverName = warn.getServerName();

        String escalationMessage = threshold.getMessage() != null ?
                threshold.getMessage() :
                "Automatic action due to " + warnCount + " warnings";

        logger.info("Escalating warnings for {} - Action: {} (warn count: {})",
                playerUuid, threshold.getActionType(), warnCount);

        CompletableFuture<Void> actionFuture = switch (threshold.getActionType()) {
            case KICK -> kickService.createKick(playerUuid, escalationMessage, issuerUuid, serverName)
                    .thenApply(k -> (Void) null);

            case TEMPBAN -> {
                Duration duration = threshold.getDuration() != null ?
                        Duration.ofSeconds(threshold.getDuration()) :
                        Duration.ofDays(1); // Default to 1 day

                yield banService.createTemporaryBan(playerUuid, escalationMessage,
                                issuerUuid, duration, serverName, null)
                        .thenApply(b -> (Void) null);
            }

            case PERMBAN -> banService.createPermanentBan(playerUuid, escalationMessage,
                            issuerUuid, serverName, null)
                    .thenApply(b -> (Void) null);
        };

        return actionFuture.thenApply(v -> warn);
    }

    @Override
    public CompletableFuture<Boolean> removeWarn(Long warnId, UUID removedBy, String reason) {
        return warnRepository.findById(warnId)
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
                                return true;
                            });
                });
    }

    @Override
    public CompletableFuture<Void> clearWarns(UUID playerUuid, UUID removedBy, String reason) {
        return warnRepository.clearWarns(playerUuid, removedBy, reason)
                .thenAccept(v -> logger.info("Cleared all warnings for {} by {}",
                        playerUuid, removedBy));
    }

    @Override
    public CompletableFuture<List<Warn>> getActiveWarns(UUID playerUuid) {
        return warnRepository.findActiveWarnsByPlayer(playerUuid);
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
