package com.luascript.aegis.repository;

import com.luascript.aegis.database.HibernateService;
import com.luascript.aegis.database.entity.Warn;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Hibernate implementation of WarnRepository.
 */
@Singleton
public class HibernateWarnRepository extends AbstractHibernateRepository<Warn, Long> implements WarnRepository {

    @Inject
    public HibernateWarnRepository(HibernateService hibernateService) {
        super(hibernateService, Warn.class);
    }

    @Override
    public CompletableFuture<List<Warn>> findActiveWarnsByPlayer(UUID uuid) {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM Warn w " +
                    "JOIN FETCH w.player p " +
                    "JOIN FETCH w.issuer i " +
                    "WHERE p.uuid = :uuid " +
                    "AND w.active = true " +
                    "AND (w.expiresAt IS NULL OR w.expiresAt > :now) " +
                    "ORDER BY w.createdAt DESC",
                    Warn.class
            ).setParameter("uuid", uuid.toString())
             .setParameter("now", Instant.now())
             .list();
        });
    }

    @Override
    public CompletableFuture<Long> countActiveWarns(UUID uuid) {
        return executeQuery(session -> {
            return session.createQuery(
                    "SELECT COUNT(w) FROM Warn w " +
                    "JOIN w.player p " +
                    "WHERE p.uuid = :uuid " +
                    "AND w.active = true " +
                    "AND (w.expiresAt IS NULL OR w.expiresAt > :now)",
                    Long.class
            ).setParameter("uuid", uuid.toString())
             .setParameter("now", Instant.now())
             .uniqueResult();
        });
    }

    @Override
    public CompletableFuture<List<Warn>> findWarnHistory(UUID uuid) {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM Warn w " +
                    "JOIN FETCH w.player p " +
                    "JOIN FETCH w.issuer i " +
                    "LEFT JOIN FETCH w.removedBy " +
                    "WHERE p.uuid = :uuid " +
                    "ORDER BY w.createdAt DESC",
                    Warn.class
            ).setParameter("uuid", uuid.toString())
             .list();
        });
    }

    @Override
    public CompletableFuture<List<Warn>> findExpiredWarns() {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM Warn w " +
                    "WHERE w.active = true " +
                    "AND w.expiresAt IS NOT NULL " +
                    "AND w.expiresAt <= :now",
                    Warn.class
            ).setParameter("now", Instant.now())
             .list();
        });
    }

    @Override
    public CompletableFuture<Void> clearWarns(UUID uuid, UUID removedBy, String reason) {
        return executeInTransaction(session -> {
            session.createQuery(
                    "UPDATE Warn w " +
                    "SET w.active = false, " +
                    "    w.removedAt = :removedAt, " +
                    "    w.removalReason = :reason " +
                    "WHERE w.player.uuid = :uuid " +
                    "AND w.active = true"
            ).setParameter("removedAt", Instant.now())
             .setParameter("reason", reason)
             .setParameter("uuid", uuid.toString())
             .executeUpdate();

            // Note: We can't set removedBy in bulk update easily with HQL
            // This would require a more complex query or fetching entities first
            return null;
        });
    }
}
