package com.luascript.aegis.repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Base repository interface for all data access operations.
 * All operations return CompletableFuture for asynchronous execution.
 *
 * @param <T> Entity type
 * @param <ID> Primary key type
 */
public interface Repository<T, ID> {

    /**
     * Save or update an entity.
     *
     * @param entity Entity to save
     * @return CompletableFuture with saved entity
     */
    CompletableFuture<T> save(T entity);

    /**
     * Find an entity by its ID.
     *
     * @param id Primary key
     * @return CompletableFuture with Optional containing entity if found
     */
    CompletableFuture<Optional<T>> findById(ID id);

    /**
     * Find all entities.
     *
     * @return CompletableFuture with list of all entities
     */
    CompletableFuture<List<T>> findAll();

    /**
     * Delete an entity.
     *
     * @param entity Entity to delete
     * @return CompletableFuture completing when deletion is done
     */
    CompletableFuture<Void> delete(T entity);

    /**
     * Delete an entity by its ID.
     *
     * @param id Primary key
     * @return CompletableFuture completing when deletion is done
     */
    CompletableFuture<Void> deleteById(ID id);

    /**
     * Check if an entity exists by ID.
     *
     * @param id Primary key
     * @return CompletableFuture with boolean indicating existence
     */
    CompletableFuture<Boolean> existsById(ID id);

    /**
     * Count all entities.
     *
     * @return CompletableFuture with count
     */
    CompletableFuture<Long> count();
}
