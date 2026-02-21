package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.Factory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class OutputStorageGUI {

    private final FactoryCore plugin;
    private final Player player;
    private final String factoryId;

    public OutputStorageGUI(FactoryCore plugin, Player player, String factoryId) {
        this.plugin = plugin;
        this.player = player;
        this.factoryId = factoryId;
    }

    public void openOutputStorage() {
        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return;
        }

        // Check if player is the factory owner
        if (!player.getUniqueId().equals(factory.getOwner())) {
            player.sendMessage("§cYou are not the owner of this factory!");
            return;
        }

        // Calculate storage slots based on factory level from config
        int slots = plugin.getConfig().getInt("factory.base-storage-slots", 9);
        int size = ((slots + 8) / 9) * 9;
        if (size > 54)
            size = 54; // Max 6 rows

        Inventory inv = Bukkit.createInventory(null, size,
                "§6§lOutput Storage - " + factory.getType().getDisplayName());

        // Fill with border
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < size; i++) {
            inv.setItem(i, border);
        }

        // Get output storage contents from StorageManager
        Map<String, Integer> outputStorage = plugin.getStorageManager().getOutputStorage(factoryId);

        int slot = 0;
        for (Map.Entry<String, Integer> entry : outputStorage.entrySet()) {
            if (slot >= slots)
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
                    if (lore != null) {
                        lore.add("");
                    }
                    lore.add("§eLeft Click: §aTake All");
                    lore.add("§eRight Click: §aTake 1");
                    lore.add("§7Output Storage");
                    meta.setLore(lore);

                    item.setItemMeta(meta);
                }
                inv.setItem(slot++, item);
            }
        }

        // Add info item in the last slot
        if (size >= 9) {
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§7Storage Slots: §e" + slots);
            infoLore.add("§7Factory Level: §e" + factory.getLevel());
            infoLore.add("§7Items in storage: §e" + outputStorage.size());
            inv.setItem(size - 1, createItem(Material.BOOK, "§6§lStorage Info", infoLore));
        }

        player.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR)
            return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null)
            return;

        Material material = clickedItem.getType();
        String name = meta.getDisplayName();

        // Handle storage info item
        if (material == Material.BOOK && name.contains("Storage Info")) {
            return; // Do nothing for info item
        }

        // Handle back button (if any)
        if (material == Material.ARROW && name.contains("Back")) {
            // Close inventory or go back to main menu
            player.closeInventory();
            return;
        }

        // Handle item withdrawal
        String resourceId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "resource_id"),
                PersistentDataType.STRING);
        if (resourceId != null) {
            handleWithdraw(clickedItem, resourceId);
        }
    }

    private void handleWithdraw(ItemStack clickedItem, String resourceId) {
        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory == null)
            return;

        int amountToWithdraw = clickedItem.getAmount();

        // Check if there's enough in output storage
        int available = plugin.getStorageManager().getOutputAmount(factoryId, resourceId);
        if (available < amountToWithdraw) {
            amountToWithdraw = available;
        }

        if (amountToWithdraw <= 0) {
            player.sendMessage("§cThere are no items to take!");
            return;
        }

        // Remove from output storage using StorageManager
        plugin.getStorageManager().removeOutputItem(factoryId, resourceId, amountToWithdraw);

        // Give item to player
        ItemStack toGive = plugin.getResourceManager().createItemStack(resourceId, amountToWithdraw);
        if (toGive != null) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);
            if (!leftover.isEmpty()) {
                // If inventory is full, return items to output storage
                int amountNotAdded = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
                plugin.getStorageManager().addOutputItem(factoryId, resourceId, amountNotAdded);
                player.sendMessage("§cInventory is full! Some items were returned to storage.");
            } else {
                ItemMeta itemMeta = toGive.getItemMeta();
                String itemName = (itemMeta != null) ? itemMeta.getDisplayName() : resourceId;
                player.sendMessage("§aSuccessfully took §e" + amountToWithdraw + "x " +
                        itemName + " §afrom the output storage!");
            }
        }

        // Close the GUI instead of refreshing
        player.closeInventory();
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