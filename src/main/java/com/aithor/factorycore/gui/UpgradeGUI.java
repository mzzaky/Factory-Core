package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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

    // ── Private helper to resolve level/price from either factory type ──────────

    /**
     * Returns the factory level regardless of factory type. Returns -1 if not
     * found.
     */
    private int resolveLevel() {
        Factory f = plugin.getFactoryManager().getFactory(factoryId);
        if (f != null)
            return f.getLevel();
        if (plugin.getPlayerFactoryManager() != null) {
            com.aithor.factorycore.models.PlayerFactory pf = plugin.getPlayerFactoryManager().getFactory(factoryId);
            if (pf != null)
                return pf.getLevel();
        }
        return -1;
    }

    /**
     * Returns the factory price regardless of factory type. Returns 0 if not found.
     */
    private double resolvePrice() {
        Factory f = plugin.getFactoryManager().getFactory(factoryId);
        if (f != null)
            return f.getPrice();
        if (plugin.getPlayerFactoryManager() != null) {
            com.aithor.factorycore.models.PlayerFactory pf = plugin.getPlayerFactoryManager().getFactory(factoryId);
            if (pf != null)
                return pf.getPrice();
        }
        return 0;
    }

    /** Returns true if the factory is currently upgrading. */
    private boolean resolveIsUpgrading() {
        Factory f = plugin.getFactoryManager().getFactory(factoryId);
        if (f != null)
            return f.isUpgrading();
        if (plugin.getPlayerFactoryManager() != null) {
            com.aithor.factorycore.models.PlayerFactory pf = plugin.getPlayerFactoryManager().getFactory(factoryId);
            if (pf != null)
                return pf.isUpgrading();
        }
        return false;
    }

    /** Returns time remaining for upgrade (seconds). */
    private int resolveUpgradeRemainingSeconds() {
        Factory f = plugin.getFactoryManager().getFactory(factoryId);
        if (f != null)
            return f.getUpgradeRemainingSeconds();
        if (plugin.getPlayerFactoryManager() != null) {
            com.aithor.factorycore.models.PlayerFactory pf = plugin.getPlayerFactoryManager().getFactory(factoryId);
            if (pf != null)
                return pf.getUpgradeRemainingSeconds();
        }
        return 0;
    }

    public void openUpgradeMenu() {
        // Resolve from either admin factory or player factory
        boolean validFactory = resolveLevel() >= 0;
        if (!validFactory)
            return;

        int currentLevel = resolveLevel();
        double factoryPrice = resolvePrice();

        player.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "current_factory_id"),
                PersistentDataType.STRING, factoryId);

        int maxLevel = plugin.getConfig().getInt("factory.max-level", 5);

        Inventory inv = Bukkit.createInventory(null, 27, "§6§lUpgrade Factory");

        // Fill with border
        Material borderMat = Material.matchMaterial(
                plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(
                borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++)
            inv.setItem(i, border);

        // ── Current level panel ───────────────────────────────────────────────
        List<String> currentLore = new ArrayList<>();
        currentLore.add("§7Current Level");
        currentLore.add("");
        currentLore.add("§aCost reduction: §e" +
                (currentLevel - 1) * plugin.getConfig().getDouble("factory.level-bonuses.cost-reduction") + "%");
        currentLore.add("§aTime reduction: §e" +
                (currentLevel - 1) * plugin.getConfig().getDouble("factory.level-bonuses.time-reduction") + "%");
        currentLore.add("§aStorage slots: §e" +
                (plugin.getConfig().getInt("factory.base-storage-slots", 9) + (currentLevel - 1) * plugin.getConfig().getInt("factory.level-bonuses.slots-per-level", 9)));

        inv.setItem(11, createItem(Material.DIAMOND,
                "§6Level " + currentLevel, currentLore));

        // ── Next level panel ──────────────────────────────────────────────────
        if (currentLevel < maxLevel) {

            // ── Upgrading in progress ─────────────────────────────────────────
            if (resolveIsUpgrading()) {
                int remaining = resolveUpgradeRemainingSeconds();
                List<String> upgradingLore = new ArrayList<>();
                upgradingLore.add("§eUpgrade in progress...");
                upgradingLore.add("");
                upgradingLore.add("§7Time remaining: §a" + formatTime(remaining));
                upgradingLore.add("");
                upgradingLore.add("§7Please wait until the upgrade is complete.");

                inv.setItem(15, createItem(Material.CLOCK,
                        "§e§lUpgrading to Level " + (currentLevel + 1), upgradingLore));

            } else {
                // ── Normal next-level panel ───────────────────────────────────
                int nextLevel = currentLevel + 1;
                double upgradeCost = factoryPrice * 0.5 * currentLevel;
                int upgradeTimeSec = plugin.getConfig().getInt("factory.upgrade-time." + nextLevel, 60);

                // Realtime nano_construction_framework buff calculation
                int finalTimeSec = upgradeTimeSec;
                double timeReduction = 0;
                if (plugin.getResearchManager() != null) {
                    timeReduction = plugin.getResearchManager().getUpgradeTimeReduction(player.getUniqueId());
                    if (timeReduction > 0) {
                        finalTimeSec = (int) (finalTimeSec * (1 - (timeReduction / 100.0)));
                    }
                }

                List<String> nextLore = new ArrayList<>();
                nextLore.add("§7Next Level");
                nextLore.add("");
                nextLore.add("§7Upgrade cost: §6$" + String.format("%.2f", upgradeCost));

                if (timeReduction > 0) {
                    nextLore.add("§7Upgrade time: §c§m" + formatTime(upgradeTimeSec) + "§r §a"
                            + formatTime(finalTimeSec) + " §d(-" + String.format("%.0f", timeReduction) + "%)");
                } else {
                    nextLore.add("§7Upgrade time: §b" + formatTime(upgradeTimeSec));
                }

                nextLore.add("");
                nextLore.add("§7Next level bonuses:");
                nextLore.add("§a+ Cost reduction: §e" +
                        plugin.getConfig().getDouble("factory.level-bonuses.cost-reduction") + "%");
                nextLore.add("§a+ Time reduction: §e" +
                        plugin.getConfig().getDouble("factory.level-bonuses.time-reduction") + "%");
                nextLore.add("§a+ Output storage: §e" +
                        plugin.getConfig().getInt("factory.level-bonuses.slots-per-level", 9) + " slots");
                nextLore.add("§c+ Additional tax: §e" +
                        plugin.getConfig().getDouble("tax.level-multiplier") + "%");

                // Resource requirements
                String reqPath = "factory.upgrade-requirements." + nextLevel;
                if (plugin.getConfig().isConfigurationSection(reqPath)) {
                    ConfigurationSection reqSection = plugin.getConfig().getConfigurationSection(reqPath);
                    nextLore.add("");
                    nextLore.add("§7Required materials:");
                    for (String resourceId : reqSection.getKeys(false)) {
                        int required = reqSection.getInt(resourceId);
                        int have = countResourceInInventory(resourceId);
                        ResourceItem res = plugin.getResourceManager().getResource(resourceId);
                        String resName = (res != null) ? res.getName() : "§f" + resourceId;
                        String countColor = (have >= required) ? "§a" : "§c";
                        nextLore.add("§8• " + resName + " §7(" + countColor + have + "§7/§e" + required + "§7)");
                    }
                }

                nextLore.add("");
                nextLore.add("§eClick to upgrade");

                inv.setItem(15, createItem(Material.EMERALD,
                        "§aLevel " + nextLevel, nextLore));
            }

        } else {
            inv.setItem(15, createItem(Material.BARRIER,
                    "§cMax Level", Arrays.asList("§7Factory has reached max level")));
        }

        // Back button
        Material backMat = Material.matchMaterial(
                plugin.getConfig().getString("gui.back-item", "ARROW"));
        inv.setItem(26, createItem(backMat != null ? backMat : Material.ARROW, "§e§lBack", null));

        player.openInventory(inv);
    }

    /**
     * Opens a confirmation GUI before the upgrade is actually started.
     */
    public void openUpgradeConfirm() {
        // Resolve from either admin factory or player factory
        int currentLevel = resolveLevel();
        if (currentLevel < 0)
            return;
        double factoryPrice = resolvePrice();

        int nextLevel = currentLevel + 1;
        double upgradeCost = factoryPrice * 0.5 * currentLevel;
        int upgradeTimeSec = plugin.getConfig().getInt("factory.upgrade-time." + nextLevel, 60);

        // Realtime nano_construction_framework buff calculation
        int finalTimeSec = upgradeTimeSec;
        double timeReduction = 0;
        if (plugin.getResearchManager() != null) {
            timeReduction = plugin.getResearchManager().getUpgradeTimeReduction(player.getUniqueId());
            if (timeReduction > 0) {
                finalTimeSec = (int) (finalTimeSec * (1 - (timeReduction / 100.0)));
            }
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§6§lConfirm Upgrade");

        // Border
        Material borderMat = Material.matchMaterial(
                plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(
                borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++)
            inv.setItem(i, border);

        // Summary item (centre)
        List<String> summaryLore = new ArrayList<>();
        summaryLore.add("§7Upgrading to: §6Level " + nextLevel);
        summaryLore.add("");
        summaryLore.add("§7Cost: §6$" + String.format("%.2f", upgradeCost));

        if (timeReduction > 0) {
            summaryLore.add("§7Time: §c§m" + formatTime(upgradeTimeSec) + "§r §a" + formatTime(finalTimeSec) + " §d(-"
                    + String.format("%.0f", timeReduction) + "%)");
        } else {
            summaryLore.add("§7Time: §b" + formatTime(upgradeTimeSec));
        }

        summaryLore.add("§7Bonus: §a+" + plugin.getConfig().getInt("factory.level-bonuses.slots-per-level", 9) + " Storage Slots");
        summaryLore.add("");

        // Show required resources
        String reqPath = "factory.upgrade-requirements." + nextLevel;
        if (plugin.getConfig().isConfigurationSection(reqPath)) {
            ConfigurationSection reqSection = plugin.getConfig().getConfigurationSection(reqPath);
            summaryLore.add("§7Materials consumed:");
            for (String resourceId : reqSection.getKeys(false)) {
                int required = reqSection.getInt(resourceId);
                ResourceItem res = plugin.getResourceManager().getResource(resourceId);
                String resName = (res != null) ? res.getName() : "§f" + resourceId;
                summaryLore.add("§8• " + resName + " §7x§e" + required);
            }
            summaryLore.add("");
        }

        summaryLore.add("§cThis action cannot be undone!");
        inv.setItem(13, createItem(Material.NETHER_STAR, "§6§lUpgrade Summary", summaryLore));

        // Confirm (green wool, slot 11)
        inv.setItem(11, createItem(Material.GREEN_WOOL, "§a§lConfirm Upgrade",
                Arrays.asList("§7Click to start the upgrade.")));

        // Cancel (red wool, slot 15)
        inv.setItem(15, createItem(Material.RED_WOOL, "§c§lCancel",
                Arrays.asList("§7Return to Upgrade menu.")));

        player.openInventory(inv);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private int countResourceInInventory(String resourceId) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR)
                continue;
            String id = plugin.getResourceManager().getResourceId(item);
            if (resourceId.equals(id))
                total += item.getAmount();
        }
        return total;
    }

    /** Format seconds into Xm Ys or Xs string. */
    private String formatTime(int totalSeconds) {
        if (totalSeconds >= 60) {
            int m = totalSeconds / 60;
            int s = totalSeconds % 60;
            return s > 0 ? m + "m " + s + "s" : m + "m";
        }
        return totalSeconds + "s";
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