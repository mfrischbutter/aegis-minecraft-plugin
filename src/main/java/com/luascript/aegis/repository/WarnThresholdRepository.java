package com.luascript.aegis.repository;

import com.luascript.aegis.database.entity.WarnThreshold;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for WarnThreshold entity operations.
 */
public interface WarnThresholdRepository extends Repository<WarnThreshold, Long> {

    /**
     * Find a threshold by warn count.
     *
     * @param warnCount Number of warnings
     * @return CompletableFuture with Optional containing threshold if found
     */
    CompletableFuture<Optional<WarnThreshold>> findByWarnCount(int warnCount);

    /**
     * Find all enabled thresholds, ordered by warn count.
     *
     * @return CompletableFuture with list of enabled thresholds
     */
    CompletableFuture<List<WarnThreshold>> findAllEnabled();

    /**
     * Find the next threshold above a given warn count.
     *
     * @param currentWarnCount Current number of warnings
     * @return CompletableFuture with Optional containing next threshold
     */
    CompletableFuture<Optional<WarnThreshold>> findNextThreshold(int currentWarnCount);
}
