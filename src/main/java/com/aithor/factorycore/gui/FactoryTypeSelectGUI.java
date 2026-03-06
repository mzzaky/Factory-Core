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
 * GUI that shows all available factory types for the player to choose when
 * creating their own factory.
 */
public class FactoryTypeSelectGUI {

    private final FactoryCore plugin;
    private final Player player;

    public FactoryTypeSelectGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(null, 27, "§6§lCreate Factory §8- §eSelect Type");

        // Fill borders
        Material borderMat = Material.matchMaterial(
                plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // Header
        inv.setItem(4, createItem(Material.ANVIL, "§6§lSelect Factory Type",
                Arrays.asList(
                        "§7Choose the type of factory",
                        "§7you want to create.",
                        "",
                        "§7The factory region will be",
                        "§7created at your current position.")));

        // Factory types
        int[] slots = {10, 11, 12, 13, 14};
        FactoryType[] types = FactoryType.values();
        for (int i = 0; i < types.length && i < slots.length; i++) {
            FactoryType type = types[i];
            String typeId = type.getId();

            double price = plugin.getConfig().getDouble("player-factory.creation-prices." + typeId,
                    plugin.getConfig().getDouble("factory-types." + typeId + ".base-price", 100000.0));

            Material icon = Material.matchMaterial(
                    plugin.getConfig().getString("factory-types." + typeId + ".icon", "FURNACE"));
            if (icon == null) icon = Material.FURNACE;

            String desc = plugin.getConfig().getString("factory-types." + typeId + ".description", "§7A factory");

            boolean canAfford = plugin.getEconomy().has(player, price);

            List<String> lore = new ArrayList<>();
            lore.add(desc);
            lore.add("");
            lore.add("§7Creation Price: §6$" + String.format("%.2f", price));
            lore.add("");
            if (canAfford) {
                lore.add("§a§lClick to select!");
            } else {
                lore.add("§c§lInsufficient funds!");
                lore.add("§7You need §6$" + String.format("%.2f", price - plugin.getEconomy().getBalance(player)) + " §7more");
            }

            ItemStack item = createItem(icon, type.getDisplayName(), lore);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "create_factory_type"),
                        PersistentDataType.STRING, type.name());
                item.setItemMeta(meta);
            }
            inv.setItem(slots[i], item);
        }

        // Back button
        inv.setItem(22, createItem(Material.ARROW, "§c§lBack",
                Arrays.asList("§7Return to Factory Browse")));

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
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
