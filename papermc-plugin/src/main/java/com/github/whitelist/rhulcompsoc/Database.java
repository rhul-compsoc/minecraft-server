package com.github.whitelist.rhulcompsoc;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;

import java.sql.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Accusations that this class comes from my year 2 group project are correct. I cannot be arsed to rewrite it.
 * Deal with it nerd.
 *
 * @author Danny
 * @version 2
 */
public class Database {
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(this.getClass().getName());
    private final BasicDataSource ds = new BasicDataSource();
    private String url, username, password;
    private PreparedStatement getMinecraftUserPs;
    private PreparedStatement getVerficationCountForUserPs;

    /**
     * Construct a database connector with given login details
     *
     * @param url      the url (with database name) of the database
     * @param username the username for the database
     * @param password the password for the database
     * @since 1
     */
    public Database(String url, String username, String password) throws SQLException {
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

        this.getMinecraftUserPs = this.prepareStatement("SELECT * FROM minecraft_users WHERE username = ?;");
        this.getVerficationCountForUserPs = this.prepareStatement("SELECT count(discord_user_id) FROM discord_minecraft_users WHERE verified = true AND miencraft_user = ?;");
    }

    /**
     * Prepares a statement and, returns it.
     *
     * @param statement the SQL statement to prepare on this database
     * @return the prepared statement, not null
     * @throws SQLException thrown when the statement cannot be prepared
     * @since 2
     */
    private PreparedStatement prepareStatement(String statement) throws SQLException {
        AtomicReference<PreparedStatement> ret = null;
        AtomicReference<SQLException> ex = null;

        this.runOnDatabase((conn -> {
            try {
                ret.set(conn.prepareStatement(statement));
            } catch (SQLException e) {
                ex.set(e);
            }
        }));

        if (ex != null) {
            throw ex.get();
        }
        return ret.get();
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
            conn.setAutoCommit(true);
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

    /**
     * Returns the user object for a given Minecraft username
     *
     * @param username Minecraft username to lookup
     * @return the user object
     * @throws SQLException thrown if any SQL errors occur when trying to get the user
     */
    public MinecraftUser getUser(String username) throws SQLException {
        AtomicReference<MinecraftUser> ret = null;
        AtomicReference<SQLException> ex = null;

        this.runOnDatabase((conn -> {
            try {
                conn.setAutoCommit(false);

                getVerficationCountForUserPs.setString(1, username);
                ResultSet res = getVerficationCountForUserPs.executeQuery();
                final int verified = res.getInt(1);

                getMinecraftUserPs.setString(1, username);
                res = getMinecraftUserPs.executeQuery();
                res.next();
                final MinecraftUser user = new MinecraftUser(res.getString("username"),
                        res.getInt("verification_number"),
                        res.getBoolean("banned"),
                        verified);

                ret.set(user);
                conn.commit();
            } catch (SQLException e) {
                ex.set(e);
            }
        }));

        if (ex != null) {
            throw ex.get();
        }
        return ret.get();
    }
}