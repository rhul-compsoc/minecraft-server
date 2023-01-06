package com.github.whitelist.rhulcompsoc;

import java.sql.Connection;

/**
 * An interface to run operations on a database safely.
 */
public interface DatabaseRunnable {
    /**
     * A runnable block that gets executed on the database, in the event of error the connection is always closed
     *
     * @param conn the connection to the database
     */
    public void run(Connection conn);
}
