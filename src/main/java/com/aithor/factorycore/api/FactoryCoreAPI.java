package com.aithor.factorycore.api;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.*;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * FactoryCore API
 * Use this API for integration with other plugins
 * 
 * @author aithor
 * @version 1.21.4
 */
public class FactoryCoreAPI {
    
    private final FactoryCore plugin;
    
    public FactoryCoreAPI() {
        this.plugin = FactoryCore.getInstance();
    }
    
    /**
     * Get the FactoryCore plugin instance
     * @return FactoryCore instance
     */
    public FactoryCore getPlugin() {
        return plugin;
    }
    
    // ==================== FACTORY METHODS ====================
    
    /**
     * Get a factory by ID
     * @param factoryId The factory ID
     * @return Factory object or null if not found
     */
    public Factory getFactory(String factoryId) {
        return plugin.getFactoryManager().getFactory(factoryId);
    }
    
    /**
     * Get all factories
     * @return List of all factories
     */
    public List<Factory> getAllFactories() {
        return plugin.getFactoryManager().getAllFactories();
    }
    
    /**
     * Get all factories owned by a player
     * @param owner Player UUID
     * @return List of factories owned by player
     */
    public List<Factory> getFactoriesByOwner(UUID owner) {
        return plugin.getFactoryManager().getFactoriesByOwner(owner);
    }
    
    /**
     * Create a new factory
     * @param id Factory ID
     * @param regionName WorldGuard region name
     * @param type Factory type
     * @param price Factory price
     * @return Created Factory object or null if failed
     */
    public Factory createFactory(String id, String regionName, FactoryType type, double price) {
        return plugin.getFactoryManager().createFactory(id, regionName, type, price);
    }
    
    /**
     * Remove a factory
     * @param factoryId Factory ID to remove
     * @return true if successful, false otherwise
     */
    public boolean removeFactory(String factoryId) {
        return plugin.getFactoryManager().removeFactory(factoryId);
    }
    
    /**
     * Check if a player owns a specific factory
     * @param player Player to check
     * @param factoryId Factory ID
     * @return true if player owns the factory, false otherwise
     */
    public boolean ownsFactory(Player player, String factoryId) {
        Factory factory = getFactory(factoryId);
        return factory != null && factory.getOwner() != null && 
               factory.getOwner().equals(player.getUniqueId());
    }
    
    /**
     * Get factory status
     * @param factoryId Factory ID
     * @return FactoryStatus or null if factory not found
     */
    public FactoryStatus getFactoryStatus(String factoryId) {
        Factory factory = getFactory(factoryId);
        return factory != null ? factory.getStatus() : null;
    }
    
    /**
     * Check if factory is currently producing
     * @param factoryId Factory ID
     * @return true if factory is producing, false otherwise
     */
    public boolean isProducing(String factoryId) {
        Factory factory = getFactory(factoryId);
        return factory != null && factory.getStatus() == FactoryStatus.RUNNING;
    }
    
    /**
     * Upgrade a factory level
     * @param player Player upgrading the factory
     * @param factoryId Factory ID
     * @return true if upgrade successful, false otherwise
     */
    public boolean upgradeFactory(Player player, String factoryId) {
        return plugin.getFactoryManager().upgradeFactory(player, factoryId);
    }
    
    // ==================== RESOURCE METHODS ====================
    
    /**
     * Get a resource item by ID
     * @param resourceId Resource ID
     * @return ResourceItem or null if not found
     */
    public ResourceItem getResource(String resourceId) {
        return plugin.getResourceManager().getResource(resourceId);
    }
    
    /**
     * Create an ItemStack from a resource
     * @param resourceId Resource ID
     * @param amount Item amount
     * @return ItemStack or null if resource not found
     */
    public org.bukkit.inventory.ItemStack createResourceItem(String resourceId, int amount) {
        return plugin.getResourceManager().createItemStack(resourceId, amount);
    }
    
    /**
     * Give a resource to a player
     * @param player Player to give resource to
     * @param resourceId Resource ID
     * @param amount Amount to give
     * @return true if successful, false otherwise
     */
    public boolean giveResource(Player player, String resourceId, int amount) {
        return plugin.getResourceManager().giveResource(player, resourceId, amount);
    }
    
    // ==================== STORAGE METHODS ====================
    
    /**
     * Add item to factory storage
     * @param factoryId Factory ID
     * @param resourceId Resource ID
     * @param amount Amount to add
     */
    public void addToStorage(String factoryId, String resourceId, int amount) {
        plugin.getStorageManager().addItem(factoryId, resourceId, amount);
    }
    
