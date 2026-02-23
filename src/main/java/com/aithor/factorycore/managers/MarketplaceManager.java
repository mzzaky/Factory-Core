package com.aithor.factorycore.managers;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.ResourceItem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MarketplaceManager - Handles player-to-player resource trading
 */
public class MarketplaceManager {

    private final FactoryCore plugin;
    private final Map<String, MarketListing> listings; // listingId -> listing
    private final Map<UUID, List<String>> playerListings; // playerId -> list of listingIds
    private final Map<UUID, Double> playerEarnings; // pending earnings from sales
    private final File dataFile;

    public MarketplaceManager(FactoryCore plugin) {
        this.plugin = plugin;
        this.listings = new HashMap<>();
        this.playerListings = new HashMap<>();
        this.playerEarnings = new HashMap<>();
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.dataFile = new File(dataFolder, "marketplace.yml");
        loadMarketplace();
    }

    private void loadMarketplace() {
        if (!dataFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        // Load listings
        if (config.contains("listings")) {
            for (String listingId : config.getConfigurationSection("listings").getKeys(false)) {
                String path = "listings." + listingId;
                try {
                    MarketListing listing = new MarketListing(
                            listingId,
                            UUID.fromString(config.getString(path + ".seller")),
                            config.getString(path + ".seller-name"),
                            config.getString(path + ".resource-id"),
                            config.getInt(path + ".amount"),
                            config.getDouble(path + ".price-per-unit"),
                            config.getLong(path + ".listed-time"),
                            config.getBoolean(path + ".active", true));

                    if (listing.active) {
                        listings.put(listingId, listing);
                        playerListings.computeIfAbsent(listing.seller, k -> new ArrayList<>()).add(listingId);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load listing: " + listingId);
                }
            }
        }

        // Load pending earnings
        if (config.contains("earnings")) {
            for (String uuidStr : config.getConfigurationSection("earnings").getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidStr);
                    double amount = config.getDouble("earnings." + uuidStr);
                    playerEarnings.put(playerId, amount);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load earnings for: " + uuidStr);
                }
            }
        }

        plugin.getLogger().info("Loaded " + listings.size() + " marketplace listings!");
    }

