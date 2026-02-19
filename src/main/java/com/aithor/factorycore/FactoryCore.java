package com.aithor.factorycore;

import com.aithor.factorycore.commands.FactoryCoreCommand;
import com.aithor.factorycore.gui.HubGUI;
import com.aithor.factorycore.gui.OutputStorageGUI;
import com.aithor.factorycore.listeners.*;
import com.aithor.factorycore.managers.*;
import com.aithor.factorycore.utils.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class FactoryCore extends JavaPlugin {

    private static FactoryCore instance;
    private Economy economy;

    // Managers
    private FactoryManager factoryManager;
    private ResourceManager resourceManager;
    private NPCManager npcManager;
    private InvoiceManager invoiceManager;
    private LanguageManager languageManager;
    private StorageManager storageManager;
    private RecipeManager recipeManager;
    private TaxManager taxManager;
    private MarketplaceManager marketplaceManager;

    // Listeners
    private HubClickListener hubClickListener;

    @Override
    public void onEnable() {
        instance = this;

        // Check dependencies
        if (!checkDependencies()) {
            getLogger().severe("Missing dependencies! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Setup Vault Economy
        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Load configurations
        saveDefaultConfig();
        createDefaultConfigs();

        // Initialize logger
        com.aithor.factorycore.utils.Logger.init(this);

        // Initialize managers
        initializeManagers();

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        // Start schedulers
        startSchedulers();

        // Register PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new FactoryCorePlaceholder(this).register();
            getLogger().info("PlaceholderAPI hooked successfully!");
        }

        com.aithor.factorycore.utils.Logger.log("FactoryCore v" + getDescription().getVersion() + " has been enabled!");
        getLogger().info("Plugin by aithor - Successfully loaded!");
        getLogger().info("New Hub System enabled! Use /fc hub to access.");
    }

    @Override
    public void onDisable() {
        // Shutdown NPC manager first (removes entities & holograms from world)
        if (npcManager != null) {
            npcManager.shutdown();
        }

        // Save all data
        if (factoryManager != null) {
            factoryManager.saveAll();
        }
        if (storageManager != null) {
            storageManager.saveAll();
        }
        if (invoiceManager != null) {
            invoiceManager.saveAll();
        }
        if (taxManager != null) {
            taxManager.saveAll();
        }
        if (marketplaceManager != null) {
            marketplaceManager.saveAll();
        }

        Logger.log("FactoryCore has been disabled!");
        getLogger().info("All data saved successfully!");
    }

    private boolean checkDependencies() {
        String[] required = { "Vault", "WorldGuard", "WorldEdit" };
        for (String plugin : required) {
            if (getServer().getPluginManager().getPlugin(plugin) == null) {
                getLogger().severe("Required dependency '" + plugin + "' not found!");
                return false;
            }
        }
        return true;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void createDefaultConfigs() {
        // Save default configs if they don't exist
        if (!new java.io.File(getDataFolder(), "resources.yml").exists()) {
            saveResource("resources.yml", false);
        }
        if (!new java.io.File(getDataFolder(), "language.yml").exists()) {
            saveResource("language.yml", false);
        }
        if (!new java.io.File(getDataFolder(), "recipes.yml").exists()) {
            saveResource("recipes.yml", false);
        }
        if (!new java.io.File(getDataFolder(), "npc.yml").exists()) {
            saveResource("npc.yml", false);
        }
    }

    private void initializeManagers() {
        languageManager = new LanguageManager(this);
        resourceManager = new ResourceManager(this);
        recipeManager = new RecipeManager(this);
        factoryManager = new FactoryManager(this);
        npcManager = new NPCManager(this);
        invoiceManager = new InvoiceManager(this);
        storageManager = new StorageManager(this);

        // New managers for hub system
        taxManager = new TaxManager(this);
        marketplaceManager = new MarketplaceManager(this);
    }

    private void registerCommands() {
        getCommand("factorycore").setExecutor(new FactoryCoreCommand(this));
        getCommand("fc").setExecutor(new FactoryCoreCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDamageListener(this), this);

        // Register hub click listener
        hubClickListener = new HubClickListener(this);
        getServer().getPluginManager().registerEvents(hubClickListener, this);
    }

    private void startSchedulers() {
        // Tax scheduler (every 3 days by default)
        int taxInterval = getConfig().getInt("tax.interval-ticks", 144000); // 3 days in ticks
        getServer().getScheduler().runTaskTimer(this, () -> {
            // Generate tax invoices using the old system
            invoiceManager.generateTaxInvoices();

            // Also assess taxes using the new tax manager
            if (taxManager != null) {
                taxManager.assessTaxes();
            }
        }, taxInterval, taxInterval);

        // Salary scheduler (every 24 hours by default)
        int salaryInterval = getConfig().getInt("salary.interval-ticks", 24000); // 1 day in ticks
        getServer().getScheduler().runTaskTimer(this, () -> {
            invoiceManager.generateSalaryInvoices();
        }, salaryInterval, salaryInterval);

        // Production checker
        getServer().getScheduler().runTaskTimer(this, () -> {
            factoryManager.updateProduction();
        }, 20L, 20L); // Every second

        // Tax overdue checker (every hour)
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (taxManager != null) {
                taxManager.checkOverdueTaxes();
            }
        }, 72000L, 72000L); // Every hour

        // Marketplace cleanup (every 6 hours)
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (marketplaceManager != null) {
                marketplaceManager.cleanupExpiredListings();
            }
        }, 432000L, 432000L); // Every 6 hours
    }

    // ==================== STATIC & GETTERS ====================

    public static FactoryCore getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public FactoryManager getFactoryManager() {
        return factoryManager;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public NPCManager getNPCManager() {
        return npcManager;
    }

    public InvoiceManager getInvoiceManager() {
        return invoiceManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    // New manager getters
    public TaxManager getTaxManager() {
        return taxManager;
    }

    public MarketplaceManager getMarketplaceManager() {
        return marketplaceManager;
    }

    public HubClickListener getHubClickListener() {
        return hubClickListener;
    }

    public OutputStorageGUI getOutputStorageGUI(Player player, String factoryId) {
        return new OutputStorageGUI(this, player, factoryId);
    }

    // ==================== HUB HELPER METHODS ====================

    /**
     * Open the main hub GUI for a player
     * 
     * @param player The player to open the hub for
     */
    public void openHub(Player player) {
        HubGUI hubGUI = new HubGUI(this, player);
        hubGUI.openHubMenu();
    }
}
