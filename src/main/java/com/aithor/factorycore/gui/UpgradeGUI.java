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

public class UpgradeGUI {

    private final FactoryCore plugin;
    private final Player player;
    private final String factoryId;

    public UpgradeGUI(FactoryCore plugin, Player player, String factoryId) {
        this.plugin = plugin;
        this.player = player;
        this.factoryId = factoryId;
    }

    public void openUpgradeMenu() {
        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory == null) return;

        // Store factory ID in player's persistent data for click handling
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_factory_id"), PersistentDataType.STRING, factoryId);

        int maxLevel = plugin.getConfig().getInt("factory.max-level", 5);

        Inventory inv = Bukkit.createInventory(null, 27, "§6§lUpgrade Factory");

        // Fill with border
        Material borderMat = Material.matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        // Current level
        List<String> currentLore = new ArrayList<>();
        currentLore.add("§7Current Level");
        currentLore.add("");
        currentLore.add("§aCost reduction: §e" +
            (factory.getLevel() - 1) * plugin.getConfig().getDouble("factory.level-bonuses.cost-reduction") + "%");
        currentLore.add("§aTime reduction: §e" +
            (factory.getLevel() - 1) * plugin.getConfig().getDouble("factory.level-bonuses.time-reduction") + "%");

        inv.setItem(11, createItem(Material.DIAMOND,
            "§6Level " + factory.getLevel(), currentLore));

        if (factory.getLevel() < maxLevel) {
            // Next level
            int nextLevel = factory.getLevel() + 1;
            double upgradeCost = factory.getPrice() * 0.5 * factory.getLevel();

            List<String> nextLore = new ArrayList<>();
            nextLore.add("§7Next Level");
            nextLore.add("");
            nextLore.add("§7Upgrade cost: §6$" + String.format("%.2f", upgradeCost));
            nextLore.add("");
            nextLore.add("§7Next level bonuses:");
            nextLore.add("§a+ Cost reduction: §e" +
                plugin.getConfig().getDouble("factory.level-bonuses.cost-reduction") + "%");
            nextLore.add("§a+ Time reduction: §e" +
                plugin.getConfig().getDouble("factory.level-bonuses.time-reduction") + "%");
            nextLore.add("§c+ Additional tax: §e" +
                plugin.getConfig().getDouble("tax.level-multiplier") + "%");
            nextLore.add("");
            nextLore.add("§eClick to upgrade");

            inv.setItem(15, createItem(Material.EMERALD,
                "§aLevel " + nextLevel, nextLore));
        } else {
            inv.setItem(15, createItem(Material.BARRIER,
                "§cMax Level", Arrays.asList("§7Factory has reached max level")));
        }

        // Back button
        Material backMat = Material.matchMaterial(plugin.getConfig().getString("gui.back-item", "ARROW"));
        inv.setItem(26, createItem(backMat != null ? backMat : Material.ARROW, "§e§lBack", null));

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