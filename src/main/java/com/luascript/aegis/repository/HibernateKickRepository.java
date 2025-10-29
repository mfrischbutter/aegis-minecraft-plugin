package com.luascript.aegis.repository;

import com.luascript.aegis.database.HibernateService;
import com.luascript.aegis.database.entity.Kick;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Hibernate implementation of KickRepository.
 */
@Singleton
public class HibernateKickRepository extends AbstractHibernateRepository<Kick, Long> implements KickRepository {

    @Inject
    public HibernateKickRepository(HibernateService hibernateService) {
        super(hibernateService, Kick.class);
    }

    @Override
    public CompletableFuture<List<Kick>> findKickHistory(UUID uuid) {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM Kick k " +
                    "JOIN FETCH k.player p " +
                    "JOIN FETCH k.issuer i " +
                    "WHERE p.uuid = :uuid " +
                    "ORDER BY k.kickedAt DESC",
                    Kick.class
            ).setParameter("uuid", uuid.toString())
             .list();
        });
    }

    @Override
    public CompletableFuture<List<Kick>> findRecentKicks(UUID uuid, int days) {
        return executeQuery(session -> {
            Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
            return session.createQuery(
                    "FROM Kick k " +
                    "JOIN FETCH k.player p " +
                    "JOIN FETCH k.issuer i " +
                    "WHERE p.uuid = :uuid " +
                    "AND k.kickedAt >= :cutoff " +
                    "ORDER BY k.kickedAt DESC",
                    Kick.class
            ).setParameter("uuid", uuid.toString())
             .setParameter("cutoff", cutoff)
             .list();
        });
    }

    @Override
    public CompletableFuture<Long> countRecentKicks(UUID uuid, int days) {
        return executeQuery(session -> {
            Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
            return session.createQuery(
                    "SELECT COUNT(k) FROM Kick k " +
                    "JOIN k.player p " +
                    "WHERE p.uuid = :uuid " +
                    "AND k.kickedAt >= :cutoff",
                    Long.class
            ).setParameter("uuid", uuid.toString())
             .setParameter("cutoff", cutoff)
             .uniqueResult();
        });
    }
}
