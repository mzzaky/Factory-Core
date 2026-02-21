package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
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
        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return;
        }

        if (plugin.getStorageManager() == null) {
            player.sendMessage("§cError: Storage manager not available!");
            return;
        }

        // Store factory ID in player's persistent data for click handling
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_factory_id"),
                PersistentDataType.STRING, factoryId);

        int slots = plugin.getConfig().getInt("factory.base-storage-slots", 9);

        // Ensure at least 2 rows: 1 for items + 1 for the action bar
        int size = ((slots + 8) / 9) * 9 + 9; // +9 reserves the bottom action row
        if (size > 54)
            size = 54;
        if (size < 18)
            size = 18; // minimum 2 rows

        Inventory inv = Bukkit.createInventory(null, size, "§6§lFactory Storage");

        // Item display area: all slots ABOVE the bottom action row
        int itemAreaSize = size - 9;
        Map<String, Integer> storage = plugin.getStorageManager().getInputStorage(factoryId);
        int slot = 0;

        if (storage != null) {
            for (Map.Entry<String, Integer> entry : storage.entrySet()) {
                if (slot >= itemAreaSize)
                    break;

                ItemStack item = plugin.getResourceManager().createItemStack(entry.getKey(), entry.getValue());
                if (item != null) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        // Store resource ID in the item
                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "resource_id"),
                                PersistentDataType.STRING, entry.getKey());

                        // Add lore for clarity
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
        // [Deposit All] .... [Withdraw All] .... [Back]
        // slot: size-9 size-5 size-1

        // Deposit All button
        inv.setItem(size - 9, createActionButton(
                Material.CHEST,
                "§a§lDeposit All",
                Arrays.asList(
                        "§7Deposits all plugin-registered",
                        "§7items from your inventory",
                        "§7into this factory storage."),
                "deposit_all"));

        // Withdraw All button
        inv.setItem(size - 5, createActionButton(
                Material.HOPPER,
                "§e§lWithdraw All",
                Arrays.asList(
                        "§7Retrieves all items stored",
                        "§7in this factory storage",
                        "§7and places them in your inventory."),
                "withdraw_all"));

        // Back button
        Material backMat = Material.matchMaterial(plugin.getConfig().getString("gui.back-item", "ARROW"));
        inv.setItem(size - 1, createItem(backMat != null ? backMat : Material.ARROW, "§e§lBack", null));

        player.openInventory(inv);
    }

    /**
     * Creates an action button tagged with a storage_action key so the click
     * handler can identify it without relying on display-name matching.
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