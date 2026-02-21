package com.aithor.factorycore.listeners;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.gui.*;
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

    public HubClickListener(FactoryCore plugin) {
        this.plugin = plugin;
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

        // Cancel all events to prevent item manipulation
        event.setCancelled(true);

        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (!clicked.hasItemMeta())
            return;

        ItemMeta meta = clicked.getItemMeta();
        String name = meta.getDisplayName();

        // Route to appropriate handler based on GUI title
        if (title.contains("Main Hub")) {
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
        } else if (title.contains("Confirm Purchase") || title.contains("Confirm Sale") ||
                title.contains("Confirm Fire") || title.contains("Confirm Unassign") ||
                title.contains("Confirm Dismiss") || title.contains("Create Listing")) {
            handleConfirmationClick(player, clicked, meta, name, title);
        }
    }

    private boolean isHubGUI(String title) {
        return title.contains("Main Hub") ||
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
                title.contains("Research:");
    }

    // ==================== HUB MAIN MENU ====================
    private void handleHubClick(Player player, ItemStack clicked, ItemMeta meta, String name) {
        if (name.contains("Factory Browse")) {
            FactoryBrowseGUI gui = new FactoryBrowseGUI(plugin, player);
            browseGUIs.put(player.getUniqueId(), gui);
            gui.openBrowseMenu();
        } else if (name.contains("My Factories")) {
            MyFactoriesGUI gui = new MyFactoriesGUI(plugin, player);
            myFactoriesGUIs.put(player.getUniqueId(), gui);
            gui.openMyFactoriesMenu();
        } else if (name.contains("Invoice Center")) {
            InvoiceCenterGUI gui = new InvoiceCenterGUI(plugin, player);
            invoiceCenterGUIs.put(player.getUniqueId(), gui);
            gui.openInvoiceCenterMenu();
        } else if (name.contains("Tax Center")) {
            TaxCenterGUI gui = new TaxCenterGUI(plugin, player);
            taxCenterGUIs.put(player.getUniqueId(), gui);
            gui.openTaxCenterMenu();
        } else if (name.contains("Employees Center")) {
            EmployeesCenterGUI gui = new EmployeesCenterGUI(plugin, player);
            employeesCenterGUIs.put(player.getUniqueId(), gui);
            gui.openEmployeesCenterMenu();
        } else if (name.contains("Marketplace")) {
            MarketplaceGUI gui = new MarketplaceGUI(plugin, player);
            marketplaceGUIs.put(player.getUniqueId(), gui);
            gui.openMarketplaceMenu();
        } else if (name.contains("Research Center")) {
            ResearchGUI gui = new ResearchGUI(plugin, player);
            researchGUIs.put(player.getUniqueId(), gui);
            gui.openResearchMenu();
        } else if (name.contains("Help & Info")) {
            HelpInfoGUI gui = new HelpInfoGUI(plugin, player);
            helpInfoGUIs.put(player.getUniqueId(), gui);
            gui.openHelpMenu();
        } else if (name.contains("Close")) {
            player.closeInventory();
        }
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
            Factory factory = plugin.getFactoryManager().getFactory(factoryId);
            if (factory == null)
                return;

            if (clickType == ClickType.SHIFT_RIGHT) {
                // Sell factory
                gui.openSellConfirmation(factoryId);
            } else if (clickType == ClickType.RIGHT) {
                // Quick teleport
                if (plugin.getFactoryManager().teleportPlayer(player, factoryId)) {
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
            if (plugin.getInvoiceManager().payInvoice(player, invoiceId)) {
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
            double totalDue = plugin.getInvoiceManager().getInvoicesByOwner(player.getUniqueId())
                    .stream().mapToDouble(Invoice::getAmount).sum();
            if (plugin.getEconomy().has(player, totalDue)) {
                for (Invoice invoice : plugin.getInvoiceManager().getInvoicesByOwner(player.getUniqueId())) {
                    plugin.getInvoiceManager().payInvoice(player, invoice.getId());
                }
                player.sendMessage("§aPaid all invoices! Total: §6$" + String.format("%.2f", totalDue));
            } else {
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

        // Sell confirmation
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
    }
}
