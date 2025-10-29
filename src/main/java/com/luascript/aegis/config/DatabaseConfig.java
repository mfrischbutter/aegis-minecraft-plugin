package com.luascript.aegis.config;

/**
 * Configuration holder for database settings.
 */
public class DatabaseConfig {
    private final String type;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int poolSize;
    private final int minimumIdle;
    private final long connectionTimeout;
    private final long idleTimeout;
    private final long maxLifetime;

    public DatabaseConfig(String type, String host, int port, String database,
                          String username, String password, int poolSize,
                          int minimumIdle, long connectionTimeout, long idleTimeout,
                          long maxLifetime) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
        this.minimumIdle = minimumIdle;
        this.connectionTimeout = connectionTimeout;
        this.idleTimeout = idleTimeout;
        this.maxLifetime = maxLifetime;
    }

    public String getType() {
        return type;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getMinimumIdle() {
        return minimumIdle;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public long getMaxLifetime() {
        return maxLifetime;
    }

    /**
     * Get JDBC URL based on database type.
     */
    public String getJdbcUrl() {
        return switch (type.toLowerCase()) {
            case "mysql" -> String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true", host, port, database);
            case "mariadb" -> String.format("jdbc:mariadb://%s:%d/%s", host, port, database);
            case "postgresql" -> String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            case "h2" -> String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MODE=MySQL", database);
            default -> throw new IllegalArgumentException("Unsupported database type: " + type);
        };
    }

    /**
     * Get JDBC driver class name.
     */
    public String getDriverClassName() {
        return switch (type.toLowerCase()) {
            case "mysql" -> "com.mysql.cj.jdbc.Driver";
            case "mariadb" -> "org.mariadb.jdbc.Driver";
            case "postgresql" -> "org.postgresql.Driver";
            case "h2" -> "org.h2.Driver";
            default -> throw new IllegalArgumentException("Unsupported database type: " + type);
        };
    }

    /**
     * Get Hibernate dialect.
     */
    public String getHibernateDialect() {
        return switch (type.toLowerCase()) {
            case "mysql" -> "org.hibernate.dialect.MySQLDialect";
            case "mariadb" -> "org.hibernate.dialect.MariaDBDialect";
            case "postgresql" -> "org.hibernate.dialect.PostgreSQLDialect";
            case "h2" -> "org.hibernate.dialect.H2Dialect";
            default -> throw new IllegalArgumentException("Unsupported database type: " + type);
        };
    }
}
