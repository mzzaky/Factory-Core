package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Main Hub GUI - Central menu for all FactoryCore features
 */
public class HubGUI {

    private final FactoryCore plugin;
    private final Player player;

    public HubGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openHubMenu() {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lFactoryCore §8- §eMain Hub");

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

        // Player info header (slot 4)
        inv.setItem(4, createPlayerInfoItem());

        // ==================== MAIN MENU ITEMS ====================

        // Factory Browse (slot 20) - Browse available factories
        int availableFactories = (int) plugin.getFactoryManager().getAllFactories().stream()
                .filter(f -> f.getOwner() == null)
                .count();
        inv.setItem(20, createItem(Material.COMPASS,
                "§a§lFactory Browse",
                Arrays.asList(
                        "§7Browse and purchase",
                        "§7available factories",
                        "",
                        "§eAvailable: §a" + availableFactories + " factories",
                        "",
                        "§7Click to browse!")));

        // My Factories (slot 21) - View owned factories
        List<Factory> ownedFactories = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());
        long runningFactories = ownedFactories.stream()
                .filter(f -> f.getStatus() == FactoryStatus.RUNNING)
                .count();
        inv.setItem(21, createItem(Material.SMITHING_TABLE,
                "§6§lMy Factories",
                Arrays.asList(
                        "§7Manage your factories",
                        "",
                        "§eOwned: §6" + ownedFactories.size() + " factories",
                        "§eRunning: §a" + runningFactories,
                        "§eStopped: §c" + (ownedFactories.size() - runningFactories),
                        "",
                        "§7Click to manage!")));

        // Invoice Center (slot 22) - All invoices
        List<Invoice> allInvoices = plugin.getInvoiceManager().getInvoicesByOwner(player.getUniqueId());
        long overdueInvoices = allInvoices.stream().filter(Invoice::isOverdue).count();
        double totalDue = allInvoices.stream().mapToDouble(Invoice::getAmount).sum();
        inv.setItem(22, createItem(overdueInvoices > 0 ? Material.RED_CONCRETE : Material.PAPER,
                "§e§lInvoice Center",
                Arrays.asList(
                        "§7View and pay all invoices",
                        "",
                        "§ePending: §6" + allInvoices.size() + " invoices",
                        "§eOverdue: §c" + overdueInvoices,
                        "§eTotal Due: §6$" + String.format("%.2f", totalDue),
                        "",
                        overdueInvoices > 0 ? "§c§lWARNING: Overdue invoices!" : "§aAll invoices up to date",
                        "",
                        "§7Click to manage!")));

        // Tax Center (slot 23) - Tax management
        double totalTaxDue = plugin.getTaxManager() != null
                ? plugin.getTaxManager().getTotalTaxDue(player.getUniqueId())
                : 0.0;
        inv.setItem(23, createItem(Material.GOLD_INGOT,
                "§c§lTax Center",
                Arrays.asList(
                        "§7View and pay taxes",
                        "",
                        "§eTax Due: §6$" + String.format("%.2f", totalTaxDue),
                        "§eTax Rate: §e" + plugin.getConfig().getDouble("tax.rate", 5.0) + "%",
                        "",
                        "§7Click to manage!")));

        // Employees Center (slot 24) - NPC management
        List<FactoryNPC> playerNPCs = getPlayerNPCs();
        List<FactoryNPC> ownedNPCs = plugin.getNPCManager().getOwnedNPCsByOwner(player.getUniqueId());
        int unassigned = plugin.getNPCManager().getUnassignedNPCsByOwner(player.getUniqueId()).size();
        inv.setItem(24, createItem(Material.VILLAGER_SPAWN_EGG,
                "§b§lEmployees Center",
                Arrays.asList(
                        "§7Manage factory employees",
                        "",
                        "§eAssigned Employees: §b" + playerNPCs.size(),
                        "§eOwned (total): §b" + ownedNPCs.size(),
                        unassigned > 0 ? "§eUnassigned: §e" + unassigned : "§aAll employees assigned!",
                        "",
                        "§7Click to manage!")));

        // Marketplace (slot 29) - Buy/Sell resources
        int activeListings = plugin.getMarketplaceManager() != null
                ? plugin.getMarketplaceManager().getPlayerListings(player.getUniqueId()).size()
                : 0;
        inv.setItem(29, createItem(Material.EMERALD,
                "§2§lMarketplace",
                Arrays.asList(
                        "§7Buy and sell resources",
                        "",
                        "§eYour Listings: §2" + activeListings,
                        "",
                        "§7Click to browse!")));

