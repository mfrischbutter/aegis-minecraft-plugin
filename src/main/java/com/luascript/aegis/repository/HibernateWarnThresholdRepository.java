package com.luascript.aegis.repository;

import com.luascript.aegis.database.HibernateService;
import com.luascript.aegis.database.entity.WarnThreshold;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Hibernate implementation of WarnThresholdRepository.
 */
@Singleton
public class HibernateWarnThresholdRepository extends AbstractHibernateRepository<WarnThreshold, Long> implements WarnThresholdRepository {

    @Inject
    public HibernateWarnThresholdRepository(HibernateService hibernateService) {
        super(hibernateService, WarnThreshold.class);
    }

    @Override
    public CompletableFuture<Optional<WarnThreshold>> findByWarnCount(int warnCount) {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM WarnThreshold wt " +
                    "WHERE wt.warnCount = :warnCount " +
                    "AND wt.enabled = true",
                    WarnThreshold.class
            ).setParameter("warnCount", warnCount)
             .uniqueResultOptional();
        });
    }

    @Override
    public CompletableFuture<List<WarnThreshold>> findAllEnabled() {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM WarnThreshold wt " +
                    "WHERE wt.enabled = true " +
                    "ORDER BY wt.warnCount ASC",
                    WarnThreshold.class
            ).list();
        });
    }

    @Override
    public CompletableFuture<Optional<WarnThreshold>> findNextThreshold(int currentWarnCount) {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM WarnThreshold wt " +
                    "WHERE wt.warnCount > :currentCount " +
                    "AND wt.enabled = true " +
                    "ORDER BY wt.warnCount ASC",
                    WarnThreshold.class
            ).setParameter("currentCount", currentWarnCount)
             .setMaxResults(1)
             .uniqueResultOptional();
        });
    }
}
