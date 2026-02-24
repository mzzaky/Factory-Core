package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.managers.DailyQuestManager;
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
 * DailyQuestGUI - Custom GUI for viewing and claiming daily quest rewards
 */
public class DailyQuestGUI {

    private final FactoryCore plugin;
    private final Player player;

    // Quest display slots (centered in row 2, columns 1-5)
    private static final int[] QUEST_SLOTS = {20, 21, 22, 23, 24};

    public DailyQuestGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openDailyQuestMenu() {
        DailyQuestManager manager = plugin.getDailyQuestManager();
        List<String> allQuests = new ArrayList<>(manager.getQuestIds());
        int totalQuests = allQuests.size();
        int completedCount = manager.getCompletedCount(player.getUniqueId());

        String title = "§6§lDaily Quests §8(" + completedCount + "/" + totalQuests + ")";
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
        inv.setItem(4, createProgressHeader(completedCount, totalQuests));

        // Quest items (row 2, slots 20-24)
        for (int i = 0; i < Math.min(allQuests.size(), QUEST_SLOTS.length); i++) {
            String questId = allQuests.get(i);
            inv.setItem(QUEST_SLOTS[i], createQuestItem(questId));
        }

        // All-Complete Bonus Reward (slot 31, center of row 3)
        if (manager.isBonusEnabled()) {
            inv.setItem(31, createBonusItem());
        }

        // Claim All button (slot 40) - only if there are unclaimed completed quests
        boolean hasUnclaimedQuests = false;
        for (String questId : allQuests) {
            if (manager.isQuestCompleted(player.getUniqueId(), questId)
                    && !manager.isRewardClaimed(player.getUniqueId(), questId)) {
                hasUnclaimedQuests = true;
                break;
            }
        }

        if (hasUnclaimedQuests) {
            inv.setItem(40, createItem(Material.HOPPER, "§a§lClaim All Rewards",
                    Arrays.asList(
                            "§7Claim all completed quest",
                            "§7rewards at once!",
                            "",
                            "§eClick to claim all!")));
        }

        // Back to Hub (slot 49)
        inv.setItem(49, createItem(Material.ARROW, "§c§lBack to Hub",
                Arrays.asList("§7Return to main menu")));

        player.openInventory(inv);
    }

