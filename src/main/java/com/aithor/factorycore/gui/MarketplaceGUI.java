package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.managers.MarketplaceManager;
import com.aithor.factorycore.models.ResourceItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Marketplace GUI - Player resource trading interface
 */
public class MarketplaceGUI {

    private final FactoryCore plugin;
    private final Player player;
    private int currentPage = 0;
    private String currentView = "browse"; // browse, my_listings, sell
    private String filterResource = null; // null = all resources

    public MarketplaceGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openMarketplaceMenu() {
        openMarketplaceMenu(0);
    }

    public void openMarketplaceMenu(int page) {
        this.currentPage = page;

        Inventory inv = Bukkit.createInventory(null, 54, "§2§lMarketplace §8- §e" + getViewTitle());

        MarketplaceManager marketplace = plugin.getMarketplaceManager();

        // Fill borders
        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);

        // Header with marketplace stats
        List<MarketplaceManager.MarketListing> allListings = marketplace.getAllListings();
        double totalVolume = allListings.stream().mapToDouble(MarketplaceManager.MarketListing::getTotalPrice).sum();

        inv.setItem(4, createItem(Material.EMERALD, "§2§lMarketplace",
                Arrays.asList(
                        "§7Player resource trading",
                        "",
                        "§eActive Listings: §2" + allListings.size(),
                        "§eTotal Volume: §6$" + String.format("%.2f", totalVolume),
                        "§eTax Rate: §e" + plugin.getConfig().getDouble("marketplace.tax-rate", 5) + "%")));

        // View navigation buttons
        inv.setItem(0, createViewButton("browse", Material.COMPASS, "§a§lBrowse All", "View all listings"));
        inv.setItem(7, createViewButton("my_listings", Material.BOOK, "§6§lMy Listings", "Manage your listings"));
        inv.setItem(8, createViewButton("sell", Material.GOLD_INGOT, "§e§lSell Items", "Create a new listing"));

        // Pending earnings button (slot 8)
        double pendingEarnings = marketplace.getPendingEarnings(player.getUniqueId());
        if (pendingEarnings > 0) {
            inv.setItem(8, createItem(Material.GOLD_BLOCK, "§6§lCollect Earnings",
                    Arrays.asList(
                            "§7You have pending earnings!",
                            "",
                            "§ePending: §6$" + String.format("%.2f", pendingEarnings),
                            "",
                            "§a§lClick to collect!")));
        }

        // Display content based on view
        switch (currentView) {
            case "browse":
                displayBrowseView(inv, page);
                break;
            case "my_listings":
                displayMyListingsView(inv, page);
                break;
            case "sell":
                displaySellView(inv);
                break;
        }

        // Navigation buttons (for browse and my_listings)
        if (!currentView.equals("sell")) {
            List<?> items = currentView.equals("browse") ? getFilteredListings()
                    : marketplace.getPlayerListings(player.getUniqueId());

            int slotsPerPage = 36;
            int totalPages = (int) Math.ceil((double) items.size() / slotsPerPage);

            // Previous page (slot 45)
            if (page > 0) {
                inv.setItem(45, createNavigationItem(Material.ARROW, "§e§l◄ Previous Page", page - 1));
            }

            // Filter button (slot 47) - only for browse
            if (currentView.equals("browse")) {
                inv.setItem(47, createFilterItem());
            }

            // Page info (slot 49)
            inv.setItem(49, createItem(Material.PAPER, "§e§lPage Info",
                    Arrays.asList(
                            "§7Page: §e" + (page + 1) + " / " + Math.max(1, totalPages),
                            "§7Items: §e" + items.size())));

            // Next page (slot 53)
            if ((page + 1) * slotsPerPage < items.size()) {
                inv.setItem(53, createNavigationItem(Material.ARROW, "§e§lNext Page ►", page + 1));
            }
        }

        // Back to hub (slot 51)
        inv.setItem(51, createItem(Material.DARK_OAK_DOOR, "§c§lBack to Hub",
                Arrays.asList("§7Return to main menu")));

