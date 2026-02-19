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
import java.util.stream.Collectors;

/**
 * Employees Center GUI - Manage factory NPC employees
 * Players can:
 * - View their factories and which employees are assigned
 * - Open the Employee Shop to purchase new employees
 * - Assign/unassign purchased employees to/from factories
 * - Fire (dismiss) employees permanently
 */
public class EmployeesCenterGUI {

    private final FactoryCore plugin;
    private final Player player;
    private int currentPage = 0;

    public EmployeesCenterGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openEmployeesCenterMenu() {
        openEmployeesCenterMenu(0);
    }

    public void openEmployeesCenterMenu(int page) {
        this.currentPage = page;

        Inventory inv = Bukkit.createInventory(null, 54, "§b§lEmployees Center §8- §ePage " + (page + 1));

        // Get player's factories and their NPCs
        List<Factory> playerFactories = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());
        List<FactoryNPC> ownedNPCs = plugin.getNPCManager().getOwnedNPCsByOwner(player.getUniqueId());
        int unassignedCount = plugin.getNPCManager().getUnassignedNPCsByOwner(player.getUniqueId()).size();

        // Fill borders
        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);

        // Header info
        List<String> headerLore = new ArrayList<>();
        headerLore.add("§7Manage your factory employees");
        headerLore.add("");
        headerLore.add("§eOwned Employees: §b" + ownedNPCs.size());
        headerLore.add("§eUnassigned Employees: §e" + unassignedCount);
        headerLore.add("§eFactories with Employees: §b" + getFactoriesWithEmployees());
        headerLore.add("§eFactories without Employees: §c" + (playerFactories.size() - getFactoriesWithEmployees()));
        headerLore.add("");
        headerLore.add("§7Employees boost production speed");
        headerLore.add("§7and are required to start production.");

        inv.setItem(4, createItem(Material.VILLAGER_SPAWN_EGG, "§b§lEmployees Center", headerLore));

        // Factory/NPC slots (slots 9-44)
        int slotsPerPage = 36;
        int startIndex = page * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, playerFactories.size());

        int slot = 9;
        for (int i = startIndex; i < endIndex && slot < 45; i++) {
            Factory factory = playerFactories.get(i);
            inv.setItem(slot++, createFactoryEmployeeItem(factory));
        }

        // Show message if no factories
        if (playerFactories.isEmpty()) {
            inv.setItem(22, createItem(Material.BARRIER, "§c§lNo Factories",
                    Arrays.asList(
                            "§7You don't own any factories!",
                            "",
                            "§7Purchase a factory first to",
                            "§7hire employees.")));
        }

        // Navigation and control buttons

        // Previous page (slot 45)
        if (page > 0) {
            inv.setItem(45, createNavigationItem(Material.ARROW, "§e§l◄ Previous Page", page - 1));
        }

        // Employee Shop button (slot 46)
        List<String> shopLore = new ArrayList<>();
        shopLore.add("§7Browse and purchase NPC employees.");
        shopLore.add("§7Each employee provides a unique");
        shopLore.add("§7production time reduction buff.");
        shopLore.add("");
        shopLore.add("§eClick to open Employee Shop!");
        inv.setItem(46, createItem(Material.GOLD_INGOT, "§6§lEmployee Shop", shopLore));

        // My Employees button (slot 47)
        List<String> myEmpLore = new ArrayList<>();
        myEmpLore.add("§7View all employees you own.");
        myEmpLore.add("§7Assign unassigned employees");
        myEmpLore.add("§7to your factories.");
        myEmpLore.add("");
        myEmpLore.add("§eOwned: §b" + ownedNPCs.size() + " §7| §eUnassigned: §e" + unassignedCount);
        myEmpLore.add("");
        myEmpLore.add("§eClick to manage!");
        inv.setItem(47, createItem(Material.PLAYER_HEAD, "§b§lMy Employees", myEmpLore));

        // Employee Info (slot 49)
        inv.setItem(49, createEmployeeInfoItem());

        // Page info (slot 50)
        int totalPages = (int) Math.ceil((double) playerFactories.size() / slotsPerPage);
        inv.setItem(50, createItem(Material.PAPER, "§e§lPage Info",
                Arrays.asList(
                        "§7Page: §e" + (page + 1) + " / " + Math.max(1, totalPages),
                        "§7Factories: §e" + playerFactories.size())));

        // Back to hub (slot 51)
        inv.setItem(51, createItem(Material.DARK_OAK_DOOR, "§c§lBack to Hub",
                Arrays.asList("§7Return to main menu")));

        // Next page (slot 53)
        if (endIndex < playerFactories.size()) {
            inv.setItem(53, createNavigationItem(Material.ARROW, "§e§lNext Page ►", page + 1));
        }

        player.openInventory(inv);
    }

    private ItemStack createFactoryEmployeeItem(Factory factory) {
        // Check for purchased (player-assigned) NPC first
        FactoryNPC assignedNPC = plugin.getNPCManager().getAssignedNPCForFactory(factory.getId());
        // Also check for admin-spawned NPC
        FactoryNPC anyNPC = plugin.getNPCManager().getAnyNPCForFactory(factory.getId());
        boolean hasEmployee = anyNPC != null;
        boolean isPurchased = assignedNPC != null;

        Material material = hasEmployee ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE;

        List<String> lore = new ArrayList<>();
        lore.add("§7Factory: §e" + factory.getId());
        lore.add("§7Type: " + factory.getType().getDisplayName());
        lore.add("§7Level: §b" + factory.getLevel());
        lore.add("");

        if (hasEmployee) {
            FactoryNPC npc = isPurchased ? assignedNPC : anyNPC;
            lore.add("§a✓ Employee Assigned");
            lore.add("");
            lore.add("§7Employee: §b" + npc.getName());
            if (isPurchased) {
                lore.add("§7Type: §e" + npc.getNpcTypeId());
                lore.add("§7Production Buff: §a-" + npc.getProductionTimeReduction() + "% §7time");
            } else {
                lore.add("§7(Admin-spawned employee)");
            }
            lore.add("§7Location: §e" + formatLocation(npc.getLocation()));
            lore.add("");
            if (isPurchased) {
                lore.add("§eLeft Click: §7Teleport to Employee");
                lore.add("§eRight Click: §7Unassign Employee");
                lore.add("§eShift+Right: §7Dismiss Employee (permanent)");
            } else {
                lore.add("§eLeft Click: §7Teleport to Employee");
                lore.add("§eRight Click: §7Fire Employee");
            }
        } else {
            lore.add("§c✗ No Employee Assigned");
            lore.add("");
            lore.add("§cProduction is BLOCKED!");
            lore.add("§7Assign an employee to enable");
            lore.add("§7production at this factory.");
            lore.add("");
            lore.add("§7Buy employees from the §6Employee Shop§7.");
            lore.add("");
            lore.add("§eClick: §7Open Employee Shop");
        }

        String title = (hasEmployee ? "§a" : "§7") + factory.getType().getDisplayName() + " §7- §e" + factory.getId();
        ItemStack item = createItem(material, title, lore);

        // Store factory ID for click handling
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "employee_factory_id"),
                    PersistentDataType.STRING,
                    factory.getId());
            if (hasEmployee) {
                FactoryNPC npc = isPurchased ? assignedNPC : anyNPC;
                meta.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "employee_npc_id"),
                        PersistentDataType.STRING,
                        npc.getId());
                // Flag to distinguish purchased vs admin-spawned
                meta.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "employee_is_purchased"),
                        PersistentDataType.INTEGER,
                        isPurchased ? 1 : 0);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createEmployeeInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7About Factory Employees");
        lore.add("");
        lore.add("§7Employees are NPCs that:");
        lore.add("§7• §aEnable factory production");
        lore.add("§7• §aReduce production time");
        lore.add("§7• §aAllow output storage access");
        lore.add("");
        lore.add("§7§nHow to get an Employee:§r");
        lore.add("§7• Open the §6Employee Shop");
        lore.add("§7• Purchase an employee");
        lore.add("§7• Assign them to your factory");
        lore.add("");
        lore.add("§7§nEmployee Tiers:§r");
        lore.add("§7• §fApprentice §7(-5% time)");
        lore.add("§7• §eSkilled §7(-15% time)");
        lore.add("§7• §cForger §7(-20% time)");
        lore.add("§7• §eOperator §7(-25% time)");
        lore.add("§7• §5Master §7(-35% time)");

        return createItem(Material.BOOK, "§b§lEmployee Guide", lore);
    }

    private List<FactoryNPC> getPlayerNPCs() {
        List<FactoryNPC> playerNPCs = new ArrayList<>();
        List<Factory> playerFactories = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());
        Set<String> playerFactoryIds = playerFactories.stream()
                .map(Factory::getId)
                .collect(Collectors.toSet());

        for (FactoryNPC npc : plugin.getNPCManager().getAllNPCs()) {
            if (playerFactoryIds.contains(npc.getFactoryId())) {
                playerNPCs.add(npc);
            }
        }

        return playerNPCs;
    }

    private int getFactoriesWithEmployees() {
        List<Factory> playerFactories = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());
        int count = 0;
        for (Factory factory : playerFactories) {
            if (plugin.getNPCManager().factoryHasEmployee(factory.getId())) {
                count++;
            }
        }
        return count;
    }

    private String formatLocation(org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null)
            return "Unknown";
        return String.format("%s (%d, %d, %d)",
                loc.getWorld().getName(),
                (int) loc.getX(),
                (int) loc.getY(),
                (int) loc.getZ());
    }

    private ItemStack createNavigationItem(Material material, String name, int targetPage) {
        ItemStack item = createItem(material, name, Arrays.asList("§7Go to page " + (targetPage + 1)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "employee_center_page"),
                    PersistentDataType.INTEGER,
                    targetPage);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── Confirmation GUIs ────────────────────────────────────────────────────

    /**
     * Open confirmation for unassigning (removing from factory but keeping) a
     * purchased employee.
     */
    public void openUnassignConfirmation(String npcId) {
        FactoryNPC npc = plugin.getNPCManager().getNPC(npcId);
        if (npc == null) {
            player.sendMessage("§cEmployee not found!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§e§lConfirm Unassign Employee");

        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++)
            inv.setItem(i, border);

        // NPC info (slot 13)
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Name: §b" + npc.getName());
        infoLore.add("§7Factory: §e" + npc.getFactoryId());
        infoLore.add("§7Production Buff: §a-" + npc.getProductionTimeReduction() + "%");
        infoLore.add("");
        infoLore.add("§eThis will unassign the employee.");
        infoLore.add("§7The employee will return to your");
        infoLore.add("§7unassigned pool (not deleted).");
        infoLore.add("§cProduction will be blocked until");
        infoLore.add("§ca new employee is assigned.");
        inv.setItem(13, createItem(Material.VILLAGER_SPAWN_EGG, "§b" + npc.getName(), infoLore));

        // Confirm button (slot 11)
        ItemStack confirmItem = createItem(Material.LIME_WOOL, "§a§lConfirm Unassign",
                Arrays.asList("§7Click to unassign this employee"));
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "confirm_unassign_npc"),
                    PersistentDataType.STRING, npcId);
            confirmItem.setItemMeta(confirmMeta);
        }
        inv.setItem(11, confirmItem);

        // Cancel button (slot 15)
        inv.setItem(15, createItem(Material.RED_WOOL, "§c§lCancel",
                Arrays.asList("§7Return to Employees Center")));

        player.openInventory(inv);
    }

    /**
     * Open confirmation for dismissing (permanently removing) a purchased employee.
     */
    public void openDismissConfirmation(String npcId) {
        FactoryNPC npc = plugin.getNPCManager().getNPC(npcId);
        if (npc == null) {
            player.sendMessage("§cEmployee not found!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§c§lConfirm Dismiss Employee");

        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++)
            inv.setItem(i, border);

        // NPC info (slot 13)
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Name: §b" + npc.getName());
        infoLore.add("§7Factory: §e" + (npc.getFactoryId() != null ? npc.getFactoryId() : "Unassigned"));
        infoLore.add("§7Production Buff: §a-" + npc.getProductionTimeReduction() + "%");
        infoLore.add("");
        infoLore.add("§c§lWARNING: This is PERMANENT!");
        infoLore.add("§7The employee will be permanently");
        infoLore.add("§7removed. You will NOT get a refund.");
        inv.setItem(13, createItem(Material.VILLAGER_SPAWN_EGG, "§c" + npc.getName(), infoLore));

        // Confirm button (slot 11)
        ItemStack confirmItem = createItem(Material.LIME_WOOL, "§a§lConfirm Dismiss",
                Arrays.asList("§7Click to permanently dismiss this employee"));
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "confirm_dismiss_npc"),
                    PersistentDataType.STRING, npcId);
            confirmItem.setItemMeta(confirmMeta);
        }
        inv.setItem(11, confirmItem);

        // Cancel button (slot 15)
        inv.setItem(15, createItem(Material.RED_WOOL, "§c§lCancel",
                Arrays.asList("§7Return to Employees Center")));

        player.openInventory(inv);
    }

    /**
     * Open confirmation for firing an admin-spawned employee.
     */
    public void openFireConfirmation(String factoryId, String npcId) {
        FactoryNPC npc = plugin.getNPCManager().getNPC(npcId);
        if (npc == null) {
            player.sendMessage("§cEmployee not found!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§c§lConfirm Fire Employee");

        // Fill with border
        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++)
            inv.setItem(i, border);

        // NPC info (slot 13)
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Name: §b" + npc.getName());
        infoLore.add("§7Factory: §e" + factoryId);
        infoLore.add("§7Location: §e" + formatLocation(npc.getLocation()));
        infoLore.add("");
        infoLore.add("§c§lThis will remove the employee!");
        infoLore.add("§7You'll need an admin to spawn");
        infoLore.add("§7a new one if you want it back.");

        inv.setItem(13, createItem(Material.VILLAGER_SPAWN_EGG, "§b" + npc.getName(), infoLore));

        // Confirm button (slot 11)
        ItemStack confirmItem = createItem(Material.LIME_WOOL, "§a§lConfirm Fire",
                Arrays.asList("§7Click to fire this employee"));
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "confirm_fire_npc"),
                    PersistentDataType.STRING,
                    npcId);
            confirmItem.setItemMeta(confirmMeta);
        }
        inv.setItem(11, confirmItem);

        // Cancel button (slot 15)
        inv.setItem(15, createItem(Material.RED_WOOL, "§c§lCancel",
                Arrays.asList("§7Return to Employees Center")));

        player.openInventory(inv);
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
