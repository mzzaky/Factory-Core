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
 * My Factories GUI - View and manage all owned factories
 */
public class MyFactoriesGUI {

    private final FactoryCore plugin;
    private final Player player;
    private int currentPage = 0;
    private String filterStatus = "all"; // all, running, stopped

    public MyFactoriesGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openMyFactoriesMenu() {
        openMyFactoriesMenu(0);
    }

    public void openMyFactoriesMenu(int page) {
        this.currentPage = page;

        Inventory inv = Bukkit.createInventory(null, 54, "§6§lMy Factories §8- §ePage " + (page + 1));

        // Get player's factories
        List<Factory> ownedFactories = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());

        // Apply filter
        List<Factory> filteredFactories;
        switch (filterStatus) {
            case "running":
                filteredFactories = ownedFactories.stream()
                        .filter(f -> f.getStatus() == FactoryStatus.RUNNING)
                        .collect(Collectors.toList());
                break;
            case "stopped":
                filteredFactories = ownedFactories.stream()
                        .filter(f -> f.getStatus() == FactoryStatus.STOPPED)
                        .collect(Collectors.toList());
                break;
            default:
                filteredFactories = new ArrayList<>(ownedFactories);
        }

        // Fill borders
        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);

        // Header info
        long runningCount = ownedFactories.stream().filter(f -> f.getStatus() == FactoryStatus.RUNNING).count();
        double totalValue = ownedFactories.stream().mapToDouble(Factory::getPrice).sum();

        int baseLimit = plugin.getConfig().getInt("factory.max-ownership", 3);
        int extraLimit = plugin.getResearchManager() != null
                ? plugin.getResearchManager().getAdditionalFactoryLimit(player.getUniqueId())
                : 0;
        int maxFactories = baseLimit + extraLimit;

        List<String> headerLore = new ArrayList<>();
        headerLore.add("§7Your factory overview");
        headerLore.add("");
        headerLore.add("§eFactories Owned: §6" + ownedFactories.size() + "§7/§e" + maxFactories);
        if (extraLimit > 0) {
            headerLore.add("§d🔬 Research Buff: §f+" + extraLimit + " §dFactory Limit");
        }
        headerLore.add("§eRunning: §a" + runningCount);
        headerLore.add("§eStopped: §c" + (ownedFactories.size() - runningCount));
        headerLore.add("§eTotal Value: §6$" + String.format("%.2f", totalValue));
        headerLore.add("");
        headerLore.add("§7Filter: §e" + getFilterDisplayName());

        inv.setItem(4, createItem(Material.SMITHING_TABLE, "§6§lMy Factories", headerLore));

        // Factory slots (slots 9-44, 36 slots total per page)
        int slotsPerPage = 36;
        int startIndex = page * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, filteredFactories.size());

        int slot = 9;
        for (int i = startIndex; i < endIndex && slot < 45; i++) {
            Factory factory = filteredFactories.get(i);
            inv.setItem(slot++, createFactoryItem(factory));
        }

        // Show empty message if no factories
        if (filteredFactories.isEmpty()) {
            inv.setItem(22, createItem(Material.BARRIER, "§c§lNo Factories",
                    Arrays.asList(
                            filterStatus.equals("all") ? "§7You don't own any factories yet!"
                                    : "§7No factories match the current filter",
                            "",
                            filterStatus.equals("all") ? "§eVisit Factory Browse to purchase!"
                                    : "§eChange filter to see other factories")));
        }

        // Navigation and control buttons

        // Previous page (slot 45)
        if (page > 0) {
            inv.setItem(45, createNavigationItem(Material.ARROW, "§e§l◄ Previous Page", page - 1));
        }

        // Filter button (slot 47)
        inv.setItem(47, createFilterItem());

        // Sell all button (slot 48) - Only show if player has factories
        if (!ownedFactories.isEmpty()) {
            double totalSellValue = ownedFactories.stream()
                    .mapToDouble(f -> f.getPrice() * plugin.getConfig().getDouble("factory.sell-price-multiplier", 0.5))
                    .sum();
            inv.setItem(48, createItem(Material.GOLD_BLOCK, "§6§lSell All Factories",
                    Arrays.asList(
                            "§c§lWARNING: This will sell ALL factories!",
                            "",
                            "§7Total sell value: §6$" + String.format("%.2f", totalSellValue),
                            "",
                            "§cShift + Click to confirm")));
        }

        // Info (slot 49)
        int totalPages = (int) Math.ceil((double) filteredFactories.size() / slotsPerPage);
        inv.setItem(49, createItem(Material.PAPER, "§e§lPage Info",
                Arrays.asList(
                        "§7Page: §e" + (page + 1) + " / " + Math.max(1, totalPages),
                        "§7Showing: §e" + filteredFactories.size() + " factories")));

        // Next page (slot 53)
        if (endIndex < filteredFactories.size()) {
            inv.setItem(53, createNavigationItem(Material.ARROW, "§e§lNext Page ►", page + 1));
        }

        // Back to hub (slot 51)
        inv.setItem(51, createItem(Material.DARK_OAK_DOOR, "§c§lBack to Hub",
                Arrays.asList("§7Return to main menu")));

        player.openInventory(inv);
    }

    private ItemStack createFactoryItem(Factory factory) {
        Material material;
        switch (factory.getStatus()) {
            case RUNNING:
                material = Material.LIME_CONCRETE;
                break;
            case NO_PARTS:
                material = Material.YELLOW_CONCRETE;
                break;
            default:
                material = Material.RED_CONCRETE;
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7ID: §e" + factory.getId());
        lore.add("§7Type: " + factory.getType().getDisplayName());
        lore.add("§7Level: §e" + factory.getLevel());
        lore.add("§7Status: " + factory.getStatus().getDisplay());
        lore.add("");

        // Show production info if running
        if (factory.getCurrentProduction() != null) {
            ProductionTask task = factory.getCurrentProduction();
            Recipe recipe = plugin.getRecipeManager().getRecipe(task.getRecipeId());
            if (recipe != null) {
                lore.add("§6Production: §e" + recipe.getName());
                lore.add("§7Progress: §e" + (int) (task.getProgress() * 100) + "%");
                lore.add("§7Time Left: §e" + task.getRemainingTime() + "s");
                lore.add("");
            }
        }

        // Calculate sell value
        double sellValue = factory.getPrice() * plugin.getConfig().getDouble("factory.sell-price-multiplier", 0.5);
        lore.add("§7Value: §6$" + String.format("%.2f", factory.getPrice()));
        lore.add("§7Sell Value: §6$" + String.format("%.2f", sellValue));
        lore.add("");
        lore.add("§eLeft Click: §7Open Management");
        lore.add("§eRight Click: §7Quick Teleport");
        lore.add("§eShift + Right Click: §7Sell Factory");

        ItemStack item = createItem(material, factory.getType().getDisplayName() + " §7- §e" + factory.getId(), lore);

        // Store factory ID for click handling
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "my_factory_id"),
                    PersistentDataType.STRING,
                    factory.getId());
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createFilterItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7Current: §e" + getFilterDisplayName());
        lore.add("");
        lore.add("§7Filters:");
        lore.add((filterStatus.equals("all") ? "§a► " : "§7• ") + "All Factories");
        lore.add((filterStatus.equals("running") ? "§a► " : "§7• ") + "Running Only");
        lore.add((filterStatus.equals("stopped") ? "§a► " : "§7• ") + "Stopped Only");
        lore.add("");
        lore.add("§eClick to cycle filter!");

        return createItem(Material.HOPPER, "§b§lFilter", lore);
    }

    private String getFilterDisplayName() {
        switch (filterStatus) {
            case "running":
                return "Running Only";
            case "stopped":
                return "Stopped Only";
            default:
                return "All Factories";
        }
    }

    public void cycleFilter() {
        switch (filterStatus) {
            case "all":
                filterStatus = "running";
                break;
            case "running":
                filterStatus = "stopped";
                break;
            default:
                filterStatus = "all";
                break;
        }
    }

    private ItemStack createNavigationItem(Material material, String name, int targetPage) {
        ItemStack item = createItem(material, name, Arrays.asList("§7Go to page " + (targetPage + 1)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "myfactory_page"),
                    PersistentDataType.INTEGER,
                    targetPage);
            item.setItemMeta(meta);
        }
        return item;
    }

    // Sell confirmation dialog
    public void openSellConfirmation(String factoryId) {
        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory == null || !factory.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-owned"));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§c§lConfirm Sale");

        // Fill with border
        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++)
            inv.setItem(i, border);

        // Factory info (slot 13)
        double sellValue = factory.getPrice() * plugin.getConfig().getDouble("factory.sell-price-multiplier", 0.5);
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Type: " + factory.getType().getDisplayName());
        infoLore.add("§7Level: §e" + factory.getLevel());
        infoLore.add("");
        infoLore.add("§7Original Price: §6$" + String.format("%.2f", factory.getPrice()));
        infoLore.add("§7Sell Value: §a$" + String.format("%.2f", sellValue));
        infoLore.add("");
        infoLore.add("§c§lWarning: This action cannot be undone!");
        infoLore.add("§c§lAll stored items will be lost!");

        inv.setItem(13, createItem(getFactoryMaterial(factory.getType()),
                "§c" + factory.getType().getDisplayName() + " §7- §e" + factory.getId(), infoLore));

        // Confirm button (slot 11)
        ItemStack confirmItem = createItem(Material.LIME_WOOL, "§a§lConfirm Sale",
                Arrays.asList("§7Click to sell this factory", "", "§a+$" + String.format("%.2f", sellValue)));
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "confirm_sell_factory"),
                    PersistentDataType.STRING,
                    factoryId);
            confirmItem.setItemMeta(confirmMeta);
        }
        inv.setItem(11, confirmItem);

        // Cancel button (slot 15)
        inv.setItem(15, createItem(Material.RED_WOOL, "§c§lCancel",
                Arrays.asList("§7Return to My Factories")));

        player.openInventory(inv);
    }

    private Material getFactoryMaterial(FactoryType type) {
        switch (type) {
            case STEEL_MILL:
                return Material.IRON_BLOCK;
            case REFINERY:
                return Material.BLAST_FURNACE;
            case WORKSHOP:
                return Material.CRAFTING_TABLE;
            case ADVANCED_FACTORY:
                return Material.DIAMOND_BLOCK;
            default:
                return Material.FURNACE;
        }
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
