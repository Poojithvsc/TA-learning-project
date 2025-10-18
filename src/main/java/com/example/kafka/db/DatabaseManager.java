package com.example.kafka.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DatabaseManager handles PostgreSQL database connections using HikariCP connection pool.
 * This class implements the Singleton pattern to ensure only one connection pool exists.
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    private HikariDataSource dataSource;

    private DatabaseManager() {
        initializeDataSource();
    }

    /**
     * Get the singleton instance of DatabaseManager
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initialize HikariCP data source with configuration from application.properties
     */
    private void initializeDataSource() {
        try {
            Properties props = loadProperties();

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(props.getProperty("db.url"));
            config.setUsername(props.getProperty("db.username"));
            config.setPassword(props.getProperty("db.password"));
            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.size", "10")));
            config.setConnectionTimeout(Long.parseLong(props.getProperty("db.connection.timeout", "30000")));

            // Additional recommended settings
            config.setAutoCommit(true);
            config.setConnectionTestQuery("SELECT 1");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            logger.info("Database connection pool initialized successfully");

        } catch (Exception e) {
            logger.error("Failed to initialize database connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Load properties from application.properties file
     */
    private Properties loadProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("Unable to find application.properties");
            }
            props.load(input);
        }
        return props;
    }

    /**
     * Get a connection from the pool
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return dataSource.getConnection();
    }

    /**
     * Get the DataSource (for repositories that need it)
     */
    public HikariDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Test the database connection
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            logger.error("Database connection test failed", e);
            return false;
        }
    }

    /**
     * Close the connection pool (call this when shutting down the application)
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool shut down successfully");
        }
    }

    /**
     * Get pool statistics for monitoring
     */
    public String getPoolStats() {
        if (dataSource != null) {
            return String.format("Active: %d, Idle: %d, Total: %d, Waiting: %d",
                    dataSource.getHikariPoolMXBean().getActiveConnections(),
                    dataSource.getHikariPoolMXBean().getIdleConnections(),
                    dataSource.getHikariPoolMXBean().getTotalConnections(),
                    dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        }
        return "DataSource not initialized";
    }
}
