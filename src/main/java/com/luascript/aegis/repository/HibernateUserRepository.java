package com.luascript.aegis.repository;

import com.luascript.aegis.database.HibernateService;
import com.luascript.aegis.database.entity.User;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Hibernate implementation of UserRepository.
 */
@Singleton
public class HibernateUserRepository extends AbstractHibernateRepository<User, Long> implements UserRepository {

    @Inject
    public HibernateUserRepository(HibernateService hibernateService) {
        super(hibernateService, User.class);
    }

    @Override
    public CompletableFuture<Optional<User>> findByUuid(UUID uuid) {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM User u WHERE u.uuid = :uuid",
                    User.class
            ).setParameter("uuid", uuid.toString())
                    .uniqueResultOptional();
        });
    }

    @Override
    public CompletableFuture<Optional<User>> findByUsername(String username) {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM User u WHERE LOWER(u.username) = LOWER(:username)",
                    User.class
            ).setParameter("username", username)
                    .uniqueResultOptional();
        });
    }

    @Override
    public CompletableFuture<Optional<User>> findByIpAddress(String ipAddress) {
        return executeQuery(session -> {
            return session.createQuery(
                    "FROM User u WHERE u.lastIp = :ipAddress",
                    User.class
            ).setParameter("ipAddress", ipAddress)
                    .setMaxResults(1)
                    .uniqueResultOptional();
        });
    }

    @Override
    public CompletableFuture<Void> updateLastSeen(UUID uuid, long timestamp) {
        return executeInTransaction(session -> {
            session.createQuery(
                    "UPDATE User u SET u.lastSeen = :timestamp WHERE u.uuid = :uuid"
            ).setParameter("timestamp", timestamp)
                    .setParameter("uuid", uuid.toString())
                    .executeUpdate();
            return null;
        });
    }

    @Override
    public CompletableFuture<Boolean> existsByUuid(UUID uuid) {
        return executeQuery(session -> {
            Long count = session.createQuery(
                    "SELECT COUNT(u) FROM User u WHERE u.uuid = :uuid",
                    Long.class
            ).setParameter("uuid", uuid.toString())
                    .uniqueResult();
            return count != null && count > 0;
        });
    }
}
