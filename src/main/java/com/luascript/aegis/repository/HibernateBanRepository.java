package com.luascript.aegis.repository;

import com.luascript.aegis.database.HibernateService;
import com.luascript.aegis.database.entity.Ban;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Hibernate implementation of BanRepository.
 */
@Singleton
public class HibernateBanRepository extends AbstractHibernateRepository<Ban, Long> implements BanRepository {

    @Inject
    public HibernateBanRepository(HibernateService hibernateService) {
        super(hibernateService, Ban.class);
    }

    @Override
    public CompletableFuture<Optional<Ban>> findActiveBanByUuid(UUID uuid) {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM Ban b " +
                    "JOIN FETCH b.player p " +
                    "JOIN FETCH b.issuer i " +
                    "WHERE p.uuid = :uuid " +
                    "AND b.active = true " +
                    "AND (b.expiresAt IS NULL OR b.expiresAt > :now)",
                    Ban.class
            ).setParameter("uuid", uuid.toString())
             .setParameter("now", Instant.now())
             .uniqueResultOptional();
        });
    }

    @Override
    public CompletableFuture<Optional<Ban>> findActiveBanByIp(String ipAddress) {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM Ban b " +
                    "JOIN FETCH b.player p " +
                    "JOIN FETCH b.issuer i " +
                    "WHERE b.ipAddress = :ipAddress " +
                    "AND b.active = true " +
                    "AND (b.expiresAt IS NULL OR b.expiresAt > :now)",
                    Ban.class
            ).setParameter("ipAddress", ipAddress)
             .setParameter("now", Instant.now())
             .uniqueResultOptional();
        });
    }

    @Override
    public CompletableFuture<List<Ban>> findBanHistory(UUID uuid) {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM Ban b " +
                    "JOIN FETCH b.player p " +
                    "JOIN FETCH b.issuer i " +
                    "LEFT JOIN FETCH b.unbannedBy " +
                    "WHERE p.uuid = :uuid " +
                    "ORDER BY b.createdAt DESC",
                    Ban.class
            ).setParameter("uuid", uuid.toString())
             .list();
        });
    }

    @Override
    public CompletableFuture<List<Ban>> findAllActiveBans() {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM Ban b " +
                    "JOIN FETCH b.player p " +
                    "JOIN FETCH b.issuer i " +
                    "WHERE b.active = true " +
                    "AND (b.expiresAt IS NULL OR b.expiresAt > :now) " +
                    "ORDER BY b.createdAt DESC",
                    Ban.class
            ).setParameter("now", Instant.now())
             .list();
        });
    }

    @Override
    public CompletableFuture<List<Ban>> findExpiredBans() {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM Ban b " +
                    "WHERE b.active = true " +
                    "AND b.expiresAt IS NOT NULL " +
                    "AND b.expiresAt <= :now",
                    Ban.class
            ).setParameter("now", Instant.now())
             .list();
        });
    }

    @Override
    public CompletableFuture<Boolean> isPlayerBanned(UUID uuid) {
        return executeQuery(session -> {
            Long count = session.createQuery(
                    "SELECT COUNT(b) FROM Ban b " +
                    "JOIN b.player p " +
                    "WHERE p.uuid = :uuid " +
                    "AND b.active = true " +
                    "AND (b.expiresAt IS NULL OR b.expiresAt > :now)",
                    Long.class
            ).setParameter("uuid", uuid.toString())
             .setParameter("now", Instant.now())
             .uniqueResult();
            return count != null && count > 0;
        });
    }

    @Override
    public CompletableFuture<List<Ban>> findActiveBansPaginated(int page, int pageSize) {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM Ban b " +
                    "JOIN FETCH b.player p " +
                    "JOIN FETCH b.issuer i " +
                    "WHERE b.active = true " +
                    "AND (b.expiresAt IS NULL OR b.expiresAt > :now) " +
                    "ORDER BY b.createdAt DESC",
                    Ban.class
            ).setParameter("now", Instant.now())
             .setFirstResult(page * pageSize)
             .setMaxResults(pageSize)
             .list();
        });
    }
}