        // Achievements (slot 30) - View achievements
        int unlockedAchievements = plugin.getAchievementManager() != null
                ? plugin.getAchievementManager().getUnlockedCount(player.getUniqueId())
                : 0;
        int totalAchievements = plugin.getAchievementManager() != null
                ? plugin.getAchievementManager().getTotalAchievementCount()
                : 0;
        inv.setItem(30, createItem(Material.DIAMOND,
                "§6§lAchievements",
                Arrays.asList(
                        "§7View your achievements",
                        "§7and track progress",
                        "",
                        "§eUnlocked: §6" + unlockedAchievements + "§7/§6" + totalAchievements,
                        "",
                        "§7Click to view!")));

        // Research Center (slot 31) - Research technologies
        long activeResearch = 0;
        int totalCompleted = 0;
        if (plugin.getResearchManager() != null) {
            for (String rId : plugin.getResearchManager().getResearchIds()) {
                if (plugin.getResearchManager().isResearching(player.getUniqueId(), rId)) {
                    activeResearch++;
                }
                totalCompleted += plugin.getResearchManager().getPlayerResearchLevel(player.getUniqueId(), rId);
            }
        }
        inv.setItem(31, createItem(Material.ENCHANTING_TABLE,
                "§5§lResearch Center",
                Arrays.asList(
                        "§7Unlock powerful buffs",
                        "§7through R&D technology",
                        "",
                        "§eActive Research: §d" + activeResearch,
                        "§eCompleted Levels: §a" + totalCompleted,
                        "",
                        "§7Click to research!")));

        // Help & Info (slot 33) - Plugin information
        inv.setItem(33, createItem(Material.BOOK,
                "§d§lHelp & Info",
                Arrays.asList(
                        "§7Learn about FactoryCore",
                        "",
                        "§7• Plugin guide",
                        "§7• Commands reference",
                        "§7• Tips & tricks",
                        "",
                        "§7Click to learn more!")));

        // Quick Stats (slot 49) - Player statistics
        inv.setItem(49, createQuickStatsItem());

        // Close button (slot 53)
        inv.setItem(53, createItem(Material.BARRIER, "§c§lClose", Arrays.asList("§7Click to close")));

        player.openInventory(inv);
    }

    private ItemStack createPlayerInfoItem() {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName("§6§l" + player.getName());

            List<String> lore = new ArrayList<>();
            lore.add("§7Welcome to FactoryCore!");
            lore.add("");

            List<Factory> owned = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());
            double totalValue = owned.stream().mapToDouble(Factory::getPrice).sum();

            lore.add("§eFactories Owned: §6" + owned.size());
            lore.add("§eTotal Value: §6$" + String.format("%.2f", totalValue));

            meta.setLore(lore);
            skull.setItemMeta(meta);
        }

        return skull;
    }

    private ItemStack createQuickStatsItem() {
        List<Factory> owned = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());

        long running = owned.stream().filter(f -> f.getStatus() == FactoryStatus.RUNNING).count();
        double totalValue = owned.stream().mapToDouble(Factory::getPrice).sum();

        List<String> lore = new ArrayList<>();
        lore.add("§7Your quick statistics");
        lore.add("");
        lore.add("§eFactories: §6" + owned.size());
        lore.add("§eRunning: §a" + running);
        lore.add("§eTotal Value: §6$" + String.format("%.2f", totalValue));

        // Factory type breakdown
        if (!owned.isEmpty()) {
            lore.add("");
            lore.add("§7Factory Types:");

            Map<FactoryType, Long> typeCounts = new HashMap<>();
            for (Factory f : owned) {
                typeCounts.merge(f.getType(), 1L, Long::sum);
            }

            for (Map.Entry<FactoryType, Long> entry : typeCounts.entrySet()) {
                lore.add("§7• " + entry.getKey().getDisplayName() + "§7: §e" + entry.getValue());
            }
        }

        return createItem(Material.NETHER_STAR, "§e§lQuick Stats", lore);
    }

    private List<FactoryNPC> getPlayerNPCs() {
        List<FactoryNPC> playerNPCs = new ArrayList<>();
        List<Factory> playerFactories = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());

        for (FactoryNPC npc : plugin.getNPCManager().getAllNPCs()) {
            // Skip unassigned purchased NPCs (factoryId is null)
            if (npc.getFactoryId() == null)
                continue;

            for (Factory factory : playerFactories) {
                if (npc.getFactoryId().equals(factory.getId())) {
                    playerNPCs.add(npc);
                    break;
                }
            }
        }

        return playerNPCs;
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
