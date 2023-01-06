package com.github.whitelist.rhulcompsoc;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class PluginMain extends JavaPlugin {
    @Override
    public void onEnable() {
        this.getLogger().log(Level.INFO, "Compsoc Whitelist plugin enabled.");
    }

    @Override
    public void onDisable() {
        this.getLogger().log(Level.INFO, "Compsoc Whitelist plugin disabled.");
    }
}