    private ItemStack createProgressHeader(int completed, int total) {
        DailyQuestManager manager = plugin.getDailyQuestManager();
        double percent = total > 0 ? (double) completed / total * 100 : 0;
        int barLength = 20;
        int filled = (int) (barLength * (percent / 100));

        StringBuilder progressBar = new StringBuilder("§a");
        for (int i = 0; i < barLength; i++) {
            if (i == filled) progressBar.append("§7");
            progressBar.append("|");
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7Complete daily quests to");
        lore.add("§7earn EXP and money rewards!");
        lore.add("");
        lore.add("§eCompleted: §6" + completed + "§7/§6" + total);
        lore.add("§eProgress: §f" + String.format("%.0f", percent) + "%");
        lore.add("");
        lore.add(progressBar.toString());
        lore.add("");
        lore.add("§eResets in: §b" + manager.formatTimeRemaining());
        lore.add("");
        lore.add("§7Quests reset daily at midnight!");

        return createItem(Material.CLOCK, "§6§lDaily Quest Progress", lore);
    }

    private ItemStack createQuestItem(String questId) {
        DailyQuestManager manager = plugin.getDailyQuestManager();
        UUID playerId = player.getUniqueId();

        boolean completed = manager.isQuestCompleted(playerId, questId);
        boolean claimed = manager.isRewardClaimed(playerId, questId);
        String name = manager.getQuestName(questId);
        String description = manager.getQuestDescription(questId);
        int progress = manager.getProgress(playerId, questId);
        int target = manager.getQuestTarget(questId);
        int rewardExp = manager.getQuestRewardExp(questId);
        double rewardMoney = manager.getQuestRewardMoney(questId);

        Material iconMaterial;
        if (claimed) {
            iconMaterial = Material.LIME_DYE;
        } else if (completed) {
            try {
                iconMaterial = Material.valueOf(manager.getQuestIcon(questId));
            } catch (IllegalArgumentException e) {
                iconMaterial = Material.PAPER;
            }
        } else {
            try {
                iconMaterial = Material.valueOf(manager.getQuestIcon(questId));
            } catch (IllegalArgumentException e) {
                iconMaterial = Material.PAPER;
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7" + description);
        lore.add("");

        if (claimed) {
            lore.add("§a§lCOMPLETED & CLAIMED");
            lore.add("");
            lore.add("§8Reward already claimed!");
        } else if (completed) {
            lore.add("§a§lCOMPLETED!");
            lore.add("");
            lore.add("§eProgress: §a" + progress + "§7/§a" + target + " §a(100%)");
            lore.add("");
            lore.add("§6Rewards:");
            if (rewardExp > 0) lore.add("§7  +" + rewardExp + " EXP");
            if (rewardMoney > 0) lore.add("§7  +$" + String.format("%.2f", rewardMoney));
            lore.add("");
            lore.add("§e>>> Click to claim reward! <<<");
        } else {
            lore.add("§c§lIN PROGRESS");
            lore.add("");

            // Progress bar
            double progressPercent = target > 0 ? (double) progress / target * 100 : 0;
            int barLength = 20;
            int filled = (int) (barLength * (progressPercent / 100));
            StringBuilder bar = new StringBuilder("§a");
            for (int i = 0; i < barLength; i++) {
                if (i == filled) bar.append("§7");
                bar.append("|");
            }

            lore.add("§eProgress: §f" + progress + "§7/§f" + target);
            lore.add(bar.toString());
            lore.add("§8" + String.format("%.1f", progressPercent) + "% complete");
            lore.add("");
            lore.add("§6Rewards:");
            if (rewardExp > 0) lore.add("§7  +" + rewardExp + " EXP");
            if (rewardMoney > 0) lore.add("§7  +$" + String.format("%.2f", rewardMoney));
        }

        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName;
            if (claimed) {
                displayName = "§a§m" + ChatColorStrip(name) + " §a(Claimed)";
            } else if (completed) {
                displayName = "§a" + ChatColorStrip(name) + " §e(Click to Claim!)";
            } else {
                displayName = name;
            }
            meta.setDisplayName(displayName);
            meta.setLore(lore);

            // Store quest ID in persistent data for click handling
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "daily_quest_id"),
                    PersistentDataType.STRING, questId);

            if (completed && !claimed) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createBonusItem() {
        DailyQuestManager manager = plugin.getDailyQuestManager();
        UUID playerId = player.getUniqueId();

        boolean allCompleted = manager.areAllQuestsCompleted(playerId);
        boolean bonusClaimed = manager.isBonusClaimed(playerId);
        int bonusExp = manager.getBonusExp();
        double bonusMoney = manager.getBonusMoney();

        Material material;
        List<String> lore = new ArrayList<>();

        if (bonusClaimed) {
            material = Material.LIME_STAINED_GLASS;
            lore.add("");
            lore.add("§a§lBONUS CLAIMED!");
            lore.add("");
            lore.add("§8Come back tomorrow for new quests!");
        } else if (allCompleted) {
            material = Material.NETHER_STAR;
            lore.add("");
            lore.add("§a§lALL QUESTS COMPLETED!");
            lore.add("");
            lore.add("§6Bonus Rewards:");
            if (bonusExp > 0) lore.add("§e  +" + bonusExp + " EXP");
            if (bonusMoney > 0) lore.add("§6  +$" + String.format("%.2f", bonusMoney));
            lore.add("");
            lore.add("§e>>> Click to claim bonus! <<<");
        } else {
            material = Material.GRAY_STAINED_GLASS;
            lore.add("");
            lore.add("§c§lLOCKED");
            lore.add("");
            lore.add("§7Complete all daily quests");
            lore.add("§7to unlock the bonus reward!");
            lore.add("");
            int remaining = manager.getTotalQuestCount() - manager.getCompletedCount(playerId);
            lore.add("§eRemaining: §c" + remaining + " quests");
            lore.add("");
            lore.add("§6Bonus Rewards:");
            if (bonusExp > 0) lore.add("§7  +" + bonusExp + " EXP");
            if (bonusMoney > 0) lore.add("§7  +$" + String.format("%.2f", bonusMoney));
        }

        String displayName;
        if (bonusClaimed) {
            displayName = "§a§lAll-Complete Bonus §7(Claimed)";
        } else if (allCompleted) {
            displayName = "§6§lAll-Complete Bonus §e(Click!)";
        } else {
            displayName = "§8§lAll-Complete Bonus §7(Locked)";
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);

            if (allCompleted && !bonusClaimed) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Strip color codes from a string for display purposes
     */
    private String ChatColorStrip(String input) {
        return org.bukkit.ChatColor.stripColor(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', input));
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
