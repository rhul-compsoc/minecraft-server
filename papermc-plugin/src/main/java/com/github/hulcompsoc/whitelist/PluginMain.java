package com.github.hulcompsoc.whitelist;

import io.github.cdimascio.dotenv.Dotenv;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

public class PluginMain extends JavaPlugin implements Listener {
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

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        this.getLogger().log(Level.INFO, "Compsoc Whitelist plugin disabled.");
    }

    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        try {
            MinecraftUser user = this.conn.getUser(event.getName());

            // Check for bans
            if (user.isBanned()) {
                event.disallow(Result.KICK_BANNED, "Your account has been banned via the Discord bot. Please contract committee if this is wrong.");
                getLogger().info("User " + event.getName() + " has been banned but has tried to join.");
                return;
            } else if (user.getVerified() == 0) {https://discord.com/channels/500612695570120704/1045311826528849960
                // Check that the user has been verified at least once
                event.disallow(Result.KICK_OTHER, "This Minecraft account has not been verified yet, please use '/mcverify "
                        + event.getName() + " "
                        + user.getVerificationNumber() + "' to verify your account");
                return;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            event.disallow(Result.KICK_OTHER, "An internal server error has occurred :(");
            return;
        } catch (UserNotFoundException e) {
            e.printStackTrace();
            event.disallow(Result.KICK_OTHER, "You are not in the Compsoc Minecraft server's whitelist. Please use '/mcadd " + event.getName() + "'");
            return;
        }

        event.allow();
    }

    @EventHandler
    public void afterPlayerJoin(PlayerJoinEvent event) {
        try {
            conn.updateMinecraftUserLastAccessDetails(event.getPlayer().getAddress().getAddress(),
                    event.getPlayer().getLocation().getX(),
                    event.getPlayer().getLocation().getY(),
                    event.getPlayer().getLocation().getZ(),
                    event.getPlayer().getName());
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().warning("Cannot update player status");
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String perms = getCommand(event.getEventName()).getPermission();
        if (perms != null && perms != "" && perms != "none") {
            // Check permissions
        }
    }
}
