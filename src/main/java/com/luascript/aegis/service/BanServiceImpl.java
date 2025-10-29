package com.luascript.aegis.service;

import com.luascript.aegis.database.entity.Ban;
import com.luascript.aegis.database.entity.BanType;
import com.luascript.aegis.database.entity.User;
import com.luascript.aegis.repository.BanRepository;
import com.luascript.aegis.repository.UserRepository;
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
 * Implementation of BanService.
 */
@Singleton
public class BanServiceImpl implements BanService {

    private final BanRepository banRepository;
    private final UserRepository userRepository;
    private final CacheService cacheService;
    private final Logger logger;

    @Inject
    public BanServiceImpl(BanRepository banRepository, UserRepository userRepository,
                          CacheService cacheService, Logger logger) {
        this.banRepository = banRepository;
        this.userRepository = userRepository;
        this.cacheService = cacheService;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Ban> createPermanentBan(UUID playerUuid, String reason, UUID issuerUuid,
                                                       String serverName, String ipAddress) {
        return createBan(playerUuid, reason, issuerUuid, null, serverName, ipAddress, BanType.PERMANENT);
    }

    @Override
    public CompletableFuture<Ban> createTemporaryBan(UUID playerUuid, String reason, UUID issuerUuid,
                                                      Duration duration, String serverName, String ipAddress) {
        return createBan(playerUuid, reason, issuerUuid, duration, serverName, ipAddress, BanType.TEMPORARY);
    }

    private CompletableFuture<Ban> createBan(UUID playerUuid, String reason, UUID issuerUuid,
                                              Duration duration, String serverName, String ipAddress,
                                              BanType banType) {
        // First, fetch both player and issuer
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

                    // Create ban entity
                    Ban ban = new Ban(player, reason, issuer, banType, serverName);

                    if (duration != null) {
                        ban.setExpiresAt(TimeUtil.getExpirationInstant(duration));
                    }

                    if (ipAddress != null) {
                        ban.setIpAddress(ipAddress);
                    }

                    // Save ban
                    return banRepository.save(ban);
                })
                .thenApply(ban -> {
                    // Invalidate cache
                    cacheService.invalidateBanCache(playerUuid);

                    logger.info("Created {} ban for {} by {} - Reason: {}",
                            banType, playerUuid, issuerUuid, reason);

                    return ban;
                });
    }

    @Override
    public CompletableFuture<Boolean> removeBan(UUID playerUuid, UUID removedBy, String reason) {
        return banRepository.findActiveBanByUuid(playerUuid)
                .thenCompose(optionalBan -> {
                    if (optionalBan.isEmpty()) {
                        return CompletableFuture.completedFuture(false);
                    }

                    Ban ban = optionalBan.get();

                    // Fetch the user who is removing the ban
                    return userRepository.findByUuid(removedBy)
                            .thenCompose(issuerOpt -> {
                                User remover = issuerOpt.orElse(null);

                                // Deactivate ban
                                ban.setActive(false);
                                ban.setUnbanReason(reason);
                                ban.setUnbannedBy(remover);
                                ban.setUnbannedAt(Instant.now());

                                return banRepository.save(ban);
                            })
                            .thenApply(savedBan -> {
                                // Invalidate cache
                                cacheService.invalidateBanCache(playerUuid);

                                logger.info("Removed ban for {} by {} - Reason: {}",
                                        playerUuid, removedBy, reason);

                                return true;
                            });
                });
    }

    @Override
    public CompletableFuture<Boolean> isBanned(UUID playerUuid) {
        // Check cache first
        Optional<Boolean> cachedStatus = cacheService.getCachedBanStatus(playerUuid);
        if (cachedStatus.isPresent()) {
            return CompletableFuture.completedFuture(cachedStatus.get());
        }

        // Query database
        return banRepository.isPlayerBanned(playerUuid)
                .thenApply(isBanned -> {
                    cacheService.cacheBanStatus(playerUuid, isBanned);
                    return isBanned;
                });
    }

    @Override
    public CompletableFuture<Boolean> isIpBanned(String ipAddress) {
        return banRepository.findActiveBanByIp(ipAddress)
                .thenApply(Optional::isPresent);
    }

    @Override
    public CompletableFuture<Optional<Ban>> getBanByIpAddress(String ipAddress) {
        return banRepository.findActiveBanByIp(ipAddress);
    }

    @Override
    public CompletableFuture<Optional<Ban>> getActiveBan(UUID playerUuid) {
        return banRepository.findActiveBanByUuid(playerUuid);
    }

    @Override
    public CompletableFuture<List<Ban>> getBanHistory(UUID playerUuid) {
        return banRepository.findBanHistory(playerUuid);
    }

    @Override
    public CompletableFuture<List<Ban>> getActiveBans(int page, int pageSize) {
        return banRepository.findActiveBansPaginated(page, pageSize);
    }

    @Override
    public CompletableFuture<Integer> processExpiredBans() {
        return banRepository.findExpiredBans()
                .thenCompose(expiredBans -> {
                    if (expiredBans.isEmpty()) {
                        return CompletableFuture.completedFuture(0);
                    }

                    logger.info("Processing {} expired bans", expiredBans.size());

                    // Deactivate each expired ban
                    List<CompletableFuture<Ban>> futures = expiredBans.stream()
                            .map(ban -> {
                                ban.setActive(false);
                                return banRepository.save(ban)
                                        .thenApply(savedBan -> {
                                            cacheService.invalidateBanCache(ban.getPlayer().getUuid());
                                            return savedBan;
                                        });
                            })
                            .toList();

                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> expiredBans.size());
                })
                .thenApply(count -> {
                    if (count > 0) {
                        logger.info("Processed {} expired bans", count);
                    }
                    return count;
                });
    }
}
