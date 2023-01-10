package com.github.hulcompsoc.whitelist;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;

import java.net.InetAddress;
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
    private final String url;
    private final String username;
    private final String password;

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
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

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

    public void updateMinecraftUserLastAccessDetails(InetAddress ipaddr, double x, double y, double z, String username) throws SQLException {
        AtomicReference<SQLException> ex = null;
        this.runOnDatabase((conn) -> {
            try {
                PreparedStatement updateStatement = conn.prepareStatement("UPDATE minecraft_users " +
                        "SET last_ip_address = ?, last_x, = ? last_y = ?, last_z = ? " +
                        "WHERE username = ?;");
                updateStatement.setString(1, ipaddr.getHostAddress());
                updateStatement.setDouble(2, x);
                updateStatement.setDouble(3, y);
                updateStatement.setDouble(4, z);
                updateStatement.setString(5, username);
                updateStatement.executeUpdate();
            } catch (SQLException e) {
                ex.set(e);
            }
        });

        if (ex != null) {
            throw ex.get();
        }
    }

    /**
     * Returns the user object for a given Minecraft username
     *
     * @param username Minecraft username to lookup
     * @return the user object
     * @throws SQLException thrown if any SQL errors occur when trying to get the user
     */
    public MinecraftUser getUser(String username) throws SQLException, UserNotFoundException {
        AtomicReference<MinecraftUser> ret = new AtomicReference<>(null);
        AtomicReference<SQLException> ex = new AtomicReference<>(null);
        AtomicReference<UserNotFoundException> ex2 = new AtomicReference<>(null);

        this.runOnDatabase((conn -> {
            try {
                conn.setAutoCommit(false);
                PreparedStatement getVerficationCountForUserPs = conn.prepareStatement("SELECT count(discord_user_id) " +
                        "FROM discord_minecraft_users WHERE verified = true AND minecraft_user = ?;");
                PreparedStatement getMinecraftUserPs = conn.prepareStatement("SELECT * FROM minecraft_users WHERE username = ?;");

                getVerficationCountForUserPs.setString(1, username);
                ResultSet res = getVerficationCountForUserPs.executeQuery();
                if (!res.next()) {
                    throw new UserNotFoundException();
                }
                final int verified = res.getInt(1);

                getMinecraftUserPs.setString(1, username);
                res = getMinecraftUserPs.executeQuery();
                if (!res.next()) {
                    throw new UserNotFoundException();
                }
                final MinecraftUser user = new MinecraftUser(res.getString("username"),
                        res.getInt("verification_number"),
                        res.getBoolean("banned"),
                        verified);

                ret.set(user);
                conn.commit();
            } catch (SQLException e) {
                ex.set(e);
            } catch (UserNotFoundException e) {
                ex2.set(e);
            }
        }));

        if (ex2.get() != null) {
            throw ex2.get();
        }

        if (ex.get() != null) {
            throw ex.get();
        }
        return ret.get();
    }
}