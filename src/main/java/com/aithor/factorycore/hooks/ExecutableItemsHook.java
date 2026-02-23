package com.aithor.factorycore.hooks;

import com.aithor.factorycore.FactoryCore;
import com.ssomar.score.api.executableitems.ExecutableItemsAPI;
import com.ssomar.score.api.executableitems.config.ExecutableItemInterface;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Hook for ExecutableItems plugin integration.
 * Allows FactoryCore resources to use items from ExecutableItems.
 */
public class ExecutableItemsHook {

    private final FactoryCore plugin;
    private boolean enabled;

    public ExecutableItemsHook(FactoryCore plugin) {
        this.plugin = plugin;
        this.enabled = false;
        setup();
    }

    private void setup() {
        if (plugin.getServer().getPluginManager().getPlugin("ExecutableItems") != null) {
            enabled = true;
            plugin.getLogger().info("ExecutableItems hooked successfully!");
        } else {
            plugin.getLogger().info("ExecutableItems not found, ExecutableItems integration disabled.");
        }
    }

    /**
     * Check if ExecutableItems is available
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get an ItemStack from ExecutableItems by its ID
     *
     * @param itemId The ExecutableItems item ID
     * @param amount The amount of items
     * @return The ItemStack, or null if not found
     */
    public ItemStack getExecutableItem(String itemId, int amount) {
        if (!enabled)
            return null;

        try {
            Optional<ExecutableItemInterface> eiOpt = ExecutableItemsAPI
                    .getExecutableItemsManager()
                    .getExecutableItem(itemId);

            if (eiOpt.isEmpty()) {
                plugin.getLogger().warning("ExecutableItems item '" + itemId + "' not found!");
                return null;
            }

            ItemStack item = eiOpt.get().buildItem(amount, Optional.empty());
            if (item == null) {
                plugin.getLogger().warning("Failed to build ExecutableItems item '" + itemId + "'!");
                return null;
            }

            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get ExecutableItems item: " + itemId + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Get an ItemStack from ExecutableItems by its ID (single item)
     */
    public ItemStack getExecutableItem(String itemId) {
        return getExecutableItem(itemId, 1);
    }

    /**
     * Check if an ItemStack is a specific ExecutableItems item
     *
     * @param item   The ItemStack to check
     * @param itemId The expected ExecutableItems item ID
     * @return true if the item matches
     */
    public boolean isExecutableItem(ItemStack item, String itemId) {
        if (!enabled || item == null)
            return false;

        try {
            Optional<ExecutableItemInterface> eiOpt = ExecutableItemsAPI
                    .getExecutableItemsManager()
                    .getExecutableItem(item);

            return eiOpt.isPresent() && eiOpt.get().getId().equalsIgnoreCase(itemId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if an ItemStack is any ExecutableItems item
     */
    public boolean isExecutableItem(ItemStack item) {
        if (!enabled || item == null)
            return false;

        try {
            Optional<ExecutableItemInterface> eiOpt = ExecutableItemsAPI
                    .getExecutableItemsManager()
                    .getExecutableItem(item);

            return eiOpt.isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the ExecutableItems item ID from an ItemStack
     *
     * @param item The ItemStack to check
     * @return The ExecutableItems item ID, or null if not an EI item
     */
    public String getExecutableItemId(ItemStack item) {
        if (!enabled || item == null)
            return null;

        try {
            Optional<ExecutableItemInterface> eiOpt = ExecutableItemsAPI
                    .getExecutableItemsManager()
                    .getExecutableItem(item);

            return eiOpt.map(ExecutableItemInterface::getId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
