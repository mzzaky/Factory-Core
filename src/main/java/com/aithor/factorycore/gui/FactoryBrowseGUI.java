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
 * Factory Browse GUI - Browse and purchase available factories
 */
public class FactoryBrowseGUI {

    private final FactoryCore plugin;
    private final Player player;
    private int currentPage = 0;
    private FactoryType filterType = null; // null = show all
    private String sortBy = "price"; // price, type, name

    public FactoryBrowseGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openBrowseMenu() {
        openBrowseMenu(0);
    }

    public void openBrowseMenu(int page) {
        this.currentPage = page;
        
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lFactory Browse §8- §ePage " + (page + 1));

        // Get available factories (no owner)
        List<Factory> availableFactories = plugin.getFactoryManager().getAllFactories().stream()
                .filter(f -> f.getOwner() == null)
                .collect(Collectors.toList());

        // Apply filter
        if (filterType != null) {
            availableFactories = availableFactories.stream()
                    .filter(f -> f.getType() == filterType)
                    .collect(Collectors.toList());
        }

        // Apply sorting
        switch (sortBy) {
            case "price":
                availableFactories.sort(Comparator.comparingDouble(Factory::getPrice));
                break;
            case "price_desc":
                availableFactories.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                break;
            case "type":
                availableFactories.sort(Comparator.comparing(f -> f.getType().getDisplayName()));
                break;
            case "name":
                availableFactories.sort(Comparator.comparing(Factory::getId));
                break;
        }

        // Fill borders
        Material borderMat = Material.matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        // Header info
        inv.setItem(4, createItem(Material.COMPASS, "§a§lFactory Browse",
                Arrays.asList(
                        "§7Browse available factories",
                        "",
                        "§eTotal Available: §a" + availableFactories.size(),
                        "§eFilter: §e" + (filterType != null ? filterType.getDisplayName() : "§7All"),
                        "§eSort: §e" + getSortDisplayName())));

        // Factory slots (slots 9-44, 36 slots total per page)
        int slotsPerPage = 36;
        int startIndex = page * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, availableFactories.size());

        int slot = 9;
        for (int i = startIndex; i < endIndex && slot < 45; i++) {
            Factory factory = availableFactories.get(i);
            inv.setItem(slot++, createFactoryItem(factory));
        }

        // Navigation and control buttons
        
        // Previous page (slot 45)
        if (page > 0) {
            inv.setItem(45, createNavigationItem(Material.ARROW, "§e§l◄ Previous Page", page - 1));
        }

        // Filter button (slot 47)
        inv.setItem(47, createFilterItem());

        // Sort button (slot 48)
        inv.setItem(48, createSortItem());

        // Info (slot 49)
        int totalPages = (int) Math.ceil((double) availableFactories.size() / slotsPerPage);
        inv.setItem(49, createItem(Material.PAPER, "§e§lPage Info",
                Arrays.asList(
                        "§7Page: §e" + (page + 1) + " / " + Math.max(1, totalPages),
                        "§7Factories: §e" + availableFactories.size())));

        // Next page (slot 53)
        if (endIndex < availableFactories.size()) {
            inv.setItem(53, createNavigationItem(Material.ARROW, "§e§lNext Page ►", page + 1));
        }

        // Back to hub (slot 51)
        inv.setItem(51, createItem(Material.DARK_OAK_DOOR, "§c§lBack to Hub",
                Arrays.asList("§7Return to main menu")));

