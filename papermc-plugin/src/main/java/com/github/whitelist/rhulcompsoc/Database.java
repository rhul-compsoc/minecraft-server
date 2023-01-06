package com.github.whitelist.rhulcompsoc;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Accusations that this class comes from my year 2 group project are correct. I cannot be arsed to rewrite it.
 * Deal with it nerd.
 */
public class Database {
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(this.getClass().getName());
    private final BasicDataSource ds = new BasicDataSource();
    private String url, username, password;

    /**
     * Construct a database connector with given login details
     *
     * @param url      the url (with database name) of the database
     * @param username the username for the database
     * @param password the password for the database
     * @since 1
     */
    public Database(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;

        // Setup pool
        this.ds.setUrl(this.url);
        this.ds.setUsername(this.username);
        this.ds.setPassword(this.password);
        this.ds.setMinIdle(20);
        this.ds.setMaxIdle(100);
        this.ds.setMaxOpenPreparedStatements(100);
        this.ds.setMaxTotal(500);
    }

    /**
     * Runs code on the database then closes the connection, even if there is a connection
     *
     * @param runnable the code to run
     * @return whether an error occurred during execution
     * @throws SQLException an sql exception that was thrown whilst connecting
     * @since 1
     */
    public boolean runOnDatabase(DatabaseRunnable runnable) throws SQLException {
        Connection conn = null;
        RuntimeException ex = null;

        try {
            conn = DriverManager.getConnection(this.url, this.username, this.password);
            runnable.run(conn);
        } catch (RuntimeException e) {
            logger.error(e);
            ex = e;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }

        return ex != null;
    }
}