    public void saveAll() {
        FileConfiguration config = new YamlConfiguration();

        // Save listings
        for (MarketListing listing : listings.values()) {
            String path = "listings." + listing.id;
            config.set(path + ".seller", listing.seller.toString());
            config.set(path + ".seller-name", listing.sellerName);
            config.set(path + ".resource-id", listing.resourceId);
            config.set(path + ".amount", listing.amount);
            config.set(path + ".price-per-unit", listing.pricePerUnit);
            config.set(path + ".listed-time", listing.listedTime);
            config.set(path + ".active", listing.active);
        }

        // Save pending earnings
        for (Map.Entry<UUID, Double> entry : playerEarnings.entrySet()) {
            config.set("earnings." + entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save marketplace data!");
            e.printStackTrace();
        }
    }

    /**
     * Create a new listing
     */
    public String createListing(Player seller, String resourceId, int amount, double pricePerUnit) {
        // Verify resource exists
        ResourceItem resource = plugin.getResourceManager().getResource(resourceId);
        if (resource == null) {
            return null;
        }

        // Check listing limit from config + research buffs
        int baseMaxListings = plugin.getConfig().getInt("marketplace.max-listings-per-player", 10);
        int additionalListings = plugin.getResearchManager().getAdditionalListingLimit(seller.getUniqueId());
        int maxListings = baseMaxListings + additionalListings;

        List<String> existing = playerListings.getOrDefault(seller.getUniqueId(), new ArrayList<>());
        if (existing.size() >= maxListings) {
            return null;
        }

        // Take items from player inventory
        boolean hasItems = plugin.getResourceManager().takeResource(seller, resourceId, amount);
        if (!hasItems) {
            return null; // Player doesn't have enough items
        }

        // Create listing
        String listingId = UUID.randomUUID().toString().substring(0, 8);
        MarketListing listing = new MarketListing(
                listingId,
                seller.getUniqueId(),
                seller.getName(),
                resourceId,
                amount,
                pricePerUnit,
                System.currentTimeMillis(),
                true);

        listings.put(listingId, listing);
        playerListings.computeIfAbsent(seller.getUniqueId(), k -> new ArrayList<>()).add(listingId);

        saveAll();
        return listingId;
    }

    /**
     * Purchase from a listing
     */
    public boolean purchaseListing(Player buyer, String listingId, int amount) {
        MarketListing listing = listings.get(listingId);
        if (listing == null || !listing.active) {
            return false;
        }

        // Can't buy own listing
        if (listing.seller.equals(buyer.getUniqueId())) {
            return false;
        }

        // Check amount
        if (amount > listing.amount || amount <= 0) {
            return false;
        }

        // Calculate price
        double totalPrice = listing.pricePerUnit * amount;
        double taxRate = plugin.getConfig().getDouble("marketplace.tax-rate", 5.0) / 100.0;
        double tax = totalPrice * taxRate;
        double sellerReceives = totalPrice - tax;

        // Check buyer has enough money
        if (!plugin.getEconomy().has(buyer, totalPrice)) {
            return false;
        }

        // Process transaction
        plugin.getEconomy().withdrawPlayer(buyer, totalPrice);

        // Add to seller's pending earnings
        playerEarnings.merge(listing.seller, sellerReceives, Double::sum);

        // Give items to buyer
        plugin.getResourceManager().giveResource(buyer, listing.resourceId, amount);

        // Update listing
        listing.amount -= amount;
        if (listing.amount <= 0) {
            removeListing(listingId);
        }

        // Notify seller if online
        Player seller = Bukkit.getPlayer(listing.seller);
        if (seller != null) {
            ResourceItem resource = plugin.getResourceManager().getResource(listing.resourceId);
            String resourceName = resource != null ? resource.getName() : listing.resourceId;
            seller.sendMessage("§a" + buyer.getName() + " purchased " + amount + "x " + resourceName +
                    " for $" + String.format("%.2f", totalPrice) + "!");
        }

        saveAll();
        return true;
    }

    /**
     * Cancel a listing
     */
    public boolean cancelListing(Player player, String listingId) {
        MarketListing listing = listings.get(listingId);
        if (listing == null || !listing.seller.equals(player.getUniqueId())) {
            return false;
        }

        // Return items to player
        plugin.getResourceManager().giveResource(player, listing.resourceId, listing.amount);

        removeListing(listingId);
        saveAll();
        return true;
    }

    private void removeListing(String listingId) {
        MarketListing listing = listings.remove(listingId);
        if (listing != null) {
            List<String> playerList = playerListings.get(listing.seller);
            if (playerList != null) {
                playerList.remove(listingId);
            }
        }
    }

    /**
     * Collect pending earnings
     */
    public double collectEarnings(Player player) {
        Double earnings = playerEarnings.remove(player.getUniqueId());
        if (earnings == null || earnings <= 0) {
            return 0;
        }

        plugin.getEconomy().depositPlayer(player, earnings);
        saveAll();
        return earnings;
    }

    /**
     * Get pending earnings amount
     */
    public double getPendingEarnings(UUID playerId) {
        return playerEarnings.getOrDefault(playerId, 0.0);
    }

    /**
     * Get all active listings
     */
    public List<MarketListing> getAllListings() {
        return new ArrayList<>(listings.values());
    }

    /**
     * Get listings sorted by resource
     */
    public List<MarketListing> getListingsByResource(String resourceId) {
        return listings.values().stream()
                .filter(l -> l.resourceId.equals(resourceId) && l.active)
                .sorted(Comparator.comparingDouble(l -> l.pricePerUnit))
                .collect(Collectors.toList());
    }

    /**
     * Get listings by seller
     */
    public List<MarketListing> getPlayerListings(UUID playerId) {
        return playerListings.getOrDefault(playerId, new ArrayList<>()).stream()
                .map(listings::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get listing by ID
     */
    public MarketListing getListing(String listingId) {
        return listings.get(listingId);
    }

    /**
     * Get all unique resources being sold
     */
    public Set<String> getAvailableResources() {
        return listings.values().stream()
                .filter(l -> l.active)
                .map(l -> l.resourceId)
                .collect(Collectors.toSet());
    }

    /**
     * Clean up expired listings
     */
    public void cleanupExpiredListings() {
        long baseExpirationTime = plugin.getConfig().getLong("marketplace.listing-expiration-hours", 168) * 60 * 60
                * 1000;
        long currentTime = System.currentTimeMillis();

        List<String> toRemove = new ArrayList<>();
        for (MarketListing listing : listings.values()) {
            long additionalHours = plugin.getResearchManager().getAdditionalListingHours(listing.seller);
            long expirationTime = baseExpirationTime + (additionalHours * 60 * 60 * 1000);

            if (currentTime - listing.listedTime > expirationTime) {
                toRemove.add(listing.id);

                // Return items to seller
                Player seller = Bukkit.getPlayer(listing.seller);
                if (seller != null) {
                    plugin.getResourceManager().giveResource(seller, listing.resourceId, listing.amount);
                    seller.sendMessage("§eYour marketplace listing has expired and items were returned.");
                }
                // If offline, items are lost (or could be mailed)
            }
        }

        toRemove.forEach(this::removeListing);
        if (!toRemove.isEmpty()) {
            saveAll();
        }
    }

    // Market listing data class
    public static class MarketListing {
        public final String id;
        public final UUID seller;
        public final String sellerName;
        public final String resourceId;
        public int amount;
        public final double pricePerUnit;
        public final long listedTime;
        public boolean active;

        public MarketListing(String id, UUID seller, String sellerName, String resourceId,
                int amount, double pricePerUnit, long listedTime, boolean active) {
            this.id = id;
            this.seller = seller;
            this.sellerName = sellerName;
            this.resourceId = resourceId;
            this.amount = amount;
            this.pricePerUnit = pricePerUnit;
            this.listedTime = listedTime;
            this.active = active;
        }

        public double getTotalPrice() {
            return amount * pricePerUnit;
        }
    }
}
