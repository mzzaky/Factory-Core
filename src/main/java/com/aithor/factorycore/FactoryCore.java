package com.aithor.factorycore;

import com.aithor.factorycore.commands.FactoryCoreCommand;
import com.aithor.factorycore.gui.HubGUI;
import com.aithor.factorycore.gui.OutputStorageGUI;
import com.aithor.factorycore.hooks.ExecutableItemsHook;
import com.aithor.factorycore.hooks.MMOItemsHook;
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
    private ResearchManager researchManager;
    private AchievementManager achievementManager;

    // Hooks
    private MMOItemsHook mmoItemsHook;
    private ExecutableItemsHook executableItemsHook;

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
        createResearchConfig();
        createAchievementConfig();

        // Initialize logger
        com.aithor.factorycore.utils.Logger.init(this);

        // Initialize hooks (before managers, since managers may depend on hooks)
        initializeHooks();

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
        printStartupBanner();
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
        if (researchManager != null) {
            researchManager.saveAll();
        }
        if (achievementManager != null) {
            achievementManager.saveAll();
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

    private void createResearchConfig() {
        if (!new java.io.File(getDataFolder(), "research.yml").exists()) {
            saveResource("research.yml", false);
        }
    }

    private void createAchievementConfig() {
        if (!new java.io.File(getDataFolder(), "achievement.yml").exists()) {
            saveResource("achievement.yml", false);
        }
    }

    private void initializeHooks() {
        // MMOItems integration
        mmoItemsHook = new MMOItemsHook(this);
        // ExecutableItems integration
        executableItemsHook = new ExecutableItemsHook(this);
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
        researchManager = new ResearchManager(this);
        achievementManager = new AchievementManager(this);
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
        long nextTaxMs = taxManager != null ? taxManager.getTimeUntilNextCollection() : taxInterval * 50L;
        long initialTaxDelay = nextTaxMs > 0 ? (nextTaxMs / 50) + 1 : 20L;
        getServer().getScheduler().runTaskTimer(this, () -> {
            // Generate tax invoices using the old system
            invoiceManager.generateTaxInvoices();

            // Also assess taxes using the new tax manager
            if (taxManager != null) {
                taxManager.assessTaxes();
            }
        }, initialTaxDelay, taxInterval);

        // Salary scheduler (every 24 hours by default)
        int salaryInterval = getConfig().getInt("salary.interval-ticks", 24000); // 1 day in ticks
        long nextSalaryMs = invoiceManager != null ? invoiceManager.getTimeUntilNextSalary() : salaryInterval * 50L;
        long initialSalaryDelay = nextSalaryMs > 0 ? (nextSalaryMs / 50) + 1 : 20L;
        getServer().getScheduler().runTaskTimer(this, () -> {
            invoiceManager.generateSalaryInvoices();
        }, initialSalaryDelay, salaryInterval);

        // Production + upgrade checker
        getServer().getScheduler().runTaskTimer(this, () -> {
            factoryManager.updateProduction();
            factoryManager.updateUpgrades();
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

        // Research completion checker (every 10 seconds)
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (researchManager != null) {
                researchManager.updateResearch();
            }
        }, 200L, 200L); // Every 10 seconds
    }

    // ==================== STARTUP BANNER ====================
    private void printStartupBanner() {
        org.bukkit.command.ConsoleCommandSender console = getServer().getConsoleSender();
        String version = getDescription().getVersion();

        console.sendMessage("§8========================================================");
        console.sendMessage("§b  ______         _                   §3____               ");
        console.sendMessage("§b |  ____|       | |                 §3/ __ \\              ");
        console.sendMessage("§b | |__ __ _  ___| |_ ___  _ __ _   §3| |  | | ___  _ __ ___ ");
        console.sendMessage("§b |  __/ _` |/ __| __/ _ \\| '__| | | §3| |  | |/ _ \\| '__/ _ \\");
        console.sendMessage("§b | | | (_| | (__| || (_) | |  | |_| §3| |__| | (_) | | |  __/");
        console.sendMessage("§b |_|  \\__,_|\\___|\\__\\___/|_|   \\__, §3|\\____/ \\___/|_|  \\___|");
        console.sendMessage("§b                                __/ |                   ");
        console.sendMessage("§b                               |___/                    ");
        console.sendMessage("§8========================================================");
        console.sendMessage("§e  FactoryCore §7| §fVersion: §a" + version);
        console.sendMessage("§e  Author: §baithor");
        console.sendMessage("§8========================================================");
        console.sendMessage("");
        console.sendMessage("§6[Dependencies Status]");
        console.sendMessage("§7- §aVault §8(Economy hook established)");
        console.sendMessage("§7- §aWorldGuard §8(Region handling ready)");
        console.sendMessage("§7- §aWorldEdit §8(Selection handling ready)");
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            console.sendMessage("§7- §aPlaceholderAPI §8(Placeholders registered)");
        } else {
            console.sendMessage("§7- §ePlaceholderAPI §8(Not found, placeholders disabled)");
        }
        if (mmoItemsHook != null && mmoItemsHook.isEnabled()) {
            console.sendMessage("§7- §aMMOItems §8(Item integration active)");
        } else {
            console.sendMessage("§7- §eMMOItems §8(Not found, MMOItems integration disabled)");
        }
        if (executableItemsHook != null && executableItemsHook.isEnabled()) {
            console.sendMessage("§7- §aExecutableItems §8(Item integration active)");
        } else {
            console.sendMessage("§7- §eExecutableItems §8(Not found, ExecutableItems integration disabled)");
        }
        console.sendMessage("");
        console.sendMessage("§6[Systems Loaded]");
        console.sendMessage("§7- §aFactory Management §8(" + factoryManager.getAllFactories().size() + " active)");
        console.sendMessage("§7- §aEconomy & Taxation §8(Active)");
        console.sendMessage("§7- §aGlobal Marketplace §8(Active)");
        console.sendMessage("§7- §aResearch Center §8(Active)");
        console.sendMessage(
                "§7- §aAchievement System §8(" + achievementManager.getTotalAchievementCount() + " achievements)");
        console.sendMessage("§7- §aNPC Workforce System §8(Active)");
        console.sendMessage("§7- §aInvoice & Storage System §8(Active)");
        console.sendMessage("");
        console.sendMessage("§8========================================================");
        console.sendMessage("§a✔ Plugin successfully initialized and ready for use!");
        console.sendMessage("§8========================================================");
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

    public ResearchManager getResearchManager() {
        return researchManager;
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public MMOItemsHook getMMOItemsHook() {
        return mmoItemsHook;
    }

    public ExecutableItemsHook getExecutableItemsHook() {
        return executableItemsHook;
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
