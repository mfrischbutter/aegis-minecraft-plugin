package com.luascript.aegis.repository;

import com.luascript.aegis.database.HibernateService;
import com.luascript.aegis.exception.DatabaseException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base repository providing common CRUD operations using Hibernate.
 * All operations are executed asynchronously using CompletableFuture.
 *
 * @param <T> Entity type
 * @param <ID> Primary key type
 */
public abstract class AbstractHibernateRepository<T, ID> implements Repository<T, ID> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final HibernateService hibernateService;
    protected final Class<T> entityClass;

    protected AbstractHibernateRepository(HibernateService hibernateService, Class<T> entityClass) {
        this.hibernateService = hibernateService;
        this.entityClass = entityClass;
    }

    @Override
    public CompletableFuture<T> save(T entity) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = hibernateService.getSessionFactory().openSession()) {
                transaction = session.beginTransaction();
                session.saveOrUpdate(entity);
                transaction.commit();
                return entity;
            } catch (Exception e) {
                if (transaction != null) {
                    transaction.rollback();
                }
                logger.error("Failed to save entity", e);
                throw new DatabaseException("Failed to save entity", e);
            }
        }, hibernateService.getExecutorService());
    }

    @Override
    public CompletableFuture<Optional<T>> findById(ID id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = hibernateService.getSessionFactory().openSession()) {
                T entity = session.get(entityClass, id);
                return Optional.ofNullable(entity);
            } catch (Exception e) {
                logger.error("Failed to find entity by ID: {}", id, e);
                throw new DatabaseException("Failed to find entity by ID", e);
            }
        }, hibernateService.getExecutorService());
    }

    @Override
    public CompletableFuture<List<T>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = hibernateService.getSessionFactory().openSession()) {
                return session.createQuery("FROM " + entityClass.getSimpleName(), entityClass)
                        .list();
            } catch (Exception e) {
                logger.error("Failed to find all entities", e);
                throw new DatabaseException("Failed to find all entities", e);
            }
        }, hibernateService.getExecutorService());
    }

    @Override
    public CompletableFuture<Void> delete(T entity) {
        return CompletableFuture.runAsync(() -> {
            Transaction transaction = null;
            try (Session session = hibernateService.getSessionFactory().openSession()) {
                transaction = session.beginTransaction();
                session.delete(entity);
                transaction.commit();
            } catch (Exception e) {
                if (transaction != null) {
                    transaction.rollback();
                }
                logger.error("Failed to delete entity", e);
                throw new DatabaseException("Failed to delete entity", e);
            }
        }, hibernateService.getExecutorService());
    }

    @Override
    public CompletableFuture<Void> deleteById(ID id) {
        return findById(id)
                .thenCompose(optional -> {
                    if (optional.isPresent()) {
                        return delete(optional.get());
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> existsById(ID id) {
        return findById(id)
                .thenApply(Optional::isPresent);
    }

    @Override
    public CompletableFuture<Long> count() {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = hibernateService.getSessionFactory().openSession()) {
                return session.createQuery(
                        "SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e",
                        Long.class
                ).uniqueResult();
            } catch (Exception e) {
                logger.error("Failed to count entities", e);
                throw new DatabaseException("Failed to count entities", e);
            }
        }, hibernateService.getExecutorService());
    }

    /**
     * Execute a query within a transaction.
     *
     * @param operation Database operation to execute
     * @param <R> Return type
     * @return CompletableFuture with result
     */
    protected <R> CompletableFuture<R> executeInTransaction(DatabaseOperation<R> operation) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = hibernateService.getSessionFactory().openSession()) {
                transaction = session.beginTransaction();
                R result = operation.execute(session);
                transaction.commit();
                return result;
            } catch (Exception e) {
                if (transaction != null) {
                    transaction.rollback();
                }
                logger.error("Failed to execute database operation", e);
                throw new DatabaseException("Failed to execute database operation", e);
            }
        }, hibernateService.getExecutorService());
    }

    /**
     * Execute a query without a transaction (read-only).
     *
     * @param operation Database operation to execute
     * @param <R> Return type
     * @return CompletableFuture with result
     */
    protected <R> CompletableFuture<R> executeQuery(DatabaseOperation<R> operation) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = hibernateService.getSessionFactory().openSession()) {
                return operation.execute(session);
            } catch (Exception e) {
                logger.error("Failed to execute database query", e);
                throw new DatabaseException("Failed to execute database query", e);
            }
        }, hibernateService.getExecutorService());
    }

    /**
     * Functional interface for database operations.
     *
     * @param <R> Return type
     */
    @FunctionalInterface
    protected interface DatabaseOperation<R> {
        R execute(Session session);
    }
}
