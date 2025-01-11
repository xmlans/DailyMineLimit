package me.yourplugin.dailymine;

/**
 * DailyMineLimit
 * Limit Dayly Mining
 * By Star Dream Studio
 * https://xmc.tw
 */

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DailyMineLimit extends JavaPlugin implements Listener {
    // Stores the mining counts for each player
    private Map<UUID, Map<Material, Integer>> playerMiningLimits = new HashMap<>();
    // Stores the last mining time for each player
    private Map<UUID, Long> playerLastMineTime = new HashMap<>();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        // Register the event listener
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("DailyMineLimit plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("DailyMineLimit plugin has been disabled!");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        Material blockType = event.getBlock().getType();
        UUID playerId = player.getUniqueId();

        // Skip if the current world is not in the restricted worlds list
        if (!config.getStringList("restricted-worlds").contains(world.getName())) {
            return;
        }

        // Get the current time (in milliseconds)
        long currentTime = System.currentTimeMillis();
        long lastMineTime = playerLastMineTime.getOrDefault(playerId, 0L);

        // Check if 24 hours have passed, if so, reset the mining count
        if (currentTime - lastMineTime > 86400000L) { // 86400000 milliseconds = 24 hours
            playerMiningLimits.put(playerId, new HashMap<>());
        }

        // Get the mining limit for the current block type from the config
        int limit = config.getInt("limits." + blockType.name(), -1);
        if (limit == -1) {
            return; // If no limit is set for this block type, skip
        }

        // Get the player's mining record
        playerMiningLimits.putIfAbsent(playerId, new HashMap<>());
        Map<Material, Integer> minedBlocks = playerMiningLimits.get(playerId);
        int minedCount = minedBlocks.getOrDefault(blockType, 0);

        // If the mining count has reached or exceeded the limit, cancel the mining
        if (minedCount >= limit) {
            player.sendMessage("You have reached the daily mining limit for " + blockType.name() + "!");
            event.setCancelled(true);
        } else {
            // If the limit is not reached, increase the mining count for this block type
            minedBlocks.put(blockType, minedCount + 1);
        }

        // Update the last mining time for the player
        playerLastMineTime.put(playerId, currentTime);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle /dailymine reload command to reload the configuration
        if (command.getName().equalsIgnoreCase("dailymine")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("dailymine.reload")) {
                    reloadConfig();
                    config = getConfig();
                    sender.sendMessage("DailyMineLimit configuration has been reloaded!");
                } else {
                    sender.sendMessage("You do not have permission to execute this command.");
                }
                return true;
            }
        }
        return false;
    }
}
