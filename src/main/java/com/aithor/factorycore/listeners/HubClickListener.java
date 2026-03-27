package com.aithor.factorycore.listeners;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.gui.*;
import com.aithor.factorycore.managers.DailyQuestManager;
import com.aithor.factorycore.managers.ResearchManager;
import com.aithor.factorycore.managers.MarketplaceManager;
import com.aithor.factorycore.managers.TaxManager;
import com.aithor.factorycore.models.*;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HubClickListener - Handles all click events for the Hub GUI system
 */
public class HubClickListener implements Listener {

    private final FactoryCore plugin;

    // Store GUI instances for players to maintain state (filters, pages, etc.)
    private final Map<UUID, FactoryBrowseGUI> browseGUIs = new HashMap<>();
    private final Map<UUID, MyFactoriesGUI> myFactoriesGUIs = new HashMap<>();
    private final Map<UUID, InvoiceCenterGUI> invoiceCenterGUIs = new HashMap<>();
    private final Map<UUID, TaxCenterGUI> taxCenterGUIs = new HashMap<>();
    private final Map<UUID, EmployeesCenterGUI> employeesCenterGUIs = new HashMap<>();
    private final Map<UUID, EmployeeShopGUI> employeeShopGUIs = new HashMap<>();
    private final Map<UUID, HelpInfoGUI> helpInfoGUIs = new HashMap<>();
    private final Map<UUID, MarketplaceGUI> marketplaceGUIs = new HashMap<>();
    private final Map<UUID, ResearchGUI> researchGUIs = new HashMap<>();
    private final Map<UUID, AchievementGUI> achievementGUIs = new HashMap<>();
    private final Map<UUID, DailyQuestGUI> dailyQuestGUIs = new HashMap<>();
    private final Map<UUID, RecipesWikiGUI> recipesWikiGUIs = new HashMap<>();

    public HubClickListener(FactoryCore plugin) {
        this.plugin = plugin;
    }

    // ── Block drag-and-drop into/from all Hub GUIs ───────────────────────────
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        String title = event.getView().getTitle();
        if (isHubGUI(title)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack clicked = event.getCurrentItem();

        // Check if it's a hub-related GUI
        if (!isHubGUI(title))
            return;

        // Cancel ALL click actions in Hub GUIs to prevent any item manipulation
        event.setCancelled(true);

        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        // Handle bottom inventory clicks (player's inventory) immediately
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv != null && clickedInv.equals(player.getInventory())) {
            if (title.contains("Marketplace") && title.contains("Sell Items")) {
                String clickedResourceId = plugin.getResourceManager().getResourceId(clicked);
                if (clickedResourceId != null) {
                    MarketplaceGUI gui = marketplaceGUIs.getOrDefault(player.getUniqueId(),
                            new MarketplaceGUI(plugin, player));
                    gui.openSellConfirmation(clickedResourceId, clicked.getAmount());
                } else {
                    player.sendMessage("§cThis item cannot be sold. Only valid factory items can be sold!");
                }
            }
            return;
        }

        if (clickedInv == null)
            return;

        if (!clicked.hasItemMeta())
            return;

        ItemMeta meta = clicked.getItemMeta();
        String name = meta.getDisplayName();

