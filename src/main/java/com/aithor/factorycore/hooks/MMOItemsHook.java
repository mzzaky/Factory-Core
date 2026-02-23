package com.aithor.factorycore.hooks;

import com.aithor.factorycore.FactoryCore;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.inventory.ItemStack;

/**
 * Hook for MMOItems plugin integration.
 * Allows FactoryCore resources to use items from MMOItems.
 */
public class MMOItemsHook {

    private final FactoryCore plugin;
    private boolean enabled;

    public MMOItemsHook(FactoryCore plugin) {
        this.plugin = plugin;
        this.enabled = false;
        setup();
    }

    private void setup() {
        if (plugin.getServer().getPluginManager().getPlugin("MMOItems") != null) {
            enabled = true;
            plugin.getLogger().info("MMOItems hooked successfully!");
        } else {
            plugin.getLogger().info("MMOItems not found, MMOItems integration disabled.");
        }
    }

    /**
     * Check if MMOItems is available
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get an ItemStack from MMOItems by type and id
     *
     * @param typeName The MMOItems type (e.g. "SWORD", "ARMOR", "MATERIAL")
     * @param itemId   The MMOItems item ID (e.g. "STEEL_SWORD")
     * @param amount   The amount of items
     * @return The ItemStack, or null if not found
     */
    public ItemStack getMMOItem(String typeName, String itemId, int amount) {
        if (!enabled) return null;

        try {
            Type type = MMOItems.plugin.getTypes().get(typeName);
            if (type == null) {
                plugin.getLogger().warning("MMOItems type '" + typeName + "' not found!");
                return null;
            }

            ItemStack item = MMOItems.plugin.getItem(type, itemId);
            if (item == null) {
                plugin.getLogger().warning("MMOItems item '" + itemId + "' of type '" + typeName + "' not found!");
                return null;
            }

            item.setAmount(amount);
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get MMOItems item: " + typeName + ":" + itemId + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Get an ItemStack from MMOItems by type and id (single item)
     */
    public ItemStack getMMOItem(String typeName, String itemId) {
        return getMMOItem(typeName, itemId, 1);
    }

    /**
     * Check if an ItemStack is a specific MMOItems item
     *
     * @param item     The ItemStack to check
     * @param typeName The expected MMOItems type
     * @param itemId   The expected MMOItems item ID
     * @return true if the item matches
     */
    public boolean isMMOItem(ItemStack item, String typeName, String itemId) {
        if (!enabled || item == null) return false;

        try {
            NBTItem nbtItem = NBTItem.get(item);
            if (!nbtItem.hasType()) return false;

            String nbtType = nbtItem.getType();
            String nbtId = nbtItem.getString("MMOITEMS_ITEM_ID");

            return typeName.equalsIgnoreCase(nbtType) && itemId.equalsIgnoreCase(nbtId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if an ItemStack is any MMOItems item
     */
    public boolean isMMOItem(ItemStack item) {
        if (!enabled || item == null) return false;

        try {
            NBTItem nbtItem = NBTItem.get(item);
            return nbtItem.hasType();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the MMOItems type name from an ItemStack
     */
    public String getMMOItemType(ItemStack item) {
        if (!enabled || item == null) return null;

        try {
            NBTItem nbtItem = NBTItem.get(item);
            return nbtItem.hasType() ? nbtItem.getType() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the MMOItems item ID from an ItemStack
     */
    public String getMMOItemId(ItemStack item) {
        if (!enabled || item == null) return null;

        try {
            NBTItem nbtItem = NBTItem.get(item);
            return nbtItem.hasType() ? nbtItem.getString("MMOITEMS_ITEM_ID") : null;
        } catch (Exception e) {
            return null;
        }
    }
}
