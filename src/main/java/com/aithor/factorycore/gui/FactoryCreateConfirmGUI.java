package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.FactoryType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Confirmation GUI before creating a player factory.
 */
public class FactoryCreateConfirmGUI {

    private final FactoryCore plugin;
    private final Player player;

    public FactoryCreateConfirmGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open(FactoryType type) {
        String typeId = type.getId();
        double price = plugin.getConfig().getDouble("player-factory.creation-prices." + typeId,
                plugin.getConfig().getDouble("factory-types." + typeId + ".base-price", 100000.0));

        int radius = plugin.getConfig().getInt("player-factory.region-radius", 3);
        int heightUp = plugin.getConfig().getInt("player-factory.region-height-up", 5);
        int heightDown = plugin.getConfig().getInt("player-factory.region-height-down", 5);
        int sizeX = (radius * 2) + 1;
        int sizeY = heightUp + heightDown + 1;

        Inventory inv = Bukkit.createInventory(null, 27, "§6§lConfirm Factory Creation");

        // Fill border
        Material borderMat = Material.matchMaterial(
                plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // Factory info (center)
        Material icon = Material.matchMaterial(
                plugin.getConfig().getString("factory-types." + typeId + ".icon", "FURNACE"));
        if (icon == null) icon = Material.FURNACE;

        boolean canAfford = plugin.getEconomy().has(player, price);

        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Type: " + type.getDisplayName());
        infoLore.add("");
        infoLore.add("§7Region Size: §e" + sizeX + "x" + sizeY + "x" + sizeX);
        infoLore.add("§7Location: §e" + player.getLocation().getBlockX() + ", "
                + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ());
        infoLore.add("§7World: §e" + player.getWorld().getName());
        infoLore.add("");
        infoLore.add("§7Price: §6$" + String.format("%.2f", price));
        infoLore.add("§7Your Balance: §6$" + String.format("%.2f", plugin.getEconomy().getBalance(player)));
        if (canAfford) {
            infoLore.add("§7After Purchase: §6$" + String.format("%.2f", plugin.getEconomy().getBalance(player) - price));
        } else {
            infoLore.add("§c§lInsufficient funds!");
        }
        infoLore.add("");
        infoLore.add("§e§oThe region will be centered");
        infoLore.add("§e§oat your current position.");

        inv.setItem(13, createItem(icon, "§6" + type.getDisplayName() + " §7Factory", infoLore));

        // Confirm button
        ItemStack confirmItem;
        if (canAfford) {
            confirmItem = createItem(Material.LIME_WOOL, "§a§lConfirm Creation",
                    Arrays.asList("§7Click to create this factory!", "", "§6-$" + String.format("%.2f", price)));
        } else {
            confirmItem = createItem(Material.GRAY_WOOL, "§7§lCannot Create",
                    Arrays.asList("§cYou don't have enough money!"));
        }
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "confirm_create_factory_type"),
                    PersistentDataType.STRING, type.name());
            confirmItem.setItemMeta(confirmMeta);
        }
        inv.setItem(11, confirmItem);

        // Cancel button
        inv.setItem(15, createItem(Material.RED_WOOL, "§c§lCancel",
                Arrays.asList("§7Return to type selection")));

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        player.openInventory(inv);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