    /**
     * Remove item from factory storage
     * @param factoryId Factory ID
     * @param resourceId Resource ID
     * @param amount Amount to remove
     * @return true if successful, false if insufficient items
     */
    public boolean removeFromStorage(String factoryId, String resourceId, int amount) {
        return plugin.getStorageManager().removeItem(factoryId, resourceId, amount);
    }
    
    /**
     * Get amount of resource in factory storage
     * @param factoryId Factory ID
     * @param resourceId Resource ID
     * @return Amount in storage
     */
    public int getStorageAmount(String factoryId, String resourceId) {
        return plugin.getStorageManager().getAmount(factoryId, resourceId);
    }
    
    /**
     * Get all items in factory storage
     * @param factoryId Factory ID
     * @return Map of resource ID to amount
     */
    public java.util.Map<String, Integer> getFactoryStorage(String factoryId) {
        return plugin.getStorageManager().getStorage(factoryId);
    }
    
    // ==================== PRODUCTION METHODS ====================
    
    /**
     * Start production in a factory
     * @param factoryId Factory ID
     * @param recipeId Recipe ID to produce
     */
    public void startProduction(String factoryId, String recipeId) {
        Factory factory = getFactory(factoryId);
        if (factory != null) {
            plugin.getFactoryManager().startProduction(factory, recipeId);
        }
    }
    
    /**
     * Get current production task
     * @param factoryId Factory ID
     * @return ProductionTask or null if not producing
     */
    public ProductionTask getCurrentProduction(String factoryId) {
        Factory factory = getFactory(factoryId);
        return factory != null ? factory.getCurrentProduction() : null;
    }
    
    /**
     * Get production progress percentage
     * @param factoryId Factory ID
     * @return Progress percentage (0.0 to 1.0) or 0.0 if not producing
     */
    public double getProductionProgress(String factoryId) {
        Factory factory = getFactory(factoryId);
        if (factory != null && factory.getCurrentProduction() != null) {
            return factory.getCurrentProduction().getProgress();
        }
        return 0.0;
    }
    
    // ==================== RECIPE METHODS ====================
    
    /**
     * Get a recipe by ID
     * @param recipeId Recipe ID
     * @return Recipe or null if not found
     */
    public Recipe getRecipe(String recipeId) {
        return plugin.getRecipeManager().getRecipe(recipeId);
    }
    
    /**
     * Get all recipes for a factory type
     * @param factoryType Factory type ID
     * @return List of recipes
     */
    public List<Recipe> getRecipesByFactoryType(String factoryType) {
        return plugin.getRecipeManager().getRecipesByFactoryType(factoryType);
    }
    
    /**
     * Get all recipes
     * @return Map of recipe ID to Recipe
     */
    public java.util.Map<String, Recipe> getAllRecipes() {
        return plugin.getRecipeManager().getAllRecipes();
    }
    
    // ==================== INVOICE METHODS ====================
    
    /**
     * Get all invoices for a player
     * @param player Player UUID
     * @return List of unpaid invoices
     */
    public List<Invoice> getPlayerInvoices(UUID player) {
        return plugin.getInvoiceManager().getInvoicesByOwner(player);
    }
    
    /**
     * Pay an invoice
     * @param player Player paying the invoice
     * @param invoiceId Invoice ID
     * @return true if payment successful, false otherwise
     */
    public boolean payInvoice(Player player, String invoiceId) {
        return plugin.getInvoiceManager().payInvoice(player, invoiceId);
    }
    
    /**
     * Get an invoice by ID
     * @param invoiceId Invoice ID
     * @return Invoice or null if not found
     */
    public Invoice getInvoice(String invoiceId) {
        return plugin.getInvoiceManager().getInvoice(invoiceId);
    }
    
    // ==================== NPC METHODS ====================
    
    /**
     * Spawn an NPC for a factory at a location
     * @param factoryId Factory ID
     * @param npcId NPC ID
     * @param location Location to spawn
     * @return true if successful, false otherwise
     */
    public boolean spawnNPC(String factoryId, String npcId, org.bukkit.Location location) {
        return plugin.getNPCManager().spawnNPC(factoryId, npcId, location);
    }

    /**
     * Spawn an NPC with a specific template
     * @param factoryId The factory ID
     * @param npcId The NPC ID
     * @param location The spawn location
     * @param template The NPC template to use
     * @return true if successful
     */
    public boolean spawnNPCWithTemplate(String factoryId, String npcId, org.bukkit.Location location, String template) {
        return plugin.getNPCManager().spawnNPCWithTemplate(factoryId, npcId, location, template);
    }
    
