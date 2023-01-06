package com.github.whitelist.rhulcompsoc;

import io.github.cdimascio.dotenv.Dotenv;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

public class PluginMain extends JavaPlugin {
    /**
     * This is the database connector for the plugin, it will connect to a PSQL database that stores all of the required
     * data for the server.
     */
    private Database conn = null;

    @Override
    public void onEnable() {
        this.getLogger().log(Level.INFO, "Compsoc Whitelist plugin enabled.");

        // Load the .env file and, create the database pool
        Dotenv dotenv = null;

        try {
            // Nasty hack to get dotenv to behave
            final String folder = System.getProperty("user.dir");
            dotenv = Dotenv.configure()
                    .directory(folder)
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            System.out.println(".env file is missing, see ../README.md");
            System.exit(1);
        }

        final String db_url = dotenv.get("DB_URL"),
                username = dotenv.get("DB_USERNAME"),
                password = dotenv.get("DB_PASSWORD");

        // Check for missing args
        if (db_url == null) {
            throw new RuntimeException("Cannot find DB_URL");
        }
        if (username == null) {
            throw new RuntimeException("Cannot find DB_USERNAME");
        }
        if (password == null) {
            throw new RuntimeException("Cannot find DB_PASSWORD");
        }

        try {
            this.conn = new Database(db_url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        this.getLogger().log(Level.INFO, "Compsoc Whitelist plugin disabled.");
    }
}
