package com.soldi.soldinoponiton;

import com.soldi.soldinoponiton.listeners.PotionStackListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Soldinoponiton extends JavaPlugin {

    private static Soldinoponiton instance;
    private int maxStack;

    public static Soldinoponiton getInstance() {
        return instance;
    }

    public int getMaxStack() {
        return maxStack;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();
        this.maxStack = Math.max(1, cfg.getInt("max-stack", 64));

        // Register only our potion stacking/anti-inventory logic
        Bukkit.getPluginManager().registerEvents(new PotionStackListener(maxStack), this);

        getLogger().info("[Soldinoponiton] Enabled. Max stack = " + maxStack);
    }

    @Override
    public void onDisable() {
        getLogger().info("[Soldinoponiton] Disabled.");
    }
}
