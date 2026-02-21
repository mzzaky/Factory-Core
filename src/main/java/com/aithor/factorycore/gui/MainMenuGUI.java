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

public class MainMenuGUI {

    private final FactoryCore plugin;
    private final Player player;
    private final String factoryId;

    public MainMenuGUI(FactoryCore plugin, Player player, String factoryId) {
        this.plugin = plugin;
        this.player = player;
        this.factoryId = factoryId;
    }

    public void openMainMenu() {
        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return;
        }

        if (!player.getUniqueId().equals(factory.getOwner())) {
            player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-owned"));
            return;
        }

        // Additional validation
        if (factory.getType() == null) {
            player.sendMessage("§cError: Invalid Factory type!");
            return;
        }

        // Store factory ID in player's persistent data for click handling
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_factory_id"),
                PersistentDataType.STRING, factoryId);

        Inventory inv = Bukkit.createInventory(null, 27,
                "§6§lFactory: §e" + factory.getType().getDisplayName());

        // Fill with border
        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        // Factory Status (Slot 4)
        inv.setItem(4, createFactoryStatusItem(factory));

        // Start Production (Slot 10)
        inv.setItem(10, createItem(Material.CRAFTING_TABLE,
                "§a§lStart Production",
                Arrays.asList(
                        "§7Click to start production",
                        "§7and select a recipe")));

        // Invoice Management (Slot 12)
        int invoiceCount = plugin.getInvoiceManager()
                .getInvoicesByOwner(player.getUniqueId()).size();
        inv.setItem(12, createItem(Material.PAPER,
                "§e§lManage Invoices",
                Arrays.asList(
                        "§7View and pay invoices",
                        "§7that are pending",
                        "",
                        "§eActive Invoices: §6" + invoiceCount)));

        // Factory Storage (Slot 14)
        int slots = plugin.getConfig().getInt("factory.base-storage-slots", 9);
        inv.setItem(14, createItem(Material.CHEST,
                "§b§lFactory Storage",
                Arrays.asList(
                        "§7Access factory storage",
                        "§7Slots: §e" + slots)));

        // Upgrade Factory (Slot 16)
        inv.setItem(16, createItem(Material.NETHER_STAR,
                "§d§lUpgrade Factory",
                Arrays.asList(
                        "§7Current level: §e" + factory.getLevel(),
                        "§7Click to upgrade")));

        // Fast Travel (Slot 22)
        inv.setItem(22, createItem(Material.ENDER_PEARL,
                "§6§lFast Travel",
                Arrays.asList(
                        "§7Teleport to the factory")));

        // Back Button (Slot 26)
        inv.setItem(26, createItem(Material.ARROW,
                "§c§lBack",
                Arrays.asList(
                        "§7Return to Hub")));

        player.openInventory(inv);
    }

    private ItemStack createFactoryStatusItem(Factory factory) {
        Material material = factory.getStatus() == FactoryStatus.RUNNING ? Material.GREEN_WOOL : Material.RED_WOOL;

        List<String> lore = new ArrayList<>();
        lore.add("§7Status: " + factory.getStatus().getDisplay());
        lore.add("§7Level: §e" + factory.getLevel());
        lore.add("§7Type: " + factory.getType().getDisplayName());

        if (factory.getCurrentProduction() != null) {
            ProductionTask task = factory.getCurrentProduction();
            Recipe recipe = plugin.getRecipeManager().getRecipe(task.getRecipeId());
            lore.add("");
            lore.add("§6Active Production:");
            lore.add("§e" + recipe.getName());
            lore.add("§7Time remaining: §e" + task.getRemainingTime() + "s");
            lore.add("§7Progress: §e" + (int) (task.getProgress() * 100) + "%");
        }

        return createItem(material, "§6§lFactory Status", lore);
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