        player.openInventory(inv);
    }

    private void displayBrowseView(Inventory inv, int page) {
        List<MarketplaceManager.MarketListing> listings = getFilteredListings();

        int slotsPerPage = 36;
        int startIndex = page * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, listings.size());

        int slot = 9;
        for (int i = startIndex; i < endIndex && slot < 45; i++) {
            MarketplaceManager.MarketListing listing = listings.get(i);
            inv.setItem(slot++, createListingItem(listing, false));
        }

        if (listings.isEmpty()) {
            inv.setItem(22, createItem(Material.BARRIER, "§c§lNo Listings",
                    Arrays.asList(
                            filterResource != null ? "§7No listings for this resource." : "§7The marketplace is empty!",
                            "",
                            "§7Be the first to sell!")));
        }
    }

    private void displayMyListingsView(Inventory inv, int page) {
        List<MarketplaceManager.MarketListing> myListings = plugin.getMarketplaceManager()
                .getPlayerListings(player.getUniqueId());

        int slotsPerPage = 36;
        int startIndex = page * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, myListings.size());

        int slot = 9;
        for (int i = startIndex; i < endIndex && slot < 45; i++) {
            MarketplaceManager.MarketListing listing = myListings.get(i);
            inv.setItem(slot++, createListingItem(listing, true));
        }

        if (myListings.isEmpty()) {
            inv.setItem(22, createItem(Material.CHEST, "§7§lNo Listings",
                    Arrays.asList(
                            "§7You don't have any active listings.",
                            "",
                            "§eClick §6Sell Items §eto create one!")));
        }

        // Stats for player (slot 48)
        double totalValue = myListings.stream().mapToDouble(MarketplaceManager.MarketListing::getTotalPrice).sum();
        int baseMaxListings = plugin.getConfig().getInt("marketplace.max-listings-per-player", 10);
        int additionalListings = plugin.getResearchManager().getAdditionalListingLimit(player.getUniqueId());
        int maxListings = baseMaxListings + additionalListings;
        inv.setItem(48, createItem(Material.BOOK, "§e§lYour Stats",
                Arrays.asList(
                        "§7Active Listings: §e" + myListings.size() + "/" + maxListings,
                        "§7Total Value: §6$" + String.format("%.2f", totalValue))));
    }

    private void displaySellView(Inventory inv) {
        // Get resources in player's inventory that are from FactoryCore
        List<String> lore = new ArrayList<>();
        lore.add("§7Click on a resource in your");
        lore.add("§7inventory to list it for sale!");
        lore.add("");
        lore.add("§7§nHow to sell:§r");
        lore.add("§71. Have resources in inventory");
        lore.add("§72. Click on them in this view");
        lore.add("§73. Set your price");
        lore.add("§74. Confirm listing");
        lore.add("");
        lore.add("§7Tax Rate: §e" + plugin.getConfig().getDouble("marketplace.tax-rate", 5) + "%");
        lore.add("§7(Deducted when item sells)");

        inv.setItem(13, createItem(Material.ANVIL, "§e§lCreate Listing", lore));
    }

    private ItemStack createListingItem(MarketplaceManager.MarketListing listing, boolean isOwn) {
        ResourceItem resource = plugin.getResourceManager().getResource(listing.resourceId);

        Material material;
        String resourceName;
        if (resource != null) {
            try {
                material = Material.valueOf(resource.getMaterial());
            } catch (IllegalArgumentException e) {
                material = Material.STONE;
            }
            resourceName = resource.getName();
        } else {
            material = Material.STONE;
            resourceName = listing.resourceId;
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7Seller: §e" + listing.sellerName);
        lore.add("");
        lore.add("§7Amount: §e" + listing.amount);
        lore.add("§7Price per Unit: §6$" + String.format("%.2f", listing.pricePerUnit));
        lore.add("§7Total Price: §6$" + String.format("%.2f", listing.getTotalPrice()));
        lore.add("");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
        lore.add("§7Listed: §e" + sdf.format(new Date(listing.listedTime)));
        lore.add("");

        if (isOwn) {
            long baseExpirationHours = plugin.getConfig().getLong("marketplace.listing-expiration-hours", 168);
            long additionalHours = plugin.getResearchManager().getAdditionalListingHours(player.getUniqueId());
            long expirationHours = baseExpirationHours + additionalHours;
            long expireTime = listing.listedTime + (expirationHours * 3600000L);
            long remainingMs = expireTime - System.currentTimeMillis();

            if (remainingMs > 0) {
                long remainingDays = remainingMs / 86400000L;
                long remainingHours = (remainingMs % 86400000L) / 3600000L;
                long remainingMinutes = (remainingMs % 3600000L) / 60000L;

                String timeStr = "";
                if (remainingDays > 0) {
                    timeStr = remainingDays + "d " + remainingHours + "h";
                } else if (remainingHours > 0) {
                    timeStr = remainingHours + "h " + remainingMinutes + "m";
                } else {
                    timeStr = remainingMinutes + "m";
                }
                lore.add("§7Expires in: §c" + timeStr);
            } else {
                lore.add("§7Expires in: §cExpired");
            }
            lore.add("");

            lore.add("§c§lClick to cancel listing");
            lore.add("§7Items will be returned to you");
        } else {
            boolean canAfford = plugin.getEconomy().has(player, listing.getTotalPrice());
            if (canAfford) {
                lore.add("§a§lLeft Click: §7Buy 1");
                lore.add("§a§lRight Click: §7Buy All");
            } else {
                lore.add("§c§lInsufficient funds!");
            }
        }

        ItemStack item = createItem(material, resourceName + " §7x" + listing.amount, lore);
        item.setAmount(Math.min(listing.amount, 64));

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "market_listing_id"),
                    PersistentDataType.STRING,
                    listing.id);
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "is_own_listing"),
                    PersistentDataType.INTEGER,
                    isOwn ? 1 : 0);
            item.setItemMeta(meta);
        }

        return item;
    }

    private List<MarketplaceManager.MarketListing> getFilteredListings() {
        List<MarketplaceManager.MarketListing> all = plugin.getMarketplaceManager().getAllListings();

        if (filterResource != null) {
            return all.stream()
                    .filter(l -> l.resourceId.equals(filterResource))
                    .sorted(Comparator.comparingDouble(l -> l.pricePerUnit))
                    .collect(Collectors.toList());
        }

        return all.stream()
                .sorted(Comparator.comparingLong(l -> -l.listedTime)) // Newest first
                .collect(Collectors.toList());
    }

    private ItemStack createViewButton(String view, Material material, String name, String description) {
        List<String> lore = new ArrayList<>();
        lore.add("§7" + description);
        lore.add("");
        if (currentView.equals(view)) {
            lore.add("§a§l► Currently viewing");
        } else {
            lore.add("§eClick to view!");
        }

        ItemStack item = createItem(material, name, lore);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "market_view"),
                    PersistentDataType.STRING,
                    view);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createFilterItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7Current: §e"
                + (filterResource != null ? plugin.getResourceManager().getResource(filterResource).getName()
                        : "All Resources"));
        lore.add("");
        lore.add("§7Available filters:");
        lore.add("§7• All Resources");

        Set<String> available = plugin.getMarketplaceManager().getAvailableResources();
        for (String resourceId : available) {
            ResourceItem resource = plugin.getResourceManager().getResource(resourceId);
            if (resource != null) {
                lore.add("§7• " + resource.getName());
            }
        }

        lore.add("");
        lore.add("§eClick to cycle filter!");

        return createItem(Material.HOPPER, "§b§lFilter", lore);
    }

    public void cycleFilter() {
        Set<String> available = plugin.getMarketplaceManager().getAvailableResources();
        List<String> resourceList = new ArrayList<>(available);

        if (filterResource == null) {
            if (!resourceList.isEmpty()) {
                filterResource = resourceList.get(0);
            }
        } else {
            int index = resourceList.indexOf(filterResource);
            if (index + 1 >= resourceList.size()) {
                filterResource = null;
            } else {
                filterResource = resourceList.get(index + 1);
            }
        }
    }

    public void setView(String view) {
        this.currentView = view;
        this.currentPage = 0;
    }

    private String getViewTitle() {
        switch (currentView) {
            case "my_listings":
                return "My Listings";
            case "sell":
                return "Sell Items";
            default:
                return "Browse";
        }
    }

    // Open sell confirmation dialog
    public void openSellConfirmation(String resourceId, int amount) {
        ResourceItem resource = plugin.getResourceManager().getResource(resourceId);
        if (resource == null) {
            player.sendMessage("§cResource not found!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§e§lCreate Listing");

        // Fill with border
        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++)
            inv.setItem(i, border);

        // Resource info (slot 13)
        double suggestedPrice = resource.getSellPrice();
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Amount: §e" + amount);
        infoLore.add("§7Suggested Price: §6$" + String.format("%.2f", suggestedPrice));
        infoLore.add("");
        infoLore.add("§7Choose a price option below!");

        Material material;
        try {
            material = Material.valueOf(resource.getMaterial());
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }
        inv.setItem(4, createItem(material, resource.getName(), infoLore));

        // Price options
        double[] multipliers = { 0.5, 0.75, 1.0, 1.25, 1.5 };
        String[] labels = { "§cCheap", "§eBelow Market", "§aMarket Price", "§6Above Market", "§cExpensive" };
        int[] slots = { 10, 11, 13, 15, 16 };

        for (int i = 0; i < multipliers.length; i++) {
            double price = suggestedPrice * multipliers[i];
            List<String> priceLore = new ArrayList<>();
            priceLore.add("§7Price per unit: §6$" + String.format("%.2f", price));
            priceLore.add("§7Total: §6$" + String.format("%.2f", price * amount));
            priceLore.add("");
            priceLore.add("§eClick to list at this price!");

            ItemStack priceItem = createItem(Material.GOLD_NUGGET,
                    labels[i] + " §7- §6$" + String.format("%.2f", price), priceLore);
            ItemMeta meta = priceItem.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "listing_resource"),
                        PersistentDataType.STRING,
                        resourceId);
                meta.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "listing_amount"),
                        PersistentDataType.INTEGER,
                        amount);
                meta.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "listing_price"),
                        PersistentDataType.DOUBLE,
                        price);
                priceItem.setItemMeta(meta);
            }
            inv.setItem(slots[i], priceItem);
        }

        // Cancel button (slot 22)
        inv.setItem(22, createItem(Material.RED_WOOL, "§c§lCancel",
                Arrays.asList("§7Return to marketplace")));

        player.openInventory(inv);
    }

    // Open purchase confirmation
    public void openPurchaseConfirmation(String listingId, int amount) {
        MarketplaceManager.MarketListing listing = plugin.getMarketplaceManager().getListing(listingId);
        if (listing == null) {
            player.sendMessage("§cListing not found!");
            return;
        }

        ResourceItem resource = plugin.getResourceManager().getResource(listing.resourceId);

        Inventory inv = Bukkit.createInventory(null, 27, "§2§lConfirm Purchase");

        // Fill with border
        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++)
            inv.setItem(i, border);

        // Item info (slot 13)
        double totalPrice = listing.pricePerUnit * amount;
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Seller: §e" + listing.sellerName);
        infoLore.add("");
        infoLore.add("§7Amount: §e" + amount);
        infoLore.add("§7Price per Unit: §6$" + String.format("%.2f", listing.pricePerUnit));
        infoLore.add("§7Total: §6$" + String.format("%.2f", totalPrice));
        infoLore.add("");
        infoLore.add("§7Your Balance: §6$" + String.format("%.2f", plugin.getEconomy().getBalance(player)));

        Material material = Material.STONE;
        String resourceName = listing.resourceId;
        if (resource != null) {
            try {
                material = Material.valueOf(resource.getMaterial());
            } catch (IllegalArgumentException e) {
                // Keep default
            }
            resourceName = resource.getName();
        }
        inv.setItem(13, createItem(material, resourceName + " §7x" + amount, infoLore));

        // Confirm button (slot 11)
        boolean canAfford = plugin.getEconomy().has(player, totalPrice);
        ItemStack confirmItem;
        if (canAfford) {
            confirmItem = createItem(Material.LIME_WOOL, "§a§lConfirm Purchase",
                    Arrays.asList("§7Click to buy", "", "§6-$" + String.format("%.2f", totalPrice)));
        } else {
            confirmItem = createItem(Material.GRAY_WOOL, "§7§lCannot Purchase",
                    Arrays.asList("§cInsufficient funds!"));
        }
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "confirm_purchase_listing"),
                    PersistentDataType.STRING,
                    listingId);
            confirmMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "purchase_amount"),
                    PersistentDataType.INTEGER,
                    amount);
            confirmItem.setItemMeta(confirmMeta);
        }
        inv.setItem(11, confirmItem);

        // Cancel button (slot 15)
        inv.setItem(15, createItem(Material.RED_WOOL, "§c§lCancel",
                Arrays.asList("§7Return to marketplace")));

        player.openInventory(inv);
    }

    private ItemStack createNavigationItem(Material material, String name, int targetPage) {
        ItemStack item = createItem(material, name, Arrays.asList("§7Go to page " + (targetPage + 1)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "marketplace_page"),
                    PersistentDataType.INTEGER,
                    targetPage);
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
