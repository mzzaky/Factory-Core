package com.aithor.factorycore.listeners;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.PlayerFactory;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Protects player-created factory regions from unauthorized block
 * breaking and placement. Only the factory owner and admins with
 * the "factorycore.admin" permission can modify blocks within the region.
 */
public class PlayerFactoryProtectionListener implements Listener {

    private final FactoryCore plugin;

    public PlayerFactoryProtectionListener(FactoryCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("player-factory.protection.enabled", true)) return;

        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        PlayerFactory factory = plugin.getPlayerFactoryManager().getFactoryAt(loc);
        if (factory == null) return;

        // Allow owner
        if (factory.getOwner().equals(player.getUniqueId())) return;

        // Allow admins
        if (player.hasPermission("factorycore.admin")) return;

        event.setCancelled(true);
        player.sendMessage(plugin.getLanguageManager().getMessage("player-factory-protected"));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("player-factory.protection.enabled", true)) return;

        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        PlayerFactory factory = plugin.getPlayerFactoryManager().getFactoryAt(loc);
        if (factory == null) return;

        // Allow owner
        if (factory.getOwner().equals(player.getUniqueId())) return;

        // Allow admins
        if (player.hasPermission("factorycore.admin")) return;

        event.setCancelled(true);
        player.sendMessage(plugin.getLanguageManager().getMessage("player-factory-protected"));
    }
}
