package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.managers.ResourceManager;
import com.aithor.factorycore.models.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class StorageGUI {

    private final FactoryCore plugin;
    private final Player player;
    private final String factoryId;

    public StorageGUI(FactoryCore plugin, Player player, String factoryId) {
        this.plugin = plugin;
        this.player = player;
        this.factoryId = factoryId;
    }

    public void openStorageMenu() {
        // Resolve from either admin factory or player factory
        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        com.aithor.factorycore.models.PlayerFactory playerFactory = null;
        if (factory == null && plugin.getPlayerFactoryManager() != null) {
            playerFactory = plugin.getPlayerFactoryManager().getFactory(factoryId);
        }
        if (factory == null && playerFactory == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return;
        }

        int factoryLevel = (factory != null) ? factory.getLevel() : playerFactory.getLevel();

        if (plugin.getStorageManager() == null) {
            player.sendMessage("§cError: Storage manager not available!");
            return;
        }

        // Store factory ID in player's persistent data for click handling
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_factory_id"),
                PersistentDataType.STRING, factoryId);

        // Calculate storage slots
        int baseSlots = plugin.getConfig().getInt("factory.base-storage-slots", 9);
        int slotsPerLevel = plugin.getConfig().getInt("factory.level-bonuses.slots-per-level", 9);
        int totalSlots = baseSlots + ((factoryLevel - 1) * slotsPerLevel);

        // GUI Size: storage rows + 1 control row
        int storageRows = (totalSlots + 8) / 9;
        int size = (storageRows + 1) * 9;
        if (size > 54) size = 54;
        if (totalSlots > 45) totalSlots = 45; // Cap to keep a row for controls
        if (size < 18) size = 18;

        Inventory inv = Bukkit.createInventory(null, size, "§6§lFactory Storage");

        // Fill with border first
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < size; i++) {
            inv.setItem(i, border);
        }

        // Get input storage contents from StorageManager
        Map<String, Integer> storage = plugin.getStorageManager().getInputStorage(factoryId);
        int slot = 0;

        if (storage != null) {
            for (Map.Entry<String, Integer> entry : storage.entrySet()) {
                if (slot >= totalSlots)
                    break;

                String resourceId = entry.getKey();
                int storedAmount = entry.getValue();
                ItemStack item;

                // Vanilla inputs (e.g. "vanilla:IRON_ORE") use a special display helper
                if (ResourceManager.isVanillaInput(resourceId)) {
                    item = plugin.getResourceManager().createVanillaInputDisplay(resourceId, storedAmount);
                } else {
                    item = plugin.getResourceManager().createItemStack(resourceId, storedAmount);
                }

                if (item != null) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        // Store resource ID in the item for click-handling
                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "resource_id"),
                                PersistentDataType.STRING, resourceId);

                        // Add interaction lore
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        lore.add("");
                        lore.add("§eLeft Click: §aTake All");
                        lore.add("§eRight Click: §aTake 1");
                        lore.add("§eShift+Click §7(from your inventory): §aDeposit");
                        meta.setLore(lore);

                        item.setItemMeta(meta);
                    }
                    inv.setItem(slot++, item);
                }
            }
        }

        // ── Bottom action row ──────────────────────────────────────────────────
        int lastRowStart = size - 9;

        // Back button - slot 0
        Material backMat = Material.matchMaterial(plugin.getConfig().getString("gui.back-item", "ARROW"));
        inv.setItem(lastRowStart, createItem(backMat != null ? backMat : Material.ARROW, "§e§lBack", null));

        // Deposit All button - slot 2
        inv.setItem(lastRowStart + 2, createActionButton(
                Material.CHEST,
                "§a§lDeposit All",
                Arrays.asList(
                        "§7Deposits all relevant items",
                        "§7from your inventory",
                        "§7into this factory storage."),
                "deposit_all"));

        // Storage Info - slot 4
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Total Capacity: §e" + totalSlots + " slots");
        infoLore.add("§7Factory Level: §e" + factoryLevel);
        infoLore.add("§7Items Stored: §e" + (storage != null ? storage.size() : 0));
        inv.setItem(lastRowStart + 4, createItem(Material.BOOK, "§6§lStorage Info", infoLore));

        // Withdraw All button - slot 6
        inv.setItem(lastRowStart + 6, createActionButton(
                Material.HOPPER,
                "§e§lWithdraw All",
                Arrays.asList(
                        "§7Retrieves all items stored",
                        "§7in this storage",
                        "§7into your inventory."),
                "withdraw_all"));

        player.openInventory(inv);
    }

    /**
     * Creates an action button tagged with a storage_action key.
     */
    private ItemStack createActionButton(Material material, String name, List<String> lore, String actionId) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "storage_action"),
                    PersistentDataType.STRING,
                    actionId);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (name != null)
                meta.setDisplayName(name);
            if (lore != null)
                meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }
}