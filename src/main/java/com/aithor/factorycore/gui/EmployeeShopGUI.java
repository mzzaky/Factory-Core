package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.Factory;
import com.aithor.factorycore.models.FactoryNPC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Employee Shop GUI — allows players to browse and purchase NPC employees.
 * Purchased NPCs are stored in the player's "employee inventory" and can
 * later be assigned to any factory the player owns.
 */
public class EmployeeShopGUI {

    private final FactoryCore plugin;
    private final Player player;

    public EmployeeShopGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    // ─── Open Shop ────────────────────────────────────────────────────────────

    public void openShop() {
        ConfigurationSection shopSection = plugin.getNPCManager().getNpcSettings().getConfigurationSection("shop.npcs");

        int size = 54;
        Inventory inv = Bukkit.createInventory(null, size, "§6§lEmployee Shop §8- §eBuy NPC");

        // Border
        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);

        // Header
        List<String> headerLore = new ArrayList<>();
        headerLore.add("§7Browse and purchase NPC employees.");
        headerLore.add("§7Purchased employees can be assigned");
        headerLore.add("§7to any factory you own.");
        headerLore.add("");
        headerLore.add("§eEach employee provides a unique");
        headerLore.add("§eProduction Time Reduction buff.");
        headerLore.add("");
        headerLore.add("§7Salary is charged periodically.");
        inv.setItem(4, createItem(Material.GOLD_INGOT, "§6§lEmployee Shop", headerLore));

        // NPC items
        int slot = 10;
        if (shopSection != null) {
            for (String npcTypeId : shopSection.getKeys(false)) {
                if (slot >= 44)
                    break;
                // Skip slots 17, 26, 35 (right border column)
                if (slot % 9 == 8)
                    slot++;

                ConfigurationSection npcSection = shopSection.getConfigurationSection(npcTypeId);
                if (npcSection == null)
                    continue;

                inv.setItem(slot, createShopNPCItem(npcTypeId, npcSection));
                slot++;
                if (slot % 9 == 0)
                    slot++; // skip left border
            }
        }

        // My Employees button (slot 46)
        inv.setItem(46, createItem(Material.PLAYER_HEAD, "§b§lMy Employees",
                Arrays.asList(
                        "§7View employees you have purchased.",
                        "§7Assign them to your factories.",
                        "",
                        "§eClick to view!")));

        // Back button (slot 49)
        inv.setItem(49, createItem(Material.DARK_OAK_DOOR, "§c§lBack",
                Arrays.asList("§7Return to Employees Center")));

