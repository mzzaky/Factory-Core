package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.managers.ResearchManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * ResearchGUI - Custom GUI for the Factory Research system.
 * Displays all available research technologies, their current levels,
 * and allows players to start new research.
 */
public class ResearchGUI {

    private final FactoryCore plugin;
    private final Player player;

    public ResearchGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /**
     * Open the main Research Center GUI
     */
    public void openResearchMenu() {
        ResearchManager rm = plugin.getResearchManager();
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lResearch Center");

        // Fill with decorative border
        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);

        // Fill borders
        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9)
            inv.setItem(i, border);
        for (int i = 17; i < 45; i += 9)
            inv.setItem(i, border);

        // Header info (slot 4)
        inv.setItem(4, createResearchInfoHeader());

        // Research items in the center area
        // Slots: 20, 21, 22, 23, 24 (row 3) -> first 5 research items
        // 31 (row 4) -> 6th research item (centered)
        int[] researchSlots = { 20, 21, 22, 23, 24, 31 };
        List<String> researchIds = new ArrayList<>(rm.getResearchIds());

        for (int i = 0; i < researchIds.size() && i < researchSlots.length; i++) {
            String researchId = researchIds.get(i);
            inv.setItem(researchSlots[i], createResearchItem(researchId));
        }

        // Active Research Status (slot 49)
        inv.setItem(49, createActiveResearchItem());

        // Back to Hub button (slot 45)
        Material backMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.back-item", "ARROW"));
        inv.setItem(45, createItem(backMat != null ? backMat : Material.ARROW, "§e§lBack to Hub",
                Arrays.asList("§7Return to the main hub")));

        // Close button (slot 53)
        inv.setItem(53, createItem(Material.BARRIER, "§c§lClose", Arrays.asList("§7Click to close")));

        player.openInventory(inv);
    }

    /**
     * Open a detailed view / confirmation for a specific research
     */
    public void openResearchDetail(String researchId) {
        ResearchManager rm = plugin.getResearchManager();
        UUID playerId = player.getUniqueId();

        int currentLevel = rm.getPlayerResearchLevel(playerId, researchId);
        int maxLevel = rm.getMaxLevel(researchId);
        boolean isResearching = rm.isResearching(playerId, researchId);

        Inventory inv = Bukkit.createInventory(null, 27,
                "§6§lResearch: §e" + stripColor(rm.getResearchName(researchId)));

        // Fill with border
        Material borderMat = Material.matchMaterial(
                plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++)
            inv.setItem(i, border);

        // Research info (slot 13 - center)
        inv.setItem(13, createDetailedResearchItem(researchId));

        // Level progress bar (slots 10-12)
        for (int lvl = 1; lvl <= Math.min(3, maxLevel); lvl++) {
            inv.setItem(9 + lvl, createLevelIndicator(researchId, lvl, currentLevel));
        }

        // Level progress bar (slots 14-16) for levels 4+
        for (int lvl = 4; lvl <= Math.min(6, maxLevel); lvl++) {
            inv.setItem(10 + lvl, createLevelIndicator(researchId, lvl, currentLevel));
        }

        // Action buttons
        if (isResearching) {
            // Show progress
            int remaining = rm.getRemainingResearchTime(playerId, researchId);
            double progress = rm.getResearchProgress(playerId, researchId);
            int percent = (int) (progress * 100);

            List<String> progressLore = new ArrayList<>();
            progressLore.add("§eResearch in progress...");
            progressLore.add("");
            progressLore.add("§7Progress: §a" + percent + "%");
            progressLore.add("§7Remaining: §b" + rm.formatSeconds(remaining));
            progressLore.add("");
            progressLore.add(createProgressBar(progress));
            progressLore.add("");
            progressLore.add("§7Please wait until research is complete.");

            inv.setItem(22, createItem(Material.CLOCK, "§e§lResearching...", progressLore));

        } else if (currentLevel >= maxLevel) {
            inv.setItem(22, createItem(Material.BARRIER, "§c§lMax Level Reached",
                    Arrays.asList(
                            "§7This research has been",
                            "§7fully completed!",
                            "",
                            "§aAll buffs are active.")));
        } else {
            // Start research button
            int nextLevel = currentLevel + 1;
            double cost = rm.getResearchCost(researchId, nextLevel);
            int duration = rm.getResearchDuration(researchId, nextLevel);
            boolean canAfford = plugin.getEconomy().has(player, cost);

            List<String> confirmLore = new ArrayList<>();
            confirmLore.add("§7Research to level §6" + nextLevel);
            confirmLore.add("");
            confirmLore.add("§7Cost: " + (canAfford ? "§a" : "§c") + "$" + String.format("%,.2f", cost));
            confirmLore.add("§7Duration: §b" + rm.formatDuration(duration));
            confirmLore.add("");
            if (canAfford) {
                confirmLore.add("§a▸ Click to start research!");
            } else {
                confirmLore.add("§c✗ Insufficient funds!");
            }

            ItemStack confirmItem = createItem(
                    canAfford ? Material.GREEN_WOOL : Material.RED_WOOL,
                    canAfford ? "§a§lStart Research" : "§c§lInsufficient Funds",
                    confirmLore);

            // Store research ID in persistent data
            ItemMeta meta = confirmItem.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "confirm_research_id"),
                        PersistentDataType.STRING, researchId);
                confirmItem.setItemMeta(meta);
            }

            inv.setItem(22, confirmItem);
        }

        // Back button (slot 18)
        inv.setItem(18, createItem(Material.ARROW, "§e§lBack to Research Center",
                Arrays.asList("§7Return to research list")));

        player.openInventory(inv);
    }

    // ── Item Creation Helpers ────────────────────────────────────────────────

    private ItemStack createResearchInfoHeader() {
        ResearchManager rm = plugin.getResearchManager();
        UUID playerId = player.getUniqueId();

        List<String> lore = new ArrayList<>();
        lore.add("§7Unlock powerful buffs through");
        lore.add("§7research and development!");
        lore.add("");
        lore.add("§6Active Buffs:");

        // Show active buffs
        boolean hasBuffs = false;
        for (String researchId : rm.getResearchIds()) {
            int level = rm.getPlayerResearchLevel(playerId, researchId);
            if (level > 0) {
                double value = level * rm.getBuffPerLevel(researchId);
                String desc = rm.getBuffDescription(researchId)
                        .replace("{value}", String.format("%.0f", value));
                lore.add("§7• " + rm.getResearchName(researchId)
                        + " §8Lv." + level + " §7→ " + desc);
                hasBuffs = true;
            }
        }

        if (!hasBuffs) {
            lore.add("§8  No active research buffs");
        }

        return createItem(Material.ENCHANTING_TABLE, "§6§lResearch Center", lore);
    }

    private ItemStack createResearchItem(String researchId) {
        ResearchManager rm = plugin.getResearchManager();
        UUID playerId = player.getUniqueId();

        Material icon = Material.matchMaterial(rm.getResearchIcon(researchId));
        if (icon == null)
            icon = Material.PAPER;

        int currentLevel = rm.getPlayerResearchLevel(playerId, researchId);
        int maxLevel = rm.getMaxLevel(researchId);
        boolean isResearching = rm.isResearching(playerId, researchId);

        List<String> lore = new ArrayList<>(rm.getResearchDescription(researchId));
        lore.add("");
        lore.add("§7Level: §6" + currentLevel + "§7/§6" + maxLevel);

        // Show current buff
        if (currentLevel > 0) {
            double value = currentLevel * rm.getBuffPerLevel(researchId);
            String desc = rm.getBuffDescription(researchId)
                    .replace("{value}", String.format("%.0f", value));
            lore.add("§7Current: " + desc);
        }

        // Show status
        lore.add("");
        if (isResearching) {
            int remaining = rm.getRemainingResearchTime(playerId, researchId);
            double progress = rm.getResearchProgress(playerId, researchId);
            lore.add("§e⏳ Researching... §7(" + (int) (progress * 100) + "%)");
            lore.add("§7Time left: §b" + rm.formatSeconds(remaining));
            lore.add(createProgressBar(progress));
        } else if (currentLevel >= maxLevel) {
            lore.add("§a✔ §lMAX LEVEL REACHED");
        } else {
            int nextLevel = currentLevel + 1;
            double cost = rm.getResearchCost(researchId, nextLevel);
            int duration = rm.getResearchDuration(researchId, nextLevel);
            lore.add("§7Next Level Cost: §6$" + String.format("%,.2f", cost));
            lore.add("§7Research Time: §b" + rm.formatDuration(duration));
            lore.add("");
            lore.add("§eClick to view details!");
        }

        String displayName = rm.getResearchName(researchId);

        ItemStack item = createItem(icon, displayName, lore);

        // Enchant glow if completed or researching
        if (currentLevel > 0 || isResearching) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }

        // Store research ID for click handling
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "research_id"),
                    PersistentDataType.STRING, researchId);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createDetailedResearchItem(String researchId) {
        ResearchManager rm = plugin.getResearchManager();
        UUID playerId = player.getUniqueId();

        Material icon = Material.matchMaterial(rm.getResearchIcon(researchId));
        if (icon == null)
            icon = Material.PAPER;

        int currentLevel = rm.getPlayerResearchLevel(playerId, researchId);
        int maxLevel = rm.getMaxLevel(researchId);

        List<String> lore = new ArrayList<>(rm.getResearchDescription(researchId));
        lore.add("");
        lore.add("§6§lProgress: §e" + currentLevel + "§7/§6" + maxLevel);
        lore.add("");

        // Show all levels
        for (int lvl = 1; lvl <= maxLevel; lvl++) {
            double cost = rm.getResearchCost(researchId, lvl);
            double buffValue = lvl * rm.getBuffPerLevel(researchId);
            String buffDesc = rm.getBuffDescription(researchId)
                    .replace("{value}", String.format("%.0f", buffValue));

            String status;
            if (lvl <= currentLevel) {
                status = "§a✔";
            } else if (lvl == currentLevel + 1 && rm.isResearching(playerId, researchId)) {
                status = "§e⏳";
            } else {
                status = "§8✗";
            }

            lore.add(status + " §7Level " + lvl + " §8- §6$" + String.format("%,.2f", cost)
                    + " §8→ " + buffDesc);
        }

        ItemStack item = createItem(icon, rm.getResearchName(researchId), lore);

        // Enchant glow if any level completed
        if (currentLevel > 0) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    private ItemStack createLevelIndicator(String researchId, int level, int currentLevel) {
        ResearchManager rm = plugin.getResearchManager();
        UUID playerId = player.getUniqueId();
        boolean isResearchingThis = rm.isResearching(playerId, researchId);
        int targetLevel = currentLevel + 1;

        Material mat;
        String name;
        List<String> lore = new ArrayList<>();

        if (level <= currentLevel) {
            mat = Material.LIME_STAINED_GLASS_PANE;
            name = "§a§lLevel " + level + " §a✔";
            double buffValue = level * rm.getBuffPerLevel(researchId);
            String buffDesc = rm.getBuffDescription(researchId)
                    .replace("{value}", String.format("%.0f", buffValue));
            lore.add("§7Status: §aCompleted");
            lore.add("§7Total Buff: " + buffDesc);
        } else if (level == targetLevel && isResearchingThis) {
            mat = Material.YELLOW_STAINED_GLASS_PANE;
            name = "§e§lLevel " + level + " §e⏳";
            lore.add("§7Status: §eIn Progress");
            int remaining = rm.getRemainingResearchTime(playerId, researchId);
            lore.add("§7Time Left: §b" + rm.formatSeconds(remaining));
        } else {
            mat = Material.RED_STAINED_GLASS_PANE;
            name = "§c§lLevel " + level + " §c✗";
            double cost = rm.getResearchCost(researchId, level);
            lore.add("§7Status: §cLocked");
            lore.add("§7Cost: §6$" + String.format("%,.2f", cost));
            int duration = rm.getResearchDuration(researchId, level);
            lore.add("§7Duration: §b" + rm.formatDuration(duration));
        }

        return createItem(mat, name, lore);
    }

    private ItemStack createActiveResearchItem() {
        ResearchManager rm = plugin.getResearchManager();
        UUID playerId = player.getUniqueId();

        List<String> lore = new ArrayList<>();
        lore.add("§7Currently active research:");
        lore.add("");

        boolean hasActive = false;
        for (String researchId : rm.getResearchIds()) {
            if (rm.isResearching(playerId, researchId)) {
                int remaining = rm.getRemainingResearchTime(playerId, researchId);
                double progress = rm.getResearchProgress(playerId, researchId);
                lore.add("§e⏳ " + rm.getResearchName(researchId));
                lore.add("   §7Progress: §a" + (int) (progress * 100) + "%");
                lore.add("   §7Remaining: §b" + rm.formatSeconds(remaining));
                lore.add("   " + createProgressBar(progress));
                lore.add("");
                hasActive = true;
            }
        }

        if (!hasActive) {
            lore.add("§8  No active research");
            lore.add("");
            lore.add("§7Start a new research by");
            lore.add("§7clicking on any technology!");
        }

        return createItem(Material.BREWING_STAND, "§b§lActive Research", lore);
    }

    // ── Utility Helpers ─────────────────────────────────────────────────────

    /**
     * Create a visual progress bar
     */
    private String createProgressBar(double progress) {
        int bars = 20;
        int filled = (int) (progress * bars);
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                sb.append("§a█");
            } else {
                sb.append("§8░");
            }
        }
        sb.append("§8]");
        return sb.toString();
    }

    /**
     * Strip color codes from a string
     */
    private String stripColor(String input) {
        if (input == null)
            return "";
        return input.replaceAll("§[0-9a-fk-or]", "");
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
