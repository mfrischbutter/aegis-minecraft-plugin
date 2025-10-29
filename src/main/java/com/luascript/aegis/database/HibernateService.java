package com.luascript.aegis.database;

import com.luascript.aegis.config.DatabaseConfig;
import com.luascript.aegis.exception.DatabaseException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for managing Hibernate SessionFactory and database connections.
 * Uses HikariCP for connection pooling.
 */
@Singleton
public class HibernateService {
    private final DatabaseConfig databaseConfig;
    private final Logger logger;
    private final ExecutorService executorService;

    private SessionFactory sessionFactory;
    private HikariDataSource dataSource;

    @Inject
    public HibernateService(DatabaseConfig databaseConfig, Logger logger) {
        this.databaseConfig = databaseConfig;
        this.logger = logger;
        this.executorService = Executors.newFixedThreadPool(
                databaseConfig.getPoolSize(),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("aegis-db-worker");
                    thread.setDaemon(true);
                    return thread;
                }
        );
    }

    /**
     * Initialize Hibernate SessionFactory with HikariCP connection pool.
     */
    public void initialize() throws DatabaseException {
        try {
            logger.info("Initializing database connection...");

            // Configure HikariCP
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(databaseConfig.getJdbcUrl());
            hikariConfig.setDriverClassName(databaseConfig.getDriverClassName());
            hikariConfig.setUsername(databaseConfig.getUsername());
            hikariConfig.setPassword(databaseConfig.getPassword());
            hikariConfig.setMaximumPoolSize(databaseConfig.getPoolSize());
            hikariConfig.setMinimumIdle(databaseConfig.getMinimumIdle());
            hikariConfig.setConnectionTimeout(databaseConfig.getConnectionTimeout());
            hikariConfig.setIdleTimeout(databaseConfig.getIdleTimeout());
            hikariConfig.setMaxLifetime(databaseConfig.getMaxLifetime());
            hikariConfig.setPoolName("AegisHikariPool");

            // Connection test
            hikariConfig.setConnectionTestQuery("SELECT 1");

            dataSource = new HikariDataSource(hikariConfig);

            // Configure Hibernate
            Configuration configuration = new Configuration();

            // Hibernate properties
            Properties properties = new Properties();
            properties.put(Environment.DIALECT, databaseConfig.getHibernateDialect());
            properties.put(Environment.DATASOURCE, dataSource);
            properties.put(Environment.SHOW_SQL, false);
            properties.put(Environment.FORMAT_SQL, true);
            properties.put(Environment.HBM2DDL_AUTO, "update"); // TODO: Use Flyway for production
            properties.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
            // Disable Hibernate second-level cache (we use Caffeine cache at application level)
            properties.put(Environment.USE_SECOND_LEVEL_CACHE, false);
            properties.put(Environment.USE_QUERY_CACHE, false);
            // ByteBuddy auto-detects via ServiceLoader (dependency + mergeServiceFiles in build.gradle)
            properties.put(Environment.STATEMENT_BATCH_SIZE, 20);

            configuration.setProperties(properties);

            // Register entities
            configuration.addAnnotatedClass(com.luascript.aegis.database.entity.User.class);
            configuration.addAnnotatedClass(com.luascript.aegis.database.entity.Ban.class);
            configuration.addAnnotatedClass(com.luascript.aegis.database.entity.Warn.class);
            configuration.addAnnotatedClass(com.luascript.aegis.database.entity.Kick.class);

            // Build SessionFactory
            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties())
                    .build();

            sessionFactory = configuration.buildSessionFactory(serviceRegistry);

            logger.info("Database connection established successfully");
            logger.info("Using {} database at {}:{}",
                    databaseConfig.getType(),
                    databaseConfig.getHost(),
                    databaseConfig.getPort());

        } catch (Exception e) {
            throw new DatabaseException("Failed to initialize database connection", e);
        }
    }

    /**
     * Shutdown Hibernate and close connections.
     */
    public void shutdown() {
        logger.info("Shutting down database connection...");

        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }

        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }

        executorService.shutdown();

        logger.info("Database connection closed");
    }

    /**
     * Get the SessionFactory for database operations.
     *
     * @return SessionFactory instance
     */
    public SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            throw new DatabaseException("SessionFactory is not initialized or has been closed");
        }
        return sessionFactory;
    }

    /**
     * Get the executor service for async database operations.
     *
     * @return ExecutorService instance
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }
}