        player.openInventory(inv);
    }

    // ─── Open My Employees ────────────────────────────────────────────────────

    /**
     * Shows the player's purchased (unassigned) employees.
     * Players can click an employee to assign it to a factory.
     */
    public void openMyEmployees() {
        Inventory inv = Bukkit.createInventory(null, 54, "§b§lMy Employees §8- §eAssign to Factory");

        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);

        // Header
        List<String> headerLore = new ArrayList<>();
        headerLore.add("§7These are your purchased employees.");
        headerLore.add("§7Click an employee to assign them");
        headerLore.add("§7to one of your factories.");
        headerLore.add("");
        headerLore.add("§eUnassigned employees still cost salary!");
        inv.setItem(4, createItem(Material.PLAYER_HEAD, "§b§lMy Employees", headerLore));

        // Get player's unassigned NPCs
        List<FactoryNPC> unassigned = plugin.getNPCManager().getUnassignedNPCsByOwner(player.getUniqueId());

        int slot = 9;
        for (FactoryNPC npc : unassigned) {
            if (slot >= 45)
                break;
            inv.setItem(slot, createOwnedNPCItem(npc, false));
            slot++;
        }

        if (unassigned.isEmpty()) {
            inv.setItem(22, createItem(Material.BARRIER, "§c§lNo Unassigned Employees",
                    Arrays.asList(
                            "§7You have no unassigned employees.",
                            "",
                            "§7Purchase employees from the",
                            "§7Employee Shop to get started!")));
        }

        // Back to shop (slot 49)
        inv.setItem(49, createItem(Material.DARK_OAK_DOOR, "§c§lBack to Shop",
                Arrays.asList("§7Return to Employee Shop")));

        player.openInventory(inv);
    }

    // ─── Open Factory Assign ──────────────────────────────────────────────────

    /**
     * Shows the player's factories so they can choose which factory to assign the
     * NPC to.
     */
    public void openFactoryAssign(String npcId) {
        FactoryNPC npc = plugin.getNPCManager().getNPC(npcId);
        if (npc == null) {
            player.sendMessage("§cEmployee not found!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, "§a§lAssign Employee §8- §eSelect Factory");

        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);

        // Header
        List<String> headerLore = new ArrayList<>();
        headerLore.add("§7Select a factory to assign:");
        headerLore.add("§b" + npc.getName());
        headerLore.add("");
        headerLore.add("§7Only factories without an employee");
        headerLore.add("§7can receive a new assignment.");
        inv.setItem(4, createItem(Material.LIME_WOOL, "§a§lAssign Employee", headerLore));

        // List player's factories
        List<Factory> playerFactories = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());
        int slot = 9;
        for (Factory factory : playerFactories) {
            if (slot >= 45)
                break;
            FactoryNPC existing = plugin.getNPCManager().getAssignedNPCForFactory(factory.getId());
            boolean hasEmployee = existing != null;

            List<String> lore = new ArrayList<>();
            lore.add("§7Factory: §e" + factory.getId());
            lore.add("§7Type: " + factory.getType().getDisplayName());
            lore.add("§7Level: §b" + factory.getLevel());
            lore.add("");
            if (hasEmployee) {
                lore.add("§c✗ Already has an employee: §f" + existing.getName());
                lore.add("§7Remove the current employee first.");
            } else {
                lore.add("§a✓ Available for assignment");
                lore.add("");
                lore.add("§eClick to assign §b" + npc.getName() + " §ehere!");
            }

            Material mat = hasEmployee ? Material.RED_CONCRETE : Material.LIME_CONCRETE;
            ItemStack factoryItem = createItem(mat,
                    (hasEmployee ? "§c" : "§a") + factory.getType().getDisplayName() + " §7- §e" + factory.getId(),
                    lore);

            if (!hasEmployee) {
                ItemMeta meta = factoryItem.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(
                            new NamespacedKey(plugin, "assign_factory_id"),
                            PersistentDataType.STRING, factory.getId());
                    meta.getPersistentDataContainer().set(
                            new NamespacedKey(plugin, "assign_npc_id"),
                            PersistentDataType.STRING, npcId);
                    factoryItem.setItemMeta(meta);
                }
            }

            inv.setItem(slot, factoryItem);
            slot++;
        }

        if (playerFactories.isEmpty()) {
            inv.setItem(22, createItem(Material.BARRIER, "§c§lNo Factories",
                    Arrays.asList("§7You don't own any factories!")));
        }

        // Back (slot 49)
        inv.setItem(49, createItem(Material.DARK_OAK_DOOR, "§c§lBack",
                Arrays.asList("§7Return to My Employees")));

        player.openInventory(inv);
    }

    // ─── Open Purchase Confirmation ───────────────────────────────────────────

    public void openPurchaseConfirmation(String npcTypeId) {
        ConfigurationSection npcSection = plugin.getNPCManager().getNpcSettings()
                .getConfigurationSection("shop.npcs." + npcTypeId);
        if (npcSection == null) {
            player.sendMessage("§cEmployee type not found!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§6§lConfirm Purchase");

        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++)
            inv.setItem(i, border);

        String displayName = npcSection.getString("display-name", "§fEmployee");
        double buyPrice = npcSection.getDouble("buy-price", 0.0);
        double salary = npcSection.getDouble("salary", 0.0);
        double reduction = npcSection.getDouble("production-time-reduction", 0.0);
        List<String> description = npcSection.getStringList("description");

        // NPC info (slot 13)
        List<String> infoLore = new ArrayList<>(description);
        infoLore.add("");
        infoLore.add("§6Buy Price: §a$" + String.format("%.2f", buyPrice));
        infoLore.add("§6Salary: §e$" + String.format("%.2f", salary) + " §7(per cycle)");
        infoLore.add("§6Production Buff: §a-" + reduction + "% §7time");
        infoLore.add("");
        infoLore.add("§7Your balance: §a$" + String.format("%.2f", plugin.getEconomy().getBalance(player)));

        String iconName = npcSection.getString("shop-icon", "VILLAGER_SPAWN_EGG");
        Material iconMat = Material.matchMaterial(iconName);
        if (iconMat == null)
            iconMat = Material.VILLAGER_SPAWN_EGG;
        inv.setItem(13, createItem(iconMat, displayName, infoLore));

        // Confirm button (slot 11)
        ItemStack confirmItem = createItem(Material.LIME_WOOL, "§a§lConfirm Purchase",
                Arrays.asList(
                        "§7Click to purchase this employee.",
                        "§6Cost: §a$" + String.format("%.2f", buyPrice)));
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "shop_confirm_npc_type"),
                    PersistentDataType.STRING, npcTypeId);
            confirmItem.setItemMeta(confirmMeta);
        }
        inv.setItem(11, confirmItem);

        // Cancel button (slot 15)
        inv.setItem(15, createItem(Material.RED_WOOL, "§c§lCancel",
                Arrays.asList("§7Return to Employee Shop")));

        player.openInventory(inv);
    }

    // ─── Item Builders ────────────────────────────────────────────────────────

    private ItemStack createShopNPCItem(String npcTypeId, ConfigurationSection npcSection) {
        String displayName = npcSection.getString("display-name", "§fEmployee");
        double buyPrice = npcSection.getDouble("buy-price", 0.0);
        double salary = npcSection.getDouble("salary", 0.0);
        double reduction = npcSection.getDouble("production-time-reduction", 0.0);
        List<String> description = npcSection.getStringList("description");

        List<String> lore = new ArrayList<>(description);
        lore.add("");
        lore.add("§6Buy Price: §a$" + String.format("%.2f", buyPrice));
        lore.add("§6Salary: §e$" + String.format("%.2f", salary) + " §7(per cycle)");
        lore.add("§6Production Buff: §a-" + reduction + "% §7time");
        lore.add("");
        lore.add("§eClick to purchase!");

        String iconName = npcSection.getString("shop-icon", "VILLAGER_SPAWN_EGG");
        Material iconMat = Material.matchMaterial(iconName);
        if (iconMat == null)
            iconMat = Material.VILLAGER_SPAWN_EGG;

        ItemStack item = createItem(iconMat, displayName, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "shop_npc_type_id"),
                    PersistentDataType.STRING, npcTypeId);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createOwnedNPCItem(FactoryNPC npc, boolean assigned) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Type: §e" + (npc.getNpcTypeId() != null ? npc.getNpcTypeId() : "Unknown"));
        lore.add("§7Production Buff: §a-" + npc.getProductionTimeReduction() + "% §7time");
        lore.add("");
        if (assigned) {
            lore.add("§a✓ Assigned to: §e" + npc.getFactoryId());
        } else {
            lore.add("§c✗ Not assigned to any factory");
            lore.add("");
            lore.add("§eClick to assign to a factory!");
        }

        ItemStack item = createItem(Material.VILLAGER_SPAWN_EGG, npc.getName(), lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "owned_npc_id"),
                    PersistentDataType.STRING, npc.getId());
            item.setItemMeta(meta);
        }
        return item;
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
