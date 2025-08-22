package com.soldi.soldinoponiton;

import com.soldi.soldinoponiton.listeners.AnvilCombineListener;
import com.soldi.soldinoponiton.listeners.PotionStackListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Soldinoponiton extends JavaPlugin {

    private static Soldinoponiton instance;
    private int maxStack;
    private boolean combineThirdLevel;
    private int combineCost;

    public static Soldinoponiton getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadSettings();

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new PotionStackListener(maxStack), this);
        Bukkit.getPluginManager().registerEvents(new AnvilCombineListener(() -> combineThirdLevel, () -> combineCost), this);

        getLogger().info("Soldinoponiton enabled. Max stack = " + maxStack + ", combineThirdLevel=" + combineThirdLevel + ", cost=" + combineCost);
    }

    private void loadSettings() {
        FileConfiguration cfg = getConfig();
        this.maxStack = Math.max(1, cfg.getInt("max-stack", 64));
        this.combineThirdLevel = cfg.getBoolean("combine-third-level", true);
        this.combineCost = Math.max(0, cfg.getInt("combine-cost", 5));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("soldinoponiton")) return false;

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("soldinoponiton.reload")) {
                sender.sendMessage("§cУ тебя нет прав (soldinoponiton.reload).");
                return true;
            }
            reloadConfig();
            loadSettings();
            sender.sendMessage("§aSoldinoponiton: конфиг перезагружен.");
            return true;
        }

        sender.sendMessage("§eИспользование: /" + label + " reload");
        return true;
    }
}
