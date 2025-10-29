package com.luascript.aegis.listener;

import com.luascript.aegis.database.entity.Ban;
import com.luascript.aegis.service.BanService;
import com.luascript.aegis.service.UserService;
import com.luascript.aegis.util.ComponentUtil;
import com.luascript.aegis.util.TimeUtil;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Event listener for player connection events.
 * Handles ban checking, user data updates, and player tracking.
 */
@Singleton
public class PlayerConnectionListener {

    private final BanService banService;
    private final UserService userService;
    private final Logger logger;

    @Inject
    public PlayerConnectionListener(BanService banService, UserService userService, Logger logger) {
        this.banService = banService;
        this.userService = userService;
        this.logger = logger;
    }

    /**
     * Check if the player is banned when they attempt to login.
     * This runs early in the connection process.
     */
    @Subscribe(order = PostOrder.FIRST)
    public EventTask onLogin(LoginEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        String username = event.getPlayer().getUsername();
        String ipAddress = event.getPlayer().getRemoteAddress().getAddress().getHostAddress();

        logger.debug("Player {} ({}) attempting to login from {}", username, playerUuid, ipAddress);

        // Create a combined future that checks both UUID and IP bans
        CompletableFuture<Void> banCheckFuture = banService.isBanned(playerUuid)
            .thenCompose(isBanned -> {
                if (isBanned) {
                    // Get ban details and deny login
                    return banService.getActiveBan(playerUuid)
                        .thenAccept(banOpt -> {
                            if (banOpt.isPresent()) {
                                Ban ban = banOpt.get();

                                // Format ban message
                                String expiration = ban.getExpiresAt() != null ?
                                        TimeUtil.formatInstant(ban.getExpiresAt()) :
                                        "Never (Permanent)";

                                Component banMessage = ComponentUtil.banMessage(
                                        ban.getReason(),
                                        expiration,
                                        ban.getId()
                                );

                                // Deny login
                                event.setResult(ResultedEvent.ComponentResult.denied(banMessage));

                                logger.info("Denied login for banned player {} ({}): {}",
                                        username, playerUuid, ban.getReason());
                            }
                        });
                } else {
                    // No UUID ban, check IP ban
                    return banService.isIpBanned(ipAddress)
                        .thenCompose(isIpBanned -> {
                            if (isIpBanned) {
                                // Get any ban with this IP address to show generic ban message
                                return banService.getBanByIpAddress(ipAddress)
                                    .thenAccept(ipBanOpt -> {
                                        if (ipBanOpt.isPresent()) {
                                            // Show normal ban message without revealing it's an IP ban
                                            var ban = ipBanOpt.get();
                                            String expiration = ban.getExpiresAt() != null ?
                                                    TimeUtil.formatInstant(ban.getExpiresAt()) :
                                                    "Permanent";

                                            Component banMessage = ComponentUtil.banMessage(
                                                    ban.getReason(),
                                                    expiration,
                                                    ban.getId()
                                            );

                                            event.setResult(ResultedEvent.ComponentResult.denied(banMessage));

                                            logger.info("Denied login for player {} from banned IP: {}",
                                                    username, ipAddress);
                                        }
                                    });
                            } else {
                                // No bans found, allow login
                                return CompletableFuture.completedFuture(null);
                            }
                        });
                }
            })
            .exceptionally(e -> {
                logger.error("Error checking ban status for {} from {}", playerUuid, ipAddress, e);
                // In case of error, allow login (fail-open for availability)
                return null;
            });

        // Tell Velocity to wait for the ban check to complete before allowing the connection
        return EventTask.resumeWhenComplete(banCheckFuture);
    }

    /**
     * Update user data when player successfully connects to a server.
     */
    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        String username = event.getPlayer().getUsername();
        String ipAddress = event.getPlayer().getRemoteAddress().getAddress().getHostAddress();

        // Create or update user record
        userService.getOrCreateUser(playerUuid, username)
                .thenCompose(user -> {
                    // Update last seen and IP
                    return userService.updateLastSeen(playerUuid, ipAddress);
                })
                .thenRun(() -> {
                    logger.debug("Updated user data for {}", username);
                })
                .exceptionally(e -> {
                    logger.error("Error updating user data for {}", username, e);
                    return null;
                });
    }
}
