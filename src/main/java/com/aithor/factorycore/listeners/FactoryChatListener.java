package com.aithor.factorycore.listeners;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.gui.FactoryGUI;
import com.aithor.factorycore.models.Factory;
import com.aithor.factorycore.models.PlayerFactory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles chat-input prompts for factory rename and icon-change actions.
 * A pending action is stored in the player's PersistentDataContainer as:
 *   "pending_action" -> "set_name:<factoryId>"
 *   "pending_action" -> "set_icon:<factoryId>"
 */
public class FactoryChatListener implements Listener {

    private final FactoryCore plugin;

    public FactoryChatListener(FactoryCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        String pending = player.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "pending_action"), PersistentDataType.STRING);

        if (pending == null) return;

        // Intercept the message
        event.setCancelled(true);

        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("cancel")) {
            player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "pending_action"));
            player.sendMessage("§cAction cancelled.");
            return;
        }

        if (pending.startsWith("set_name:")) {
            String factoryId = pending.substring("set_name:".length());

            if (message.length() > 32) {
                player.sendMessage("§cName too long! Maximum 32 characters.");
                return;
            }

            player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "pending_action"));

            // Apply on main thread
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                applyDisplayName(player, factoryId, message);
            });

        } else if (pending.startsWith("set_icon:")) {
            String factoryId = pending.substring("set_icon:".length());
            Material mat = Material.matchMaterial(message.toUpperCase());

            if (mat == null || mat == Material.AIR) {
                player.sendMessage("§cInvalid material ID: §e" + message + "§c. Try again or type §fcancel§c.");
                return;
            }

            player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "pending_action"));

            // Apply on main thread
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                applyCustomIcon(player, factoryId, mat.name());
            });
        }
    }

    private void applyDisplayName(Player player, String factoryId, String name) {
        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        PlayerFactory pf = plugin.getPlayerFactoryManager() != null
                ? plugin.getPlayerFactoryManager().getFactory(factoryId)
                : null;

        if (factory != null) {
            factory.setDisplayName(name);
            plugin.getFactoryManager().saveAll();
        } else if (pf != null) {
            pf.setDisplayName(name);
            plugin.getPlayerFactoryManager().saveAll();
        } else {
            player.sendMessage("§cFactory not found.");
            return;
        }

        player.sendMessage("§aDisplay name set to: §f" + name);

        // Re-open the factory main menu
        player.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "current_factory_id"),
                PersistentDataType.STRING, factoryId);
        new FactoryGUI(plugin, player, factoryId).openMainMenu();
    }

    private void applyCustomIcon(Player player, String factoryId, String materialName) {
        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        PlayerFactory pf = plugin.getPlayerFactoryManager() != null
                ? plugin.getPlayerFactoryManager().getFactory(factoryId)
                : null;

        if (factory != null) {
            factory.setCustomIcon(materialName);
            plugin.getFactoryManager().saveAll();
        } else if (pf != null) {
            pf.setCustomIcon(materialName);
            plugin.getPlayerFactoryManager().saveAll();
        } else {
            player.sendMessage("§cFactory not found.");
            return;
        }

        player.sendMessage("§aFactory icon set to: §b" + materialName);

        // Re-open the factory main menu
        player.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "current_factory_id"),
                PersistentDataType.STRING, factoryId);
        new FactoryGUI(plugin, player, factoryId).openMainMenu();
    }
}