        // Route to appropriate handler based on GUI title
        String configuredHubTitle = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                plugin.getMainMenuConfig().getString("gui.title", "Main Hub"));

        if (title.equals(configuredHubTitle) || title.contains("Main Hub")) {
            handleHubClick(player, clicked, meta, name);
        } else if (title.contains("Factory Browse")) {
            handleFactoryBrowseClick(player, clicked, meta, name, event.getClick());
        } else if (title.contains("My Factories")) {
            handleMyFactoriesClick(player, clicked, meta, name, event.getClick());
        } else if (title.contains("Invoice Center")) {
            handleInvoiceCenterClick(player, clicked, meta, name);
        } else if (title.contains("Tax Center")) {
            handleTaxCenterClick(player, clicked, meta, name);
        } else if (title.contains("Employees Center")) {
            handleEmployeesCenterClick(player, clicked, meta, name, event.getClick());
        } else if (title.contains("Employee Shop") || title.contains("My Employees")
                || title.contains("Assign Employee")) {
            handleEmployeeShopClick(player, clicked, meta, name, event.getClick());
        } else if (title.contains("Help & Info")) {
            handleHelpInfoClick(player, clicked, meta, name);
        } else if (title.contains("Marketplace")) {
            handleMarketplaceClick(player, clicked, meta, name, event.getClick());
        } else if (title.contains("Research Center")) {
            handleResearchCenterClick(player, clicked, meta, name);
        } else if (title.contains("Research:")) {
            handleResearchDetailClick(player, clicked, meta, name);
        } else if (title.contains("Achievements")) {
            handleAchievementsClick(player, clicked, meta, name);
        } else if (title.contains("Daily Quests")) {
            handleDailyQuestClick(player, clicked, meta, name);
        } else if (title.contains("Recipes Wiki")) {
            handleRecipesWikiClick(player, clicked, meta, name, event.getClick());
        } else if (title.contains("Distribution Center") || title.contains("Request:")) {
            handleDistributionCenterClick(player, clicked, meta, name);
        } else if (title.contains("Create Factory") && title.contains("Select Type")) {
            handleFactoryTypeSelectClick(player, clicked, meta, name);
        } else if (title.contains("Confirm Factory Creation")) {
            handleFactoryCreateConfirmClick(player, clicked, meta, name);
        } else if (title.contains("Confirm Purchase") || title.contains("Confirm Sale") ||
                title.contains("Confirm Fire") || title.contains("Confirm Unassign") ||
                title.contains("Confirm Dismiss") || title.contains("Create Listing")) {
            handleConfirmationClick(player, clicked, meta, name, title);
        }
    }

    private boolean isHubGUI(String title) {
        String configuredHubTitle = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                plugin.getMainMenuConfig().getString("gui.title", "Main Hub"));

        return title.equals(configuredHubTitle) ||
                title.contains("Main Hub") ||
                title.contains("Factory Browse") ||
                title.contains("My Factories") ||
                title.contains("Invoice Center") ||
                title.contains("Tax Center") ||
                title.contains("Employees Center") ||
                title.contains("Employee Shop") ||
                title.contains("My Employees") ||
                title.contains("Assign Employee") ||
                title.contains("Help & Info") ||
                title.contains("Marketplace") ||
                title.contains("Confirm Purchase") ||
                title.contains("Confirm Sale") ||
                title.contains("Confirm Fire") ||
                title.contains("Confirm Unassign") ||
                title.contains("Confirm Dismiss") ||
                title.contains("Create Listing") ||
                title.contains("Research Center") ||
                title.contains("Research:") ||
                title.contains("Achievements") ||
                title.contains("Daily Quests") ||
                title.contains("Create Factory") ||
                title.contains("Confirm Factory Creation") ||
                title.contains("Recipes Wiki") ||
                title.contains("Distribution Center") ||
                title.contains("Request:");
    }

    // ==================== HUB MAIN MENU ====================
    private void handleHubClick(Player player, ItemStack clicked, ItemMeta meta, String name) {
        // ── Special on_click action (configured in main_menu.yml) ────────────
        if (executeOnClick(player, meta))
            return;

        // ── Route by stable hub_gui_id PDC tag ───────────────────────────────
        String guiId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "hub_gui_id"),
                PersistentDataType.STRING);

        if (guiId != null) {
            switch (guiId) {
                case "factory_browse": {
                    playHubSound(player, "factory_browse");
                    FactoryBrowseGUI gui = new FactoryBrowseGUI(plugin, player);
                    browseGUIs.put(player.getUniqueId(), gui);
                    gui.openBrowseMenu();
                    break;
                }
                case "my_factories": {
                    playHubSound(player, "my_factories");
                    MyFactoriesGUI gui = new MyFactoriesGUI(plugin, player);
                    myFactoriesGUIs.put(player.getUniqueId(), gui);
                    gui.openMyFactoriesMenu();
                    break;
                }
                case "invoice_center": {
                    playHubSound(player, "invoice_center");
                    InvoiceCenterGUI gui = new InvoiceCenterGUI(plugin, player);
                    invoiceCenterGUIs.put(player.getUniqueId(), gui);
                    gui.openInvoiceCenterMenu();
                    break;
                }
                case "tax_center": {
                    playHubSound(player, "tax_center");
                    TaxCenterGUI gui = new TaxCenterGUI(plugin, player);
                    taxCenterGUIs.put(player.getUniqueId(), gui);
                    gui.openTaxCenterMenu();
                    break;
                }
                case "employees_center": {
                    playHubSound(player, "employees_center");
                    EmployeesCenterGUI gui = new EmployeesCenterGUI(plugin, player);
                    employeesCenterGUIs.put(player.getUniqueId(), gui);
                    gui.openEmployeesCenterMenu();
                    break;
                }
                case "marketplace": {
                    playHubSound(player, "marketplace");
                    MarketplaceGUI gui = new MarketplaceGUI(plugin, player);
                    marketplaceGUIs.put(player.getUniqueId(), gui);
                    gui.openMarketplaceMenu();
                    break;
                }
                case "achievements": {
                    playHubSound(player, "achievements");
                    AchievementGUI gui = new AchievementGUI(plugin, player);
                    achievementGUIs.put(player.getUniqueId(), gui);
                    gui.openAchievementMenu();
                    break;
                }
                case "daily_quests": {
                    playHubSound(player, "daily_quests");
                    DailyQuestGUI gui = new DailyQuestGUI(plugin, player);
                    dailyQuestGUIs.put(player.getUniqueId(), gui);
                    gui.openDailyQuestMenu();
                    break;
                }
                case "research_center": {
                    playHubSound(player, "research_center");
                    ResearchGUI gui = new ResearchGUI(plugin, player);
                    researchGUIs.put(player.getUniqueId(), gui);
                    gui.openResearchMenu();
                    break;
                }
                case "help_info": {
                    playHubSound(player, "help_info");
                    HelpInfoGUI gui = new HelpInfoGUI(plugin, player);
                    helpInfoGUIs.put(player.getUniqueId(), gui);
                    gui.openHelpMenu();
                    break;
                }
                case "recipes_wiki": {
                    playHubSound(player, "recipes_wiki");
                    RecipesWikiGUI wikiGui = new RecipesWikiGUI(plugin, player);
                    recipesWikiGUIs.put(player.getUniqueId(), wikiGui);
                    wikiGui.openWikiMenu();
                    break;
                }
                case "distribution_center": {
                    playHubSound(player, "distribution_center");
                    DistributionCenterGUI distGui = new DistributionCenterGUI(plugin, player);
                    distGui.openDistributionCenter();
                    break;
                }
                case "close_button": {
                    playHubSound(player, "close_button");
                    player.closeInventory();
                    break;
                }
                default:
                    break;
            }
            return;
        }

        // ── Legacy fallback: route by display name (border/unnamed items) ─────
        if (name.contains("Close")) {
            player.closeInventory();
        }
    }

    /**
     * Checks whether {@code meta} carries a {@code hub_on_click} PDC tag and, if
     * so, executes the configured action on behalf of {@code player}.
     *
     * <p>
     * Supported actions:
     * <ul>
     * <li>{@code console_command} – each {@code ;}-separated segment of the
     * {@code hub_on_click_value} PDC tag is dispatched as a console command
     * with {@code %player%} replaced by the player's name.</li>
     * <li>{@code open_gui_factory_browse} – open Factory Browse GUI.</li>
     * <li>{@code open_gui_my_factories} – open My Factories GUI.</li>
     * <li>{@code open_gui_invoice_center} – open Invoice Center GUI.</li>
     * <li>{@code open_gui_tax_center} – open Tax Center GUI.</li>
     * <li>{@code open_gui_employees_center} – open Employees Center GUI.</li>
     * <li>{@code open_gui_marketplace} – open Marketplace GUI.</li>
     * <li>{@code open_gui_achievements} – open Achievements GUI.</li>
     * <li>{@code open_gui_research_center} – open Research Center GUI.</li>
     * <li>{@code open_gui_daily_quests} – open Daily Quests GUI.</li>
     * <li>{@code open_gui_help_info} – open Help &amp; Info GUI.</li>
     * </ul>
     *
     * @param player the clicking player
     * @param meta   the ItemMeta of the clicked item
     * @return {@code true} if an action was found and executed (caller should
     *         {@code return} after this); {@code false} if no action was configured
     */
    private boolean executeOnClick(Player player, ItemMeta meta) {
        String action = meta.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "hub_on_click"),
                org.bukkit.persistence.PersistentDataType.STRING);

        if (action == null)
            return false;

        String value = meta.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "hub_on_click_value"),
                org.bukkit.persistence.PersistentDataType.STRING);

        if (value == null)
            value = "";

        switch (action) {
            case "console_command": {
                String playerName = player.getName();
                org.bukkit.command.ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();
                for (String cmd : value.split(";")) {
                    String finalCmd = cmd.trim().replace("%player%", playerName);
                    if (!finalCmd.isEmpty()) {
                        org.bukkit.Bukkit.dispatchCommand(console, finalCmd);
                    }
                }
                return true;
            }
            case "open_gui_factory_browse": {
                playHubSound(player, "factory_browse");
                FactoryBrowseGUI browseGui = new FactoryBrowseGUI(plugin, player);
                browseGUIs.put(player.getUniqueId(), browseGui);
                browseGui.openBrowseMenu();
                return true;
            }
            case "open_gui_my_factories": {
                playHubSound(player, "my_factories");
                MyFactoriesGUI myGui = new MyFactoriesGUI(plugin, player);
                myFactoriesGUIs.put(player.getUniqueId(), myGui);
                myGui.openMyFactoriesMenu();
                return true;
            }
            case "open_gui_invoice_center": {
                playHubSound(player, "invoice_center");
                InvoiceCenterGUI invoiceGui = new InvoiceCenterGUI(plugin, player);
                invoiceCenterGUIs.put(player.getUniqueId(), invoiceGui);
                invoiceGui.openInvoiceCenterMenu();
                return true;
            }
            case "open_gui_tax_center": {
                playHubSound(player, "tax_center");
                TaxCenterGUI taxGui = new TaxCenterGUI(plugin, player);
                taxCenterGUIs.put(player.getUniqueId(), taxGui);
                taxGui.openTaxCenterMenu();
                return true;
            }
            case "open_gui_employees_center": {
                playHubSound(player, "employees_center");
                EmployeesCenterGUI empGui = new EmployeesCenterGUI(plugin, player);
                employeesCenterGUIs.put(player.getUniqueId(), empGui);
                empGui.openEmployeesCenterMenu();
                return true;
            }
            case "open_gui_marketplace": {
                playHubSound(player, "marketplace");
                MarketplaceGUI marketGui = new MarketplaceGUI(plugin, player);
                marketplaceGUIs.put(player.getUniqueId(), marketGui);
                marketGui.openMarketplaceMenu();
                return true;
            }
            case "open_gui_achievements": {
                playHubSound(player, "achievements");
                AchievementGUI achGui = new AchievementGUI(plugin, player);
                achievementGUIs.put(player.getUniqueId(), achGui);
                achGui.openAchievementMenu();
                return true;
            }
            case "open_gui_research_center": {
                playHubSound(player, "research_center");
                ResearchGUI resGui = new ResearchGUI(plugin, player);
                researchGUIs.put(player.getUniqueId(), resGui);
                resGui.openResearchMenu();
                return true;
            }
            case "open_gui_daily_quests": {
                playHubSound(player, "daily_quests");
                DailyQuestGUI questGui = new DailyQuestGUI(plugin, player);
                dailyQuestGUIs.put(player.getUniqueId(), questGui);
                questGui.openDailyQuestMenu();
                return true;
            }
            case "open_gui_help_info": {
                playHubSound(player, "help_info");
                HelpInfoGUI helpGui = new HelpInfoGUI(plugin, player);
                helpInfoGUIs.put(player.getUniqueId(), helpGui);
                helpGui.openHelpMenu();
                return true;
            }
            case "open_gui_recipes_wiki": {
                playHubSound(player, "recipes_wiki");
                RecipesWikiGUI wikiGui = new RecipesWikiGUI(plugin, player);
                recipesWikiGUIs.put(player.getUniqueId(), wikiGui);
                wikiGui.openWikiMenu();
                return true;
            }
            case "open_gui_distribution_center": {
                playHubSound(player, "distribution_center");
                new DistributionCenterGUI(plugin, player).openDistributionCenter();
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * Delegates sound playback to
     * {@link com.aithor.factorycore.gui.HubGUI#playClickSound(String)}.
     * HubGUI reads all sound settings from {@code custom_gui/main_menu.yml}.
     */
    private void playHubSound(Player player, String itemKey) {
        new com.aithor.factorycore.gui.HubGUI(plugin, player).playClickSound(itemKey);
    }

    // ==================== FACTORY BROWSE ====================
    private void handleFactoryBrowseClick(Player player, ItemStack clicked, ItemMeta meta, String name,
            ClickType clickType) {
        FactoryBrowseGUI gui = browseGUIs.getOrDefault(player.getUniqueId(), new FactoryBrowseGUI(plugin, player));

        // Check for factory ID (purchase)
        String factoryId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "browse_factory_id"), PersistentDataType.STRING);
        if (factoryId != null) {
            gui.openPurchaseConfirmation(factoryId);
            return;
        }

        // Navigation
        Integer page = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "browse_page"), PersistentDataType.INTEGER);
        if (page != null) {
            gui.openBrowseMenu(page);
            return;
        }

        // Filter
        if (name.contains("Filter")) {
            if (clickType == ClickType.RIGHT) {
                gui.setFilterType(null);
            } else {
                gui.cycleFilter();
            }
            gui.openBrowseMenu(0);
            return;
        }

        // Sort
        if (name.contains("Sort")) {
            gui.cycleSort();
            gui.openBrowseMenu(0);
            return;
        }

        // Create Factory button
        if (name.contains("Create Factory")) {
            FactoryTypeSelectGUI typeGui = new FactoryTypeSelectGUI(plugin, player);
            typeGui.open();
            return;
        }

        // Back to hub
        if (name.contains("Back to Hub")) {
            openHub(player);
        }
    }

    // ==================== MY FACTORIES ====================
    private void handleMyFactoriesClick(Player player, ItemStack clicked, ItemMeta meta, String name,
            ClickType clickType) {
        MyFactoriesGUI gui = myFactoriesGUIs.getOrDefault(player.getUniqueId(), new MyFactoriesGUI(plugin, player));

        // Check for factory ID
        String factoryId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "my_factory_id"), PersistentDataType.STRING);
        if (factoryId != null) {
            // Check if it's a player-created factory
            PlayerFactory playerFactory = plugin.getPlayerFactoryManager() != null
                    ? plugin.getPlayerFactoryManager().getFactory(factoryId)
                    : null;
            Factory factory = plugin.getFactoryManager().getFactory(factoryId);

            if (factory == null && playerFactory == null)
                return;

            if (clickType == ClickType.SHIFT_RIGHT) {
                if (playerFactory != null) {
                    // Sell player factory
                    double sellPrice = plugin.getPlayerFactoryManager().getSellPrice(factoryId);
                    if (plugin.getPlayerFactoryManager().sellFactory(player, factoryId)) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("player-factory-sold")
                                .replace("{price}", String.format("%.2f", sellPrice)));
                        player.closeInventory();
                    }
                } else {
                    gui.openSellConfirmation(factoryId);
                }
            } else if (clickType == ClickType.RIGHT) {
                // Quick teleport
                if (playerFactory != null) {
                    org.bukkit.Location loc = playerFactory.getCenterLocation();
                    if (loc != null) {
                        player.teleport(loc);
                        player.sendMessage("§aSuccessfully teleported to factory!");
                        player.closeInventory();
                    } else {
                        player.sendMessage("§cCould not teleport to factory!");
                    }
                } else if (plugin.getFactoryManager().teleportPlayer(player, factoryId)) {
                    player.sendMessage("§aSuccessfully teleported to factory!");
                    player.closeInventory();
                } else {
                    player.sendMessage("§cCould not teleport to factory!");
                }
            } else {
                // Open factory management GUI
                FactoryGUI factoryGUI = new FactoryGUI(plugin, player, factoryId);
                factoryGUI.openMainMenu();
            }
            return;
        }

        // Navigation
        Integer page = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "myfactory_page"), PersistentDataType.INTEGER);
        if (page != null) {
            gui.openMyFactoriesMenu(page);
            return;
        }

        // Filter
        if (name.contains("Filter")) {
            gui.cycleFilter();
            gui.openMyFactoriesMenu(0);
            return;
        }

        // Back to hub
        if (name.contains("Back to Hub")) {
            openHub(player);
        }
    }

    // ==================== INVOICE CENTER ====================
    private void handleInvoiceCenterClick(Player player, ItemStack clicked, ItemMeta meta, String name) {
        InvoiceCenterGUI gui = invoiceCenterGUIs.getOrDefault(player.getUniqueId(),
                new InvoiceCenterGUI(plugin, player));

        // Check for invoice ID
        String invoiceId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "center_invoice_id"), PersistentDataType.STRING);
        if (invoiceId != null) {
            boolean success = false;

            if (invoiceId.startsWith("TM_")) {
                String factoryId = invoiceId.substring(3);
                if (plugin.getTaxManager() != null) {
                    success = plugin.getTaxManager().payTax(player, factoryId);
                }
            } else {
                success = plugin.getInvoiceManager().payInvoice(player, invoiceId);
            }

            if (success) {
                player.sendMessage(plugin.getLanguageManager().getMessage("invoice-paid"));
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage("insufficient-funds"));
            }
            gui.openInvoiceCenterMenu();
            return;
        }

        // Navigation
        Integer page = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "invoice_center_page"), PersistentDataType.INTEGER);
        if (page != null) {
            gui.openInvoiceCenterMenu(page);
            return;
        }

        // Filter
        if (name.contains("Filter")) {
            gui.cycleFilter();
            gui.openInvoiceCenterMenu(0);
            return;
        }

        // Sort
        if (name.contains("Sort")) {
            gui.cycleSort();
            gui.openInvoiceCenterMenu(0);
            return;
        }

        // Pay All
        if (name.contains("Pay All")) {
            double invoiceDue = plugin.getInvoiceManager().getInvoicesByOwner(player.getUniqueId())
                    .stream().mapToDouble(Invoice::getAmount).sum();
            double taxDue;

            if (plugin.getTaxManager() != null) {
                taxDue = plugin.getTaxManager().getPlayerTaxRecords(player.getUniqueId()).stream()
                        .mapToDouble(record -> record.amountDue).sum();
            } else {
                taxDue = 0.0;
            }

            double totalDue = invoiceDue + taxDue;

            if (totalDue > 0 && plugin.getEconomy().has(player, totalDue)) {
                for (Invoice invoice : plugin.getInvoiceManager().getInvoicesByOwner(player.getUniqueId())) {
                    plugin.getInvoiceManager().payInvoice(player, invoice.getId());
                }
                if (plugin.getTaxManager() != null) {
                    for (TaxManager.TaxRecord record : plugin.getTaxManager()
                            .getPlayerTaxRecords(player.getUniqueId())) {
                        if (record.amountDue > 0) {
                            plugin.getTaxManager().payTax(player, record.factoryId);
                        }
                    }
                }
                player.sendMessage("§aPaid all invoices! Total: §6$" + String.format("%.2f", totalDue));
            } else if (totalDue > 0) {
                player.sendMessage(plugin.getLanguageManager().getMessage("insufficient-funds"));
            }
            gui.openInvoiceCenterMenu();
            return;
        }

        // Back to hub
        if (name.contains("Back to Hub")) {
            openHub(player);
        }
    }

    // ==================== TAX CENTER ====================
    private void handleTaxCenterClick(Player player, ItemStack clicked, ItemMeta meta, String name) {
        TaxCenterGUI gui = taxCenterGUIs.getOrDefault(player.getUniqueId(), new TaxCenterGUI(plugin, player));
        TaxManager taxManager = plugin.getTaxManager();

        // Check for factory tax payment
        String factoryId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "tax_factory_id"), PersistentDataType.STRING);
        if (factoryId != null && taxManager != null) {
            if (taxManager.payTax(player, factoryId)) {
                player.sendMessage("§aTax paid successfully!");
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage("insufficient-funds"));
            }
            gui.openTaxCenterMenu();
            return;
        }

        // Navigation
        Integer page = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "tax_center_page"), PersistentDataType.INTEGER);
        if (page != null) {
            gui.openTaxCenterMenu(page);
            return;
        }

        // Pay All Taxes
        if (name.contains("Pay All Taxes") && taxManager != null) {
            if (taxManager.payAllTaxes(player)) {
                player.sendMessage("§aAll taxes paid successfully!");
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage("insufficient-funds"));
            }
            gui.openTaxCenterMenu();
            return;
        }

        // Back to hub
        if (name.contains("Back to Hub")) {
            openHub(player);
        }
    }

    // ==================== EMPLOYEES CENTER ====================
    private void handleEmployeesCenterClick(Player player, ItemStack clicked, ItemMeta meta, String name,
            ClickType clickType) {
        EmployeesCenterGUI gui = employeesCenterGUIs.getOrDefault(player.getUniqueId(),
                new EmployeesCenterGUI(plugin, player));

        // Employee Shop button
        if (name.contains("Employee Shop")) {
            EmployeeShopGUI shopGui = new EmployeeShopGUI(plugin, player);
            employeeShopGUIs.put(player.getUniqueId(), shopGui);
            shopGui.openShop();
            return;
        }

        // My Employees button
        if (name.contains("My Employees")) {
            EmployeeShopGUI shopGui = employeeShopGUIs.getOrDefault(player.getUniqueId(),
                    new EmployeeShopGUI(plugin, player));
            employeeShopGUIs.put(player.getUniqueId(), shopGui);
            shopGui.openMyEmployees();
            return;
        }

        // Check for factory/NPC
        String factoryId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "employee_factory_id"), PersistentDataType.STRING);
        String npcId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "employee_npc_id"), PersistentDataType.STRING);
        Integer isPurchasedFlag = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "employee_is_purchased"), PersistentDataType.INTEGER);
        boolean isPurchased = isPurchasedFlag != null && isPurchasedFlag == 1;

        if (factoryId != null) {
            if (npcId != null) {
                // Has NPC
                if (isPurchased) {
                    if (clickType == ClickType.SHIFT_RIGHT) {
                        // Dismiss (permanent remove)
                        gui.openDismissConfirmation(npcId);
                    } else if (clickType == ClickType.RIGHT) {
                        // Unassign from factory
                        gui.openUnassignConfirmation(npcId);
                    } else {
                        // Teleport to NPC
                        FactoryNPC npc = plugin.getNPCManager().getNPC(npcId);
                        if (npc != null && npc.getLocation() != null) {
                            player.teleport(npc.getLocation());
                            player.sendMessage("§aTeleported to employee!");
                            player.closeInventory();
                        }
                    }
                } else {
                    // Admin-spawned NPC
                    if (clickType == ClickType.RIGHT) {
                        // Fire employee
                        gui.openFireConfirmation(factoryId, npcId);
                    } else {
                        // Teleport to NPC
                        FactoryNPC npc = plugin.getNPCManager().getNPC(npcId);
                        if (npc != null && npc.getLocation() != null) {
                            player.teleport(npc.getLocation());
                            player.sendMessage("§aTeleported to employee!");
                            player.closeInventory();
                        }
                    }
                }
            } else {
                // No NPC - open Employee Shop
                EmployeeShopGUI shopGui = new EmployeeShopGUI(plugin, player);
                employeeShopGUIs.put(player.getUniqueId(), shopGui);
                shopGui.openShop();
            }
            return;
        }

        // Navigation
        Integer page = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "employee_center_page"), PersistentDataType.INTEGER);
        if (page != null) {
            gui.openEmployeesCenterMenu(page);
            return;
        }

        // Back to hub
        if (name.contains("Back to Hub")) {
            openHub(player);
        }
    }

    // ==================== EMPLOYEE SHOP ====================
    private void handleEmployeeShopClick(Player player, ItemStack clicked, ItemMeta meta, String name,
            ClickType clickType) {
        EmployeeShopGUI shopGui = employeeShopGUIs.getOrDefault(player.getUniqueId(),
                new EmployeeShopGUI(plugin, player));
        employeeShopGUIs.put(player.getUniqueId(), shopGui);

        // NPC type to purchase (from shop listing)
        String npcTypeId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "shop_npc_type_id"), PersistentDataType.STRING);
        if (npcTypeId != null) {
            shopGui.openPurchaseConfirmation(npcTypeId);
            return;
        }

        // Owned NPC to assign
        String ownedNpcId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "owned_npc_id"), PersistentDataType.STRING);
        if (ownedNpcId != null) {
            shopGui.openFactoryAssign(ownedNpcId);
            return;
        }

        // Factory assignment click
        String assignFactoryId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "assign_factory_id"), PersistentDataType.STRING);
        String assignNpcId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "assign_npc_id"), PersistentDataType.STRING);
        if (assignFactoryId != null && assignNpcId != null) {
            if (plugin.getNPCManager().assignNPCToFactory(player, assignNpcId, assignFactoryId)) {
                player.sendMessage("§aEmployee successfully assigned to factory §e" + assignFactoryId + "§a!");
            }
            // Return to employees center
            EmployeesCenterGUI empGui = employeesCenterGUIs.getOrDefault(player.getUniqueId(),
                    new EmployeesCenterGUI(plugin, player));
            employeesCenterGUIs.put(player.getUniqueId(), empGui);
            empGui.openEmployeesCenterMenu();
            return;
        }

        // My Employees button (from shop)
        if (name.contains("My Employees")) {
            shopGui.openMyEmployees();
            return;
        }

        // Employee Shop button (from sub-menus)
        if (name.contains("Employee Shop")) {
            shopGui.openShop();
            return;
        }

        // Back buttons
        if (name.contains("Back to Shop")) {
            shopGui.openShop();
            return;
        }
        if (name.contains("Back")) {
            EmployeesCenterGUI empGui = employeesCenterGUIs.getOrDefault(player.getUniqueId(),
                    new EmployeesCenterGUI(plugin, player));
            employeesCenterGUIs.put(player.getUniqueId(), empGui);
            empGui.openEmployeesCenterMenu();
        }
    }

    // ==================== HELP & INFO ====================
    private void handleHelpInfoClick(Player player, ItemStack clicked, ItemMeta meta, String name) {
        HelpInfoGUI gui = helpInfoGUIs.getOrDefault(player.getUniqueId(), new HelpInfoGUI(plugin, player));

        // Category navigation
        if (name.contains("Overview")) {
            gui.openHelpMenu("overview");
        } else if (name.contains("Factories") && !name.contains("My")) {
            gui.openHelpMenu("factories");
        } else if (name.contains("Production")) {
            gui.openHelpMenu("production");
        } else if (name.contains("Economy")) {
            gui.openHelpMenu("economy");
        } else if (name.contains("Commands")) {
            gui.openHelpMenu("commands");
        } else if (name.contains("Tips")) {
            gui.openHelpMenu("tips");
        } else if (name.contains("Back to Hub")) {
            openHub(player);
        }
    }

    // ==================== MARKETPLACE ====================
    private void handleMarketplaceClick(Player player, ItemStack clicked, ItemMeta meta, String name,
            ClickType clickType) {
        MarketplaceGUI gui = marketplaceGUIs.getOrDefault(player.getUniqueId(), new MarketplaceGUI(plugin, player));
        MarketplaceManager marketplace = plugin.getMarketplaceManager();

        // View navigation
        String view = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "market_view"), PersistentDataType.STRING);
        if (view != null) {
            gui.setView(view);
            gui.openMarketplaceMenu();
            return;
        }

        // Listing click
        String listingId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "market_listing_id"), PersistentDataType.STRING);
        Integer isOwn = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "is_own_listing"), PersistentDataType.INTEGER);

        if (listingId != null) {
            if (isOwn != null && isOwn == 1) {
                // Cancel own listing
                if (marketplace.cancelListing(player, listingId)) {
                    player.sendMessage("§aListing cancelled! Items returned.");
                }
                gui.openMarketplaceMenu();
            } else {
                // Purchase
                MarketplaceManager.MarketListing listing = marketplace.getListing(listingId);
                if (listing != null) {
                    int amount = clickType == ClickType.RIGHT ? listing.amount : 1;
                    gui.openPurchaseConfirmation(listingId, amount);
                }
            }
            return;
        }

        // Sell resource button
        String sellResourceId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "sell_resource_id"), PersistentDataType.STRING);
        if (sellResourceId != null) {
            // Check if player has this resource
            // For now, open sell dialog with amount 1
            gui.openSellConfirmation(sellResourceId, 1);
            return;
        }

        // Collect earnings
        if (name.contains("Collect Earnings")) {
            double earnings = marketplace.collectEarnings(player);
            if (earnings > 0) {
                player.sendMessage("§aCollected §6$" + String.format("%.2f", earnings) + " §ain earnings!");
            }
            gui.openMarketplaceMenu();
            return;
        }

        // Filter
        if (name.contains("Filter")) {
            gui.cycleFilter();
            gui.openMarketplaceMenu();
            return;
        }

        // Navigation
        Integer page = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "marketplace_page"), PersistentDataType.INTEGER);
        if (page != null) {
            gui.openMarketplaceMenu(page);
            return;
        }

        // Back to hub
        if (name.contains("Back to Hub")) {
            openHub(player);
        }
    }

    // ==================== RESEARCH CENTER ====================
    private void handleResearchCenterClick(Player player, ItemStack clicked, ItemMeta meta, String name) {
        // Check for research ID on clicked item
        String researchId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "research_id"), PersistentDataType.STRING);
        if (researchId != null) {
            ResearchGUI gui = researchGUIs.getOrDefault(player.getUniqueId(), new ResearchGUI(plugin, player));
            researchGUIs.put(player.getUniqueId(), gui);
            gui.openResearchDetail(researchId);
            return;
        }

        // Back to hub
        if (name.contains("Back to Hub")) {
            openHub(player);
            return;
        }

        // Close
        if (name.contains("Close")) {
            player.closeInventory();
        }
    }

    // ==================== RESEARCH DETAIL ====================
    private void handleResearchDetailClick(Player player, ItemStack clicked, ItemMeta meta, String name) {
        // Confirm research start
        String confirmResearchId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "confirm_research_id"), PersistentDataType.STRING);
        if (confirmResearchId != null && name.contains("Start Research")) {
            if (plugin.getResearchManager().startResearch(player, confirmResearchId)) {
                // Reopen the detail view to show progress
                ResearchGUI gui = researchGUIs.getOrDefault(player.getUniqueId(), new ResearchGUI(plugin, player));
                researchGUIs.put(player.getUniqueId(), gui);
                gui.openResearchDetail(confirmResearchId);
            }
            return;
        }

        // Back to research center
        if (name.contains("Back to Research Center")) {
            ResearchGUI gui = researchGUIs.getOrDefault(player.getUniqueId(), new ResearchGUI(plugin, player));
            researchGUIs.put(player.getUniqueId(), gui);
            gui.openResearchMenu();
            return;
        }
    }

    // ==================== ACHIEVEMENTS ====================
    private void handleAchievementsClick(Player player, ItemStack clicked, ItemMeta meta, String name) {
        AchievementGUI gui = achievementGUIs.getOrDefault(player.getUniqueId(), new AchievementGUI(plugin, player));
        achievementGUIs.put(player.getUniqueId(), gui);

        if (name.contains("Previous Page")) {
            gui.openAchievementMenu(gui.getCurrentPage() - 1);
            return;
        }

        if (name.contains("Next Page")) {
            gui.openAchievementMenu(gui.getCurrentPage() + 1);
            return;
        }

        if (name.contains("Back to Hub")) {
            openHub(player);
        }
    }

    // ==================== DAILY QUESTS ====================
    private void handleDailyQuestClick(Player player, ItemStack clicked, ItemMeta meta, String name) {
        DailyQuestManager questManager = plugin.getDailyQuestManager();
        if (questManager == null)
            return;

        // Check for quest ID on clicked item (claim individual quest reward)
        String questId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "daily_quest_id"), PersistentDataType.STRING);
        if (questId != null) {
            if (questManager.isQuestCompleted(player.getUniqueId(), questId)
                    && !questManager.isRewardClaimed(player.getUniqueId(), questId)) {
                if (questManager.claimReward(player, questId)) {
                    int exp = questManager.getQuestRewardExp(questId);
                    double money = questManager.getQuestRewardMoney(questId);
                    player.sendMessage("§aReward claimed! §e+" + exp + " EXP §7& §6$" + String.format("%.2f", money));
                }
            }
            // Refresh the GUI
            DailyQuestGUI gui = dailyQuestGUIs.getOrDefault(player.getUniqueId(),
                    new DailyQuestGUI(plugin, player));
            dailyQuestGUIs.put(player.getUniqueId(), gui);
            gui.openDailyQuestMenu();
            return;
        }

        // All-Complete Bonus claim
        if (name.contains("All-Complete Bonus") && name.contains("Click")) {
            if (questManager.claimBonus(player)) {
                // Notification is handled inside claimBonus()
            }
            DailyQuestGUI gui = dailyQuestGUIs.getOrDefault(player.getUniqueId(),
                    new DailyQuestGUI(plugin, player));
            dailyQuestGUIs.put(player.getUniqueId(), gui);
            gui.openDailyQuestMenu();
            return;
        }

        // Claim All Rewards button
        if (name.contains("Claim All Rewards")) {
            int claimed = 0;
            for (String qId : questManager.getQuestIds()) {
                if (questManager.isQuestCompleted(player.getUniqueId(), qId)
                        && !questManager.isRewardClaimed(player.getUniqueId(), qId)) {
                    if (questManager.claimReward(player, qId)) {
                        claimed++;
                    }
                }
            }
            if (claimed > 0) {
                player.sendMessage("§aClaimed rewards for §e" + claimed + " §aquest(s)!");
            }
            DailyQuestGUI gui = dailyQuestGUIs.getOrDefault(player.getUniqueId(),
                    new DailyQuestGUI(plugin, player));
            dailyQuestGUIs.put(player.getUniqueId(), gui);
            gui.openDailyQuestMenu();
            return;
        }

        // Back to hub
        if (name.contains("Back to Hub")) {
            openHub(player);
        }
    }

    // ==================== RECIPES WIKI ====================
    private void handleRecipesWikiClick(Player player, ItemStack clicked, ItemMeta meta, String name,
            ClickType clickType) {
        RecipesWikiGUI gui = recipesWikiGUIs.getOrDefault(player.getUniqueId(),
                new RecipesWikiGUI(plugin, player));
        recipesWikiGUIs.put(player.getUniqueId(), gui);

        // Check for wiki_action PDC tag
        String action = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "wiki_action"),
                PersistentDataType.STRING);

        if (action != null) {
            switch (action) {
                case "sort":
                    gui.cycleSortMode();
                    gui.openWikiMenu(gui.getCurrentPage());
                    return;
                case "prev_page":
                    gui.openWikiMenu(gui.getCurrentPage() - 1);
                    return;
                case "next_page":
                    gui.openWikiMenu(gui.getCurrentPage() + 1);
                    return;
                case "back_to_hub":
                    openHub(player);
                    return;
                case "back_to_wiki":
                    gui.openWikiMenu(gui.getCurrentPage());
                    return;
                case "close":
                    player.closeInventory();
                    return;
                default:
                    // Handle prefixed actions: "recipe_prev:{resourceId}" / "recipe_next:{resourceId}"
                    if (action.startsWith("recipe_prev:")) {
                        String resId = action.substring("recipe_prev:".length());
                        gui.openRecipeDetail(resId, gui.getCurrentRecipePage() - 1);
                        return;
                    }
                    if (action.startsWith("recipe_next:")) {
                        String resId = action.substring("recipe_next:".length());
                        gui.openRecipeDetail(resId, gui.getCurrentRecipePage() + 1);
                        return;
                    }
                    break;
            }
        }

        // Check for resource click (left = recipe, right = usage)
        String resourceId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "wiki_resource_id"),
                PersistentDataType.STRING);

        if (resourceId != null) {
            if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
                gui.openUsageDetail(resourceId);
            } else {
                gui.openRecipeDetail(resourceId);
            }
        }
    }

    // ==================== DISTRIBUTION CENTER ====================
    private void handleDistributionCenterClick(Player player, ItemStack clicked, ItemMeta meta, String name) {
        // Bossbar Toggle
        Integer bbToggle = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "dist_toggle_bossbar"),
                PersistentDataType.INTEGER);
        
        if (bbToggle != null) {
            com.aithor.factorycore.managers.DistributionManager distManager = plugin.getDistributionManager();
            boolean isEnabled = bbToggle == 1;
            boolean newState = !isEnabled;
            distManager.setBossbarEnabled(player.getUniqueId(), newState);
            player.sendMessage("§aTimer reminder " + (newState ? "enabled" : "disabled") + "!");
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            } catch (Exception ignored) {}
            new DistributionCenterGUI(plugin, player).openDistributionCenter();
            return;
        }

        // Distribution Offers Toggle
        Integer distOffersToggle = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "dist_toggle_offers"),
                PersistentDataType.INTEGER);
        
        if (distOffersToggle != null) {
            com.aithor.factorycore.managers.DistributionManager distManager = plugin.getDistributionManager();
            boolean isEnabled = distOffersToggle == 1;
            boolean newState = !isEnabled;
            distManager.setDistributionEnabled(player.getUniqueId(), newState);
            player.sendMessage("§aDistribution offers " + (newState ? "enabled" : "disabled") + "!");
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            } catch (Exception ignored) {}
            new DistributionCenterGUI(plugin, player).openDistributionCenter();
            return;
        }

        // Back to Hub
        if (name.contains("Back to Hub")) {
            openHub(player);
            return;
        }

        // Back to Distribution Center (from detail view)
        if (name.contains("Back to Distribution Center")) {
            new DistributionCenterGUI(plugin, player).openDistributionCenter();
            return;
        }

        // Click on a request item (open detail view)
        String requestId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "dist_request_id"),
                PersistentDataType.STRING);

        if (requestId != null) {
            com.aithor.factorycore.managers.DistributionManager distManager = plugin.getDistributionManager();
            java.util.List<com.aithor.factorycore.models.ActiveDistributionRequest> requests =
                    distManager.getActiveRequests(player.getUniqueId());

            for (com.aithor.factorycore.models.ActiveDistributionRequest request : requests) {
                if (request.getRequestId().equals(requestId)) {
                    new DistributionCenterGUI(plugin, player).openRequestDetail(request);
                    return;
                }
            }
            player.sendMessage("\u00a7cThis request is no longer active.");
            new DistributionCenterGUI(plugin, player).openDistributionCenter();
            return;
        }

        // Deliver a specific resource
        String demandResource = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "dist_demand_resource"),
                PersistentDataType.STRING);
        String demandRequest = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "dist_demand_request"),
                PersistentDataType.STRING);

        if (demandResource != null && demandRequest != null) {
            com.aithor.factorycore.managers.DistributionManager distManager = plugin.getDistributionManager();
            boolean success = distManager.deliverResource(player, demandRequest, demandResource);

            if (success) {
                player.sendMessage("\u00a7a\u2714 Resource delivered successfully!");
                try {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                } catch (Exception ignored) {}

                // Refresh the detail view
                java.util.List<com.aithor.factorycore.models.ActiveDistributionRequest> requests =
                        distManager.getActiveRequests(player.getUniqueId());
                for (com.aithor.factorycore.models.ActiveDistributionRequest request : requests) {
                    if (request.getRequestId().equals(demandRequest)) {
                        new DistributionCenterGUI(plugin, player).openRequestDetail(request);
                        return;
                    }
                }
                // If request was completed and removed, go back to main
                new DistributionCenterGUI(plugin, player).openDistributionCenter();
            } else {
                player.sendMessage("\u00a7c\u2716 You don't have this resource in your inventory!");
                try {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                } catch (Exception ignored) {}
            }
            return;
        }

        // Deliver All button
        String deliverAllId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "dist_deliver_all"),
                PersistentDataType.STRING);

        if (deliverAllId != null) {
            com.aithor.factorycore.managers.DistributionManager distManager = plugin.getDistributionManager();
            java.util.List<com.aithor.factorycore.models.ActiveDistributionRequest> requests =
                    distManager.getActiveRequests(player.getUniqueId());

            for (com.aithor.factorycore.models.ActiveDistributionRequest request : requests) {
                if (request.getRequestId().equals(deliverAllId)) {
                    int delivered = 0;
                    for (String resourceId : new java.util.ArrayList<>(request.getDeliveredResources().keySet())) {
                        if (!request.isResourceDelivered(resourceId)) {
                            if (distManager.deliverResource(player, deliverAllId, resourceId)) {
                                delivered++;
                            }
                        }
                    }

                    if (delivered > 0) {
                        player.sendMessage("\u00a7a\u2714 Delivered " + delivered + " resource(s)!");
                        try {
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                        } catch (Exception ignored) {}
                    } else {
                        player.sendMessage("\u00a7cNo resources could be delivered. Check your inventory!");
                    }

                    // Refresh view
                    requests = distManager.getActiveRequests(player.getUniqueId());
                    for (com.aithor.factorycore.models.ActiveDistributionRequest req : requests) {
                        if (req.getRequestId().equals(deliverAllId)) {
                            new DistributionCenterGUI(plugin, player).openRequestDetail(req);
                            return;
                        }
                    }
                    new DistributionCenterGUI(plugin, player).openDistributionCenter();
                    return;
                }
            }
            player.sendMessage("\u00a7cThis request is no longer active.");
            new DistributionCenterGUI(plugin, player).openDistributionCenter();
            return;
        }
    }

    // ==================== FACTORY TYPE SELECT (Create Factory)
    // ====================
    private void handleFactoryTypeSelectClick(Player player, ItemStack clicked, ItemMeta meta, String name) {
        // Check for factory type selection
        String typeStr = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "create_factory_type"), PersistentDataType.STRING);
        if (typeStr != null) {
            try {
                FactoryType type = FactoryType.valueOf(typeStr);
                FactoryCreateConfirmGUI confirmGui = new FactoryCreateConfirmGUI(plugin, player);
                confirmGui.open(type);
            } catch (IllegalArgumentException e) {
                player.sendMessage("§cInvalid factory type!");
            }
            return;
        }

        // Back button
        if (name.contains("Back")) {
            FactoryBrowseGUI gui = browseGUIs.getOrDefault(player.getUniqueId(),
                    new FactoryBrowseGUI(plugin, player));
            browseGUIs.put(player.getUniqueId(), gui);
            gui.openBrowseMenu();
        }
    }

    // ==================== FACTORY CREATE CONFIRM ====================
    private void handleFactoryCreateConfirmClick(Player player, ItemStack clicked, ItemMeta meta, String name) {
        // Confirm creation
        String typeStr = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "confirm_create_factory_type"), PersistentDataType.STRING);
        if (typeStr != null && name.contains("Confirm")) {
            try {
                FactoryType type = FactoryType.valueOf(typeStr);
                PlayerFactory pf = plugin.getPlayerFactoryManager().createFactory(player, type);
                if (pf != null) {
                    // Success message
                    player.sendMessage(plugin.getLanguageManager().getMessage("player-factory-created")
                            .replace("{type}", type.getDisplayName()));

                    // Sound effect
                    if (plugin.getConfig().getBoolean("notifications.sound.enabled")) {
                        try {
                            player.playSound(player.getLocation(),
                                    plugin.getConfig().getString("notifications.sound.factory-created",
                                            "ENTITY_PLAYER_LEVELUP"),
                                    1.0f, 1.0f);
                        } catch (Exception ignored) {
                        }
                    }

                    // Title
                    if (plugin.getConfig().getBoolean("notifications.titles.enabled")) {
                        player.sendTitle(
                                plugin.getLanguageManager().getMessage("titles.player-factory-created.title"),
                                plugin.getLanguageManager().getMessage("titles.player-factory-created.subtitle")
                                        .replace("{type}", type.getDisplayName()),
                                plugin.getConfig().getInt("notifications.titles.fade-in", 10),
                                plugin.getConfig().getInt("notifications.titles.stay", 40),
                                plugin.getConfig().getInt("notifications.titles.fade-out", 10));
                    }

                    player.closeInventory();
                }
                // If createFactory returned null, error message was already sent by the manager
            } catch (IllegalArgumentException e) {
                player.sendMessage("§cInvalid factory type!");
            }
            return;
        }

        // Cancel button
        if (name.contains("Cancel")) {
            FactoryTypeSelectGUI typeGui = new FactoryTypeSelectGUI(plugin, player);
            typeGui.open();
        }
    }

    // ==================== CONFIRMATION DIALOGS ====================
    private void handleConfirmationClick(Player player, ItemStack clicked, ItemMeta meta, String name, String title) {
        // Purchase confirmation
        String purchaseFactoryId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "confirm_purchase_factory"), PersistentDataType.STRING);
        if (purchaseFactoryId != null && name.contains("Confirm")) {
            if (plugin.getFactoryManager().buyFactory(player, purchaseFactoryId)) {
                Factory factory = plugin.getFactoryManager().getFactory(purchaseFactoryId);
                player.sendMessage(plugin.getLanguageManager().getMessage("factory-bought")
                        .replace("{factory}", factory.getType().getDisplayName())
                        .replace("{price}", String.valueOf(factory.getPrice())));
                player.closeInventory();
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage("insufficient-funds"));
            }
            return;
        }

        // Sell confirmation (admin factory)
        String sellFactoryId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "confirm_sell_factory"), PersistentDataType.STRING);
        if (sellFactoryId != null && name.contains("Confirm")) {
            Factory factory = plugin.getFactoryManager().getFactory(sellFactoryId);
            double sellPrice = factory.getPrice() * plugin.getConfig().getDouble("factory.sell-price-multiplier", 0.5);
            if (plugin.getFactoryManager().sellFactory(player, sellFactoryId)) {
                player.sendMessage(plugin.getLanguageManager().getMessage("factory-sold")
                        .replace("{factory}", factory.getType().getDisplayName())
                        .replace("{price}", String.valueOf(sellPrice)));
                player.closeInventory();
            }
            return;
        }

        // Sell confirmation (player factory)
        String sellPlayerFactoryId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "confirm_sell_player_factory"), PersistentDataType.STRING);
        if (sellPlayerFactoryId != null && name.contains("Confirm") && plugin.getPlayerFactoryManager() != null) {
            double sellPrice = plugin.getPlayerFactoryManager().getSellPrice(sellPlayerFactoryId);
            if (plugin.getPlayerFactoryManager().sellFactory(player, sellPlayerFactoryId)) {
                player.sendMessage(plugin.getLanguageManager().getMessage("player-factory-sold")
                        .replace("{price}", String.format("%.2f", sellPrice)));
                player.closeInventory();
            }
            return;
        }

        // Fire NPC confirmation (admin-spawned)
        String fireNpcId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "confirm_fire_npc"), PersistentDataType.STRING);
        if (fireNpcId != null && name.contains("Confirm")) {
            if (plugin.getNPCManager().removeNPC(fireNpcId)) {
                player.sendMessage("§aEmployee fired successfully!");
            }
            EmployeesCenterGUI gui = new EmployeesCenterGUI(plugin, player);
            employeesCenterGUIs.put(player.getUniqueId(), gui);
            gui.openEmployeesCenterMenu();
            return;
        }

        // Unassign NPC confirmation (purchased NPC - keeps record)
        String unassignNpcId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "confirm_unassign_npc"), PersistentDataType.STRING);
        if (unassignNpcId != null && name.contains("Confirm")) {
            if (plugin.getNPCManager().unassignNPC(player, unassignNpcId)) {
                player.sendMessage("§aEmployee unassigned successfully! They are now in your employee pool.");
            }
            EmployeesCenterGUI gui = new EmployeesCenterGUI(plugin, player);
            employeesCenterGUIs.put(player.getUniqueId(), gui);
            gui.openEmployeesCenterMenu();
            return;
        }

        // Dismiss NPC confirmation (purchased NPC - permanent removal)
        String dismissNpcId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "confirm_dismiss_npc"), PersistentDataType.STRING);
        if (dismissNpcId != null && name.contains("Confirm")) {
            if (plugin.getNPCManager().dismissNPC(player, dismissNpcId)) {
                player.sendMessage("§aEmployee dismissed permanently.");
            }
            EmployeesCenterGUI gui = new EmployeesCenterGUI(plugin, player);
            employeesCenterGUIs.put(player.getUniqueId(), gui);
            gui.openEmployeesCenterMenu();
            return;
        }

        // Purchase NPC from shop confirmation
        String shopNpcType = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "shop_confirm_npc_type"), PersistentDataType.STRING);
        if (shopNpcType != null && name.contains("Confirm")) {
            String newNpcId = plugin.getNPCManager().purchaseNPC(player, shopNpcType);
            if (newNpcId != null) {
                player.sendMessage(
                        "§aEmployee purchased successfully! Assign them to a factory from §bMy Employees§a.");
            }
            EmployeeShopGUI shopGui = employeeShopGUIs.getOrDefault(player.getUniqueId(),
                    new EmployeeShopGUI(plugin, player));
            employeeShopGUIs.put(player.getUniqueId(), shopGui);
            shopGui.openShop();
            return;
        }

        // Marketplace purchase confirmation
        String purchaseListingId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "confirm_purchase_listing"), PersistentDataType.STRING);
        Integer purchaseAmount = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "purchase_amount"), PersistentDataType.INTEGER);
        if (purchaseListingId != null && purchaseAmount != null && name.contains("Confirm")) {
            if (plugin.getMarketplaceManager().purchaseListing(player, purchaseListingId, purchaseAmount)) {
                player.sendMessage("§aPurchase successful!");
            } else {
                player.sendMessage("§cPurchase failed!");
            }
            MarketplaceGUI gui = new MarketplaceGUI(plugin, player);
            gui.openMarketplaceMenu();
            return;
        }

        // Create listing confirmation
        String listingResource = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "listing_resource"), PersistentDataType.STRING);
        Integer listingAmount = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "listing_amount"), PersistentDataType.INTEGER);
        Double listingPrice = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "listing_price"), PersistentDataType.DOUBLE);
        if (listingResource != null && listingAmount != null && listingPrice != null) {
            String result = plugin.getMarketplaceManager().createListing(player, listingResource, listingAmount,
                    listingPrice);
            if (result != null) {
                player.sendMessage("§aListing created successfully!");
            } else {
                player.sendMessage("§cFailed to create listing!");
            }
            MarketplaceGUI gui = new MarketplaceGUI(plugin, player);
            gui.openMarketplaceMenu();
            return;
        }

        // Cancel button - return to appropriate menu
        if (name.contains("Cancel")) {
            if (title.contains("Purchase") && title.contains("Factory")) {
                FactoryBrowseGUI gui = new FactoryBrowseGUI(plugin, player);
                gui.openBrowseMenu();
            } else if (title.contains("Sale")) {
                MyFactoriesGUI gui = new MyFactoriesGUI(plugin, player);
                gui.openMyFactoriesMenu();
            } else if (title.contains("Fire") || title.contains("Unassign") || title.contains("Dismiss")) {
                EmployeesCenterGUI gui = employeesCenterGUIs.getOrDefault(player.getUniqueId(),
                        new EmployeesCenterGUI(plugin, player));
                employeesCenterGUIs.put(player.getUniqueId(), gui);
                gui.openEmployeesCenterMenu();
            } else if (title.contains("Purchase") && !title.contains("Factory")) {
                // Employee shop purchase cancel
                EmployeeShopGUI shopGui = employeeShopGUIs.getOrDefault(player.getUniqueId(),
                        new EmployeeShopGUI(plugin, player));
                employeeShopGUIs.put(player.getUniqueId(), shopGui);
                shopGui.openShop();
            } else if (title.contains("Marketplace") || title.contains("Listing")) {
                MarketplaceGUI gui = new MarketplaceGUI(plugin, player);
                gui.openMarketplaceMenu();
            } else {
                openHub(player);
            }
        }
    }

    private void openHub(Player player) {
        HubGUI hubGUI = new HubGUI(plugin, player);
        hubGUI.openHubMenu();
    }

    // Clean up player data when they leave
    public void cleanupPlayer(UUID playerId) {
        browseGUIs.remove(playerId);
        myFactoriesGUIs.remove(playerId);
        invoiceCenterGUIs.remove(playerId);
        taxCenterGUIs.remove(playerId);
        employeesCenterGUIs.remove(playerId);
        employeeShopGUIs.remove(playerId);
        helpInfoGUIs.remove(playerId);
        marketplaceGUIs.remove(playerId);
        researchGUIs.remove(playerId);
        achievementGUIs.remove(playerId);
        dailyQuestGUIs.remove(playerId);
        recipesWikiGUIs.remove(playerId);
    }
}
