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
        // Resolve from either admin or player factory
        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        com.aithor.factorycore.models.PlayerFactory playerFactory = null;
        if (factory == null && plugin.getPlayerFactoryManager() != null) {
            playerFactory = plugin.getPlayerFactoryManager().getFactory(factoryId);
        }

        if (factory == null && playerFactory == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return;
        }

        // Ownership check
        java.util.UUID ownerUUID = factory != null ? factory.getOwner() : playerFactory.getOwner();
        if (!player.getUniqueId().equals(ownerUUID)) {
            player.sendMessage("§cYou are not the owner of this factory!");
            return;
        }

        // Resolve display info
        String typeDisplay = factory != null
                ? factory.getType().getDisplayName()
                : playerFactory.getType().getDisplayName();
        int factoryLevel = factory != null ? factory.getLevel() : playerFactory.getLevel();

        // Calculate storage slots
        int baseSlots = plugin.getConfig().getInt("factory.base-storage-slots", 9);
        int slotsPerLevel = plugin.getConfig().getInt("factory.level-bonuses.slots-per-level", 9);
        int totalSlots = baseSlots + ((factoryLevel - 1) * slotsPerLevel);
        
        // GUI Size: storage rows + 1 control row
        int storageRows = (totalSlots + 8) / 9;
        int size = (storageRows + 1) * 9;
        if (size > 54) size = 54;
        if (totalSlots > 45) totalSlots = 45; // Cap storage at 45 to leave room for controls

        Inventory inv = Bukkit.createInventory(null, size,
                "§6§lOutput Storage - " + typeDisplay);

        // Fill with border
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < size; i++) {
            inv.setItem(i, border);
        }

        // Get output storage contents from StorageManager
        Map<String, Integer> outputStorage = plugin.getStorageManager().getOutputStorage(factoryId);

        int slot = 0;
        for (Map.Entry<String, Integer> entry : outputStorage.entrySet()) {
            if (slot >= totalSlots)
                break;

            ItemStack item = plugin.getResourceManager().createItemStack(entry.getKey(), entry.getValue());
            if (item == null && entry.getKey().startsWith("vanilla:")) {
                // Handle vanilla items
                String materialName = entry.getKey().substring("vanilla:".length()).toUpperCase();
                try {
                    Material mat = Material.valueOf(materialName);
                    item = new ItemStack(mat, entry.getValue());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (item != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "resource_id"),
                            PersistentDataType.STRING, entry.getKey());

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

        // Control Row (Bottom Row)
        int lastRowStart = size - 9;
        
        // Back Button
        inv.setItem(lastRowStart, createItem(Material.ARROW, "§e§lBack", Arrays.asList("§7Return to previous menu")));

        // Info item
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Total Capacity: §e" + totalSlots + " slots");
        infoLore.add("§7Factory Level: §e" + factoryLevel);
        infoLore.add("§7Items Stored: §e" + outputStorage.size());
        inv.setItem(lastRowStart + 4, createItem(Material.BOOK, "§6§lStorage Info", infoLore));

        // Collect All Button
        if (!outputStorage.isEmpty()) {
            inv.setItem(lastRowStart + 8, createItem(Material.CHEST, "§a§lCollect All", 
                Arrays.asList("§7Click to withdraw all items", "§7into your inventory.")));
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
            return;
        }

        // Handle back button
        if (material == Material.ARROW && name.contains("Back")) {
            player.closeInventory();
            // Could link back to MainMenuGUI if needed
            return;
        }

        // Handle Collect All
        if (material == Material.CHEST && name.contains("Collect All")) {
            handleCollectAll();
            return;
        }

        // Handle item withdrawal
        String resourceId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "resource_id"),
                PersistentDataType.STRING);
        if (resourceId != null) {
            handleWithdraw(clickedItem, resourceId, event.isRightClick());
        }
    }

    private void handleCollectAll() {
        Map<String, Integer> outputStorage = new HashMap<>(plugin.getStorageManager().getOutputStorage(factoryId));
        if (outputStorage.isEmpty()) return;

        boolean anyCollected = false;
        boolean inventoryFull = false;

        for (Map.Entry<String, Integer> entry : outputStorage.entrySet()) {
            String resourceId = entry.getKey();
            int amount = entry.getValue();

            ItemStack toGive = plugin.getResourceManager().createItemStack(resourceId, amount);
            if (toGive == null && resourceId.startsWith("vanilla:")) {
                String materialName = resourceId.substring("vanilla:".length()).toUpperCase();
                try {
                    toGive = new ItemStack(Material.valueOf(materialName), amount);
                } catch (IllegalArgumentException ignored) {}
            }

            if (toGive != null) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);
                int collected = amount;
                if (!leftover.isEmpty()) {
                    int amountNotAdded = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
                    collected = amount - amountNotAdded;
                    inventoryFull = true;
                }

                if (collected > 0) {
                    plugin.getStorageManager().removeOutputItem(factoryId, resourceId, collected);
                    anyCollected = true;
                }
            }
            
            if (inventoryFull) break;
        }

        if (anyCollected) {
            player.sendMessage("§aSuccessfully collected items from storage!");
            if (inventoryFull) {
                player.sendMessage("§cInventory is full! Some items remain in storage.");
            }
            openOutputStorage(); // Refresh GUI
        } else if (inventoryFull) {
            player.sendMessage("§cInventory is full!");
        }
    }

    private void handleWithdraw(ItemStack clickedItem, String resourceId, boolean isOne) {
        // Verify factory ownership
        boolean ownerValid = false;
        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory != null) {
            ownerValid = player.getUniqueId().equals(factory.getOwner());
        } else if (plugin.getPlayerFactoryManager() != null) {
            com.aithor.factorycore.models.PlayerFactory pf = plugin.getPlayerFactoryManager().getFactory(factoryId);
            if (pf != null) ownerValid = player.getUniqueId().equals(pf.getOwner());
        }
        if (!ownerValid) return;

        int available = plugin.getStorageManager().getOutputAmount(factoryId, resourceId);
        int amountToWithdraw = isOne ? 1 : clickedItem.getAmount();
        
        if (amountToWithdraw > available) amountToWithdraw = available;

        if (amountToWithdraw <= 0) {
            player.sendMessage("§cThere are no items to take!");
            return;
        }

        // Build item to give
        ItemStack toGive = plugin.getResourceManager().createItemStack(resourceId, amountToWithdraw);
        if (toGive == null && resourceId.startsWith("vanilla:")) {
            String materialName = resourceId.substring("vanilla:".length()).toUpperCase();
            try {
                toGive = new ItemStack(Material.valueOf(materialName), amountToWithdraw);
            } catch (IllegalArgumentException ignored) {}
        }

        if (toGive != null) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);
            if (!leftover.isEmpty()) {
                int amountNotAdded = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
                int actualTaken = amountToWithdraw - amountNotAdded;
                if (actualTaken > 0) {
                    plugin.getStorageManager().removeOutputItem(factoryId, resourceId, actualTaken);
                    player.sendMessage("§aSuccessfully took §e" + actualTaken + "x §afrom storage!");
                }
                player.sendMessage("§cInventory is full!");
            } else {
                plugin.getStorageManager().removeOutputItem(factoryId, resourceId, amountToWithdraw);
                ItemMeta itemMeta = toGive.getItemMeta();
                String itemName = (itemMeta != null && itemMeta.hasDisplayName()) ? itemMeta.getDisplayName() : resourceId;
                player.sendMessage("§aSuccessfully took §e" + amountToWithdraw + "x " + itemName + " §afrom storage!");
            }
        }

        openOutputStorage(); // Refresh GUI instead of closing
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