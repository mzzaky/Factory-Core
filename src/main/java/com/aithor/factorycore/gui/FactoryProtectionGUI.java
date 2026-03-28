package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.PlayerFactory;
import com.aithor.factorycore.models.ProtectionFlag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FactoryProtectionGUI {

    private final FactoryCore plugin;
    private final Player player;
    private final String factoryId;

    public FactoryProtectionGUI(FactoryCore plugin, Player player, String factoryId) {
        this.plugin = plugin;
        this.player = player;
        this.factoryId = factoryId;
    }

    public void open() {
        PlayerFactory factory = plugin.getPlayerFactoryManager() != null
                ? plugin.getPlayerFactoryManager().getFactory(factoryId) : null;
        if (factory == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return;
        }

        if (!factory.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-owned"));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, "§6§lFactory Protection");

        // Fill border
        Material borderMat = Material.matchMaterial(
                plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }

        // Info item at slot 4
        inv.setItem(4, createItem(Material.SHIELD, "§e§lFactory Protection",
                Arrays.asList(
                        "§7Toggle protection flags for",
                        "§7your factory region.",
                        "",
                        "§a● Green §7= Enabled (Protected)",
                        "§c● Red §7= Disabled (Unprotected)",
                        "",
                        "§7Click a flag to toggle it.")));

        // Place protection flag toggles
        ProtectionFlag[] flags = ProtectionFlag.values();
        int[] slots = {10, 11, 12, 13, 14, 15, 16,
                       19, 20, 21, 22, 23, 24, 25,
                       28, 29, 30, 31, 32, 33, 34};

        for (int i = 0; i < flags.length && i < slots.length; i++) {
            ProtectionFlag flag = flags[i];
            boolean enabled = factory.isProtectionEnabled(flag);
            inv.setItem(slots[i], createFlagItem(flag, enabled));
        }

        // Toggle All ON button (slot 38)
        inv.setItem(38, createTaggedItem(Material.LIME_DYE,
                "§a§lEnable All",
                Arrays.asList("§7Click to enable all", "§7protection flags."),
                "protection_action", "enable_all"));

        // Toggle All OFF button (slot 42)
        inv.setItem(42, createTaggedItem(Material.GRAY_DYE,
                "§c§lDisable All",
                Arrays.asList("§7Click to disable all", "§7protection flags."),
                "protection_action", "disable_all"));

        // Back button (slot 45)
        inv.setItem(45, createItem(Material.ARROW, "§c§lBack",
                Arrays.asList("§7Return to Factory Menu")));

        player.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String name = meta.getDisplayName();

        // Back button
        if (name.contains("Back")) {
            new MainMenuGUI(plugin, player, factoryId).openMainMenu();
            return;
        }

        PlayerFactory factory = plugin.getPlayerFactoryManager() != null
                ? plugin.getPlayerFactoryManager().getFactory(factoryId) : null;
        if (factory == null) return;

        // Check for bulk action tags
        String action = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "protection_action"), PersistentDataType.STRING);

        if ("enable_all".equals(action)) {
            for (ProtectionFlag flag : ProtectionFlag.values()) {
                factory.setProtectionFlag(flag, true);
            }
            plugin.getPlayerFactoryManager().saveAll();
            player.sendMessage(plugin.getLanguageManager().getMessage("protection-all-enabled"));
            open();
            return;
        }

        if ("disable_all".equals(action)) {
            for (ProtectionFlag flag : ProtectionFlag.values()) {
                factory.setProtectionFlag(flag, false);
            }
            plugin.getPlayerFactoryManager().saveAll();
            player.sendMessage(plugin.getLanguageManager().getMessage("protection-all-disabled"));
            open();
            return;
        }

        // Check for individual flag toggle
        String flagName = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "protection_flag"), PersistentDataType.STRING);

        if (flagName != null) {
            try {
                ProtectionFlag flag = ProtectionFlag.valueOf(flagName);
                factory.toggleProtectionFlag(flag);
                plugin.getPlayerFactoryManager().saveAll();

                boolean nowEnabled = factory.isProtectionEnabled(flag);
                String msg = nowEnabled
                        ? plugin.getLanguageManager().getMessage("protection-flag-enabled")
                        : plugin.getLanguageManager().getMessage("protection-flag-disabled");
                player.sendMessage(msg.replace("{flag}", flag.getDisplayName()));

                open(); // refresh GUI
            } catch (IllegalArgumentException ignored) {}
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ItemStack createFlagItem(ProtectionFlag flag, boolean enabled) {
        Material material = enabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;

        List<String> lore = new ArrayList<>();
        for (String line : flag.getDescription()) {
            lore.add("§7" + line);
        }
        lore.add("");
        lore.add(enabled ? "§aStatus: §lENABLED" : "§cStatus: §lDISABLED");
        lore.add("");
        lore.add("§eClick to toggle!");

        String displayName = (enabled ? "§a" : "§c") + "§l" + flag.getDisplayName();

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "protection_flag"),
                    PersistentDataType.STRING, flag.name());
            item.setItemMeta(meta);
        }
        return item;
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

    private ItemStack createTaggedItem(Material material, String name, List<String> lore,
                                       String tagKey, String tagValue) {
        ItemStack item = createItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, tagKey),
                    PersistentDataType.STRING, tagValue);
            item.setItemMeta(meta);
        }
        return item;
    }
}
