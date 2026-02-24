package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.managers.AchievementManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * AchievementGUI - Custom GUI for viewing player achievements
 */
public class AchievementGUI {

    private final FactoryCore plugin;
    private final Player player;
    private int currentPage = 0;

    private static final int ITEMS_PER_PAGE = 21; // 3 rows x 7 columns
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm");

    public AchievementGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openAchievementMenu() {
        openAchievementMenu(0);
    }

    public void openAchievementMenu(int page) {
        this.currentPage = page;

        AchievementManager manager = plugin.getAchievementManager();
        List<String> allAchievements = new ArrayList<>(manager.getAchievementIds());
        int totalAchievements = allAchievements.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalAchievements / ITEMS_PER_PAGE));

        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        this.currentPage = page;

        int unlockedCount = manager.getUnlockedCount(player.getUniqueId());

        String title = "§6§lAchievements §8(" + unlockedCount + "/" + totalAchievements + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Fill with decorative border
        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);

        // Fill borders (top row, bottom row, sides)
        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9)
            inv.setItem(i, border);
        for (int i = 17; i < 45; i += 9)
            inv.setItem(i, border);

        // Progress header (slot 4)
        inv.setItem(4, createProgressItem(unlockedCount, totalAchievements));

        // Achievement items (slots: rows 1-3, columns 1-7 = slots 10-16, 19-25, 28-34)
        int startIndex = page * ITEMS_PER_PAGE;
        int slotIndex = 0;
        int[] contentSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        for (int i = startIndex; i < Math.min(startIndex + ITEMS_PER_PAGE, totalAchievements); i++) {
            if (slotIndex >= contentSlots.length) break;
            String achievementId = allAchievements.get(i);
            inv.setItem(contentSlots[slotIndex], createAchievementItem(achievementId));
            slotIndex++;
        }

        // Navigation
        // Previous page (slot 45)
        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW, "§e§lPrevious Page",
                    Arrays.asList("§7Page " + page + "/" + totalPages, "", "§7Click to go back")));
        }

        // Back to Hub (slot 49)
        inv.setItem(49, createItem(Material.ARROW, "§c§lBack to Hub",
                Arrays.asList("§7Return to main menu")));

        // Next page (slot 53)
        if (page < totalPages - 1) {
            inv.setItem(53, createItem(Material.ARROW, "§e§lNext Page",
                    Arrays.asList("§7Page " + (page + 2) + "/" + totalPages, "", "§7Click to continue")));
        }

        player.openInventory(inv);
    }

    private ItemStack createProgressItem(int unlocked, int total) {
        double percent = total > 0 ? (double) unlocked / total * 100 : 0;
        int barLength = 20;
        int filled = (int) (barLength * (percent / 100));

        StringBuilder progressBar = new StringBuilder("§a");
        for (int i = 0; i < barLength; i++) {
            if (i == filled) progressBar.append("§7");
            progressBar.append("|");
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7Your achievement progress");
        lore.add("");
        lore.add("§eUnlocked: §6" + unlocked + "§7/§6" + total);
        lore.add("§eProgress: §f" + String.format("%.1f", percent) + "%");
        lore.add("");
        lore.add(progressBar.toString());
        lore.add("");
        lore.add("§7Complete challenges to");
        lore.add("§7unlock new achievements!");

        return createItem(Material.NETHER_STAR, "§6§lAchievement Progress", lore);
    }

    private ItemStack createAchievementItem(String achievementId) {
        AchievementManager manager = plugin.getAchievementManager();
        boolean unlocked = manager.hasAchievement(player.getUniqueId(), achievementId);
        String name = manager.getAchievementName(achievementId);
        String description = manager.getAchievementDescription(achievementId);
        String type = manager.getAchievementType(achievementId);
        double threshold = manager.getAchievementThreshold(achievementId);
        double progress = manager.getProgress(player.getUniqueId(), achievementId);

        Material iconMaterial;
        if (unlocked) {
            try {
                iconMaterial = Material.valueOf(manager.getAchievementIcon(achievementId));
            } catch (IllegalArgumentException e) {
                iconMaterial = Material.PAPER;
            }
        } else {
            iconMaterial = Material.GRAY_DYE;
        }

        List<String> lore = new ArrayList<>();
        lore.add("");

        if (unlocked) {
            lore.add("§a§lUNLOCKED");
            lore.add("");
            lore.add("§7" + description);
            lore.add("");
            long timestamp = manager.getUnlockTimestamp(player.getUniqueId(), achievementId);
            if (timestamp > 0) {
                lore.add("§8Unlocked: " + DATE_FORMAT.format(new Date(timestamp)));
            }
        } else {
            lore.add("§c§lLOCKED");
            lore.add("");
            lore.add("§7" + description);

            // Show progress for cumulative achievements
            if (isCumulativeType(type) && threshold > 1) {
                lore.add("");
                double displayProgress = Math.min(progress, threshold);
                double percent = (displayProgress / threshold) * 100;

                if (type.contains("COST") || type.contains("EARNINGS") || type.contains("SALARY") || type.contains("TAX_PAID")) {
                    lore.add("§eProgress: §f$" + formatNumber(displayProgress) + " §7/ §f$" + formatNumber(threshold));
                } else {
                    lore.add("§eProgress: §f" + (int) displayProgress + " §7/ §f" + (int) threshold);
                }

                // Progress bar
                int barLength = 20;
                int filled = (int) (barLength * (percent / 100));
                StringBuilder bar = new StringBuilder("§a");
                for (int i = 0; i < barLength; i++) {
                    if (i == filled) bar.append("§7");
                    bar.append("|");
                }
                lore.add(bar.toString());
                lore.add("§8" + String.format("%.1f", percent) + "% complete");
            }
        }

        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = unlocked ? name : "§8§l???  §7" + name;
            meta.setDisplayName(displayName);
            meta.setLore(lore);

            if (unlocked) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private boolean isCumulativeType(String type) {
        return type != null && type.startsWith("CUMULATIVE_");
    }

    private String formatNumber(double value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000);
        } else if (value >= 1_000) {
            return String.format("%.1fK", value / 1_000);
        } else {
            return String.format("%.0f", value);
        }
    }

    public int getCurrentPage() {
        return currentPage;
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