    /**
     * Remove an NPC
     * @param npcId NPC ID
     * @return true if successful, false otherwise
     */
    public boolean removeNPC(String npcId) {
        return plugin.getNPCManager().removeNPC(npcId);
    }
    
    /**
     * Check if an entity is a factory NPC
     * @param entityUUID Entity UUID
     * @return true if entity is an NPC, false otherwise
     */
    public boolean isFactoryNPC(UUID entityUUID) {
        return plugin.getNPCManager().isNPC(entityUUID);
    }
    
    /**
     * Get factory ID associated with an NPC
     * @param entityUUID Entity UUID
     * @return Factory ID or null if not found
     */
    public String getFactoryIdByNPC(UUID entityUUID) {
        return plugin.getNPCManager().getFactoryIdByEntity(entityUUID);
    }
    
    // ==================== ECONOMY METHODS ====================
    
    /**
     * Get factory price
     * @param factoryId Factory ID
     * @return Factory price or 0.0 if not found
     */
    public double getFactoryPrice(String factoryId) {
        Factory factory = getFactory(factoryId);
        return factory != null ? factory.getPrice() : 0.0;
    }
    
    /**
     * Get factory sell price (usually 50% of buy price)
     * @param factoryId Factory ID
     * @return Sell price or 0.0 if not found
     */
    public double getFactorySellPrice(String factoryId) {
        Factory factory = getFactory(factoryId);
        if (factory != null) {
            double multiplier = plugin.getConfig().getDouble("factory.sell-price-multiplier", 0.5);
            return factory.getPrice() * multiplier;
        }
        return 0.0;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Get localized message from language.yml
     * @param key Message key
     * @return Localized message
     */
    public String getMessage(String key) {
        return plugin.getLanguageManager().getMessage(key);
    }
    
    /**
     * Reload plugin configuration
     */
    public void reloadPlugin() {
        plugin.reloadConfig();
        plugin.getLanguageManager().reload();
        plugin.getResourceManager().reload();
        plugin.getRecipeManager().reload();
    }
}

// ==================== EXAMPLE USAGE ====================

/**
 * Example: How to use FactoryCore API in your plugin
 * 
 * 1. Add FactoryCore as dependency in your plugin.yml:
 *    depend: [FactoryCore]
 * 
 * 2. Use the API in your code:
 * 
 * public class YourPlugin extends JavaPlugin {
 *     
 *     private FactoryCoreAPI api;
 *     
 *     @Override
 *     public void onEnable() {
 *         // Initialize API
 *         api = new FactoryCoreAPI();
 *         
 *         // Example: Check if player owns factories
 *         Player player = Bukkit.getPlayer("PlayerName");
 *         List<Factory> factories = api.getFactoriesByOwner(player.getUniqueId());
 *         getLogger().info(player.getName() + " owns " + factories.size() + " factories");
 *         
 *         // Example: Give resource to player
 *         api.giveResource(player, "steel_ingot", 10);
 *         
 *         // Example: Check factory status
 *         if (api.isProducing("factory1")) {
 *             getLogger().info("Factory1 is currently producing!");
 *             double progress = api.getProductionProgress("factory1");
 *             getLogger().info("Progress: " + (int)(progress * 100) + "%");
 *         }
 *         
 *         // Example: Add item to factory storage
 *         api.addToStorage("factory1", "iron_ore", 50);
 *         
 *         // Example: Get all player invoices
 *         List<Invoice> invoices = api.getPlayerInvoices(player.getUniqueId());
 *         getLogger().info(player.getName() + " has " + invoices.size() + " unpaid invoices");
 *     }
 * }
 * 
 * 3. Listen to FactoryCore events (create custom events):
 * 
 * // Event when production completes
 * public class ProductionCompleteEvent extends Event {
 *     private static final HandlerList handlers = new HandlerList();
 *     private final Factory factory;
 *     private final Recipe recipe;
 *     
 *     public ProductionCompleteEvent(Factory factory, Recipe recipe) {
 *         this.factory = factory;
 *         this.recipe = recipe;
 *     }
 *     
 *     public Factory getFactory() { return factory; }
 *     public Recipe getRecipe() { return recipe; }
 *     
 *     public HandlerList getHandlers() { return handlers; }
 *     public static HandlerList getHandlerList() { return handlers; }
 * }
 * 
 * // Listen to the event
 * @EventHandler
 * public void onProductionComplete(ProductionCompleteEvent event) {
 *     Factory factory = event.getFactory();
 *     Recipe recipe = event.getRecipe();
 *     getLogger().info("Factory " + factory.getId() + " completed producing " + recipe.getName());
 * }
 */