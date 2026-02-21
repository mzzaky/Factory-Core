package com.aithor.factorycore.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.gui.FactoryGUI;
import com.aithor.factorycore.gui.OutputStorageGUI;

public class InventoryClickListener implements Listener {

    private final FactoryCore plugin;

    public InventoryClickListener(FactoryCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Debug logging untuk semua klik pada factory GUI (jika debug aktif)
        if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
            plugin.getLogger().info("=== INVENTORY CLICK DEBUG ===");
            plugin.getLogger().info("GUI Title: '" + title + "'");
            plugin.getLogger().info(
                    "Clicked item: " + (event.getCurrentItem() != null ? event.getCurrentItem().getType() : "NULL"));
        }

        // Check if it's a factory GUI - use more robust checking
        if (title.contains("Factory:") ||
                title.toLowerCase().contains("select") ||
                title.contains("Confirm:") ||
                title.contains("Confirm Upgrade") ||
                title.contains("Storage") ||
                title.contains("Invoice") ||
                title.contains("Upgrade Factory") ||
                title.contains("Production Recipe") ||
                title.contains("Output Storage")) {

            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Factory GUI detected, processing click...");
            }

            ItemStack clickedItem = event.getCurrentItem();

            // For recipe selection menu, be more permissive with clicks
            if (title.contains("Select Recipe")) {
                if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                    plugin.getLogger().info("Recipe selection menu detected");
                }
                // Only cancel if it's clearly not a valid interaction
                if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                    if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                        plugin.getLogger().info("Cancelling click on null/air item");
                    }
                    event.setCancelled(true);
                    return;
                }
                if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                    plugin.getLogger().info("Allowing click on recipe item: " + clickedItem.getType());
                }
                // Allow all other clicks in recipe menu to be processed
            } else {
                if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                    plugin.getLogger().info("Other factory GUI detected");
                }
                // For other GUIs, prevent item manipulation but allow button clicks
                if (clickedItem == null || !clickedItem.hasItemMeta()) {
                    if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                        plugin.getLogger().info("Cancelling click on null item or item without meta");
                    }
                    event.setCancelled(true);
                    return;
                }
                if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                    plugin.getLogger().info("Allowing click on valid item");
                }
            }

            // Get factory ID and recipe ID from player's persistent data
            String factoryId = player.getPersistentDataContainer().get(new NamespacedKey(plugin, "current_factory_id"),
                    PersistentDataType.STRING);
            String recipeId = player.getPersistentDataContainer().get(new NamespacedKey(plugin, "current_recipe_id"),
                    PersistentDataType.STRING);

            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Factory ID from persistent data: " + factoryId);
                plugin.getLogger().info("Recipe ID from persistent data: " + recipeId);
            }

            // Handle GUI clicks based on GUI type
            if (title.contains("Output Storage")) {
                // Handle output storage clicks
                if (factoryId != null) {
                    plugin.getOutputStorageGUI(player, factoryId).handleClick(event);
                }
            } else {
                // Handle other factory GUI clicks in FactoryGUI class
                FactoryGUI gui = new FactoryGUI(plugin, player, factoryId);
                if (recipeId != null) {
                    gui.setCurrentRecipeId(recipeId);
                }

                if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                    plugin.getLogger().info("Calling gui.handleClick()...");
                }
                gui.handleClick(event);
                if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                    plugin.getLogger().info("gui.handleClick() completed");
                }
            }
        } else {
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Not a factory GUI, ignoring click");
            }
        }
    }
}