        player.openInventory(inv);
    }

    private ItemStack createFactoryItem(Factory factory) {
        Material material = getFactoryMaterial(factory.getType());
        
        List<String> lore = new ArrayList<>();
        lore.add("§7ID: §e" + factory.getId());
        lore.add("§7Type: " + factory.getType().getDisplayName());
        lore.add("§7Region: §e" + factory.getRegionName());
        lore.add("");
        lore.add("§7Price: §6$" + String.format("%.2f", factory.getPrice()));
        lore.add("");
        
        // Check if player can afford
        boolean canAfford = plugin.getEconomy().has(player, factory.getPrice());
        if (canAfford) {
            lore.add("§a§lClick to purchase!");
        } else {
            lore.add("§c§lInsufficient funds!");
            lore.add("§7You need §6$" + String.format("%.2f", factory.getPrice() - plugin.getEconomy().getBalance(player)) + " §7more");
        }

        ItemStack item = createItem(material, factory.getType().getDisplayName() + " §7- §e" + factory.getId(), lore);
        
        // Store factory ID in item for click handling
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "browse_factory_id"),
                    PersistentDataType.STRING,
                    factory.getId());
            item.setItemMeta(meta);
        }
        
        return item;
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

    private ItemStack createFilterItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7Current: §e" + (filterType != null ? filterType.getDisplayName() : "§7All Types"));
        lore.add("");
        lore.add("§7Available Filters:");
        lore.add("§7• §fAll Types");
        for (FactoryType type : FactoryType.values()) {
            lore.add("§7• " + type.getDisplayName());
        }
        lore.add("");
        lore.add("§eLeft Click: §7Next filter");
        lore.add("§eRight Click: §7Reset filter");

        return createItem(Material.HOPPER, "§b§lFilter", lore);
    }

    private ItemStack createSortItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7Current: §e" + getSortDisplayName());
        lore.add("");
        lore.add("§7Sort Options:");
        lore.add((sortBy.equals("price") ? "§a► " : "§7• ") + "Price (Low to High)");
        lore.add((sortBy.equals("price_desc") ? "§a► " : "§7• ") + "Price (High to Low)");
        lore.add((sortBy.equals("type") ? "§a► " : "§7• ") + "Type");
        lore.add((sortBy.equals("name") ? "§a► " : "§7• ") + "Name");
        lore.add("");
        lore.add("§eClick to cycle sort!");

        return createItem(Material.COMPARATOR, "§d§lSort", lore);
    }

    private String getSortDisplayName() {
        switch (sortBy) {
            case "price": return "Price (Low → High)";
            case "price_desc": return "Price (High → Low)";
            case "type": return "Type";
            case "name": return "Name";
            default: return "Default";
        }
    }

    private ItemStack createNavigationItem(Material material, String name, int targetPage) {
        ItemStack item = createItem(material, name, Arrays.asList("§7Go to page " + (targetPage + 1)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "browse_page"),
                    PersistentDataType.INTEGER,
                    targetPage);
            item.setItemMeta(meta);
        }
        return item;
    }

    // Purchase confirmation
    public void openPurchaseConfirmation(String factoryId) {
        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory == null || factory.getOwner() != null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§6§lConfirm Purchase");

        // Fill with border
        Material borderMat = Material.matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // Factory info (slot 13)
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Type: " + factory.getType().getDisplayName());
        infoLore.add("§7Region: §e" + factory.getRegionName());
        infoLore.add("");
        infoLore.add("§7Price: §6$" + String.format("%.2f", factory.getPrice()));
        infoLore.add("");
        infoLore.add("§7Your Balance: §6$" + String.format("%.2f", plugin.getEconomy().getBalance(player)));
        
        boolean canAfford = plugin.getEconomy().has(player, factory.getPrice());
        if (canAfford) {
            infoLore.add("§7After Purchase: §6$" + String.format("%.2f", plugin.getEconomy().getBalance(player) - factory.getPrice()));
        } else {
            infoLore.add("§c§lInsufficient funds!");
        }

        inv.setItem(13, createItem(getFactoryMaterial(factory.getType()),
                "§6" + factory.getType().getDisplayName() + " §7- §e" + factory.getId(), infoLore));

        // Confirm button (slot 11)
        ItemStack confirmItem;
        if (canAfford) {
            confirmItem = createItem(Material.LIME_WOOL, "§a§lConfirm Purchase",
                    Arrays.asList("§7Click to buy this factory", "", "§6-$" + String.format("%.2f", factory.getPrice())));
        } else {
            confirmItem = createItem(Material.GRAY_WOOL, "§7§lCannot Purchase",
                    Arrays.asList("§cYou don't have enough money"));
        }
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "confirm_purchase_factory"),
                    PersistentDataType.STRING,
                    factoryId);
            confirmItem.setItemMeta(confirmMeta);
        }
        inv.setItem(11, confirmItem);

        // Cancel button (slot 15)
        inv.setItem(15, createItem(Material.RED_WOOL, "§c§lCancel",
                Arrays.asList("§7Return to browse menu")));

        player.openInventory(inv);
    }

    public void setFilterType(FactoryType type) {
        this.filterType = type;
    }

    public FactoryType getFilterType() {
        return filterType;
    }

    public void cycleFilter() {
        if (filterType == null) {
            filterType = FactoryType.values()[0];
        } else {
            int currentIndex = filterType.ordinal();
            if (currentIndex + 1 >= FactoryType.values().length) {
                filterType = null; // Back to all
            } else {
                filterType = FactoryType.values()[currentIndex + 1];
            }
        }
    }

    public void setSortBy(String sort) {
        this.sortBy = sort;
    }

    public void cycleSort() {
        switch (sortBy) {
            case "price":
                sortBy = "price_desc";
                break;
            case "price_desc":
                sortBy = "type";
                break;
            case "type":
                sortBy = "name";
                break;
            default:
                sortBy = "price";
                break;
        }
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
}
