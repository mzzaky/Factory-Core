package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

/**
 * Help & Info GUI - Comprehensive plugin information and guide
 */
public class HelpInfoGUI {

    private final FactoryCore plugin;
    private final Player player;
    private String currentCategory = "overview";

    public HelpInfoGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openHelpMenu() {
        openHelpMenu("overview");
    }

    public void openHelpMenu(String category) {
        this.currentCategory = category;
        
        Inventory inv = Bukkit.createInventory(null, 54, "§d§lHelp & Info §8- §e" + getCategoryTitle());

        // Fill borders
        Material borderMat = Material.matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9) inv.setItem(i, border);
        for (int i = 17; i < 45; i += 9) inv.setItem(i, border);

        // Header with plugin info
        inv.setItem(4, createPluginInfoItem());

        // Category navigation (top row)
        inv.setItem(1, createCategoryItem("overview", Material.BOOK, "§6Overview", "General plugin information"));
        inv.setItem(2, createCategoryItem("factories", Material.FURNACE, "§6Factories", "Factory management guide"));
        inv.setItem(3, createCategoryItem("production", Material.CRAFTING_TABLE, "§6Production", "How production works"));
        inv.setItem(5, createCategoryItem("economy", Material.GOLD_INGOT, "§6Economy", "Taxes, invoices, marketplace"));
        inv.setItem(6, createCategoryItem("commands", Material.COMMAND_BLOCK, "§6Commands", "Available commands"));
        inv.setItem(7, createCategoryItem("tips", Material.TORCH, "§6Tips & Tricks", "Pro tips for success"));

        // Display content based on category
        switch (currentCategory) {
            case "overview":
                displayOverview(inv);
                break;
            case "factories":
                displayFactoriesGuide(inv);
                break;
            case "production":
                displayProductionGuide(inv);
                break;
            case "economy":
                displayEconomyGuide(inv);
                break;
            case "commands":
                displayCommandsGuide(inv);
                break;
            case "tips":
                displayTipsGuide(inv);
                break;
        }

        // Back to hub (slot 49)
        inv.setItem(49, createItem(Material.DARK_OAK_DOOR, "§c§lBack to Hub",
                Arrays.asList("§7Return to main menu")));

        player.openInventory(inv);
    }

    private String getCategoryTitle() {
        switch (currentCategory) {
            case "factories": return "Factories";
            case "production": return "Production";
            case "economy": return "Economy";
            case "commands": return "Commands";
            case "tips": return "Tips & Tricks";
            default: return "Overview";
        }
    }

    private void displayOverview(Inventory inv) {
        // Welcome message (slot 20)
        inv.setItem(20, createItem(Material.NETHER_STAR, "§e§lWelcome to FactoryCore!",
                Arrays.asList(
                        "§7FactoryCore is a comprehensive",
                        "§7factory management plugin that",
                        "§7lets you own, operate, and profit",
                        "§7from industrial factories.",
                        "",
                        "§7§nKey Features:§r",
                        "§7• Own and manage factories",
                        "§7• Produce valuable resources",
                        "§7• Trade on the marketplace",
                        "§7• Hire NPC employees",
                        "§7• Upgrade and expand")));

        // Getting Started (slot 21)
        inv.setItem(21, createItem(Material.COMPASS, "§a§lGetting Started",
                Arrays.asList(
                        "§7§nStep 1: Buy a Factory§r",
                        "§7Use §eFactory Browse §7to find",
                        "§7available factories to purchase.",
                        "",
                        "§7§nStep 2: Stock Resources§r",
                        "§7Add raw materials to your",
                        "§7factory storage.",
                        "",
                        "§7§nStep 3: Start Production§r",
                        "§7Select a recipe and start",
                        "§7producing valuable goods!",
                        "",
                        "§7§nStep 4: Collect & Sell§r",
                        "§7Collect outputs and sell them",
                        "§7on the marketplace for profit.")));

        // Quick Stats (slot 22)
        List<Factory> owned = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());
        inv.setItem(22, createItem(Material.PAPER, "§b§lYour Progress",
                Arrays.asList(
                        "§7Your current stats:",
                        "",
                        "§eFactories Owned: §6" + owned.size(),
                        "§eHighest Level: §6" + owned.stream().mapToInt(Factory::getLevel).max().orElse(0),
                        "§eTotal Value: §6$" + String.format("%.2f", owned.stream().mapToDouble(Factory::getPrice).sum()),
                        "",
                        "§7Keep expanding to become",
                        "§7the ultimate factory tycoon!")));

        // Factory Types Preview (slot 23)
        inv.setItem(23, createItem(Material.IRON_BLOCK, "§6§lFactory Types",
                Arrays.asList(
                        "§7Available factory types:",
                        "",
                        "§6Steel Mill",
                        "§7• Produces metal products",
                        "",
                        "§eRefinery",
                        "§7• Processes raw materials",
                        "",
                        "§bWorkshop",
                        "§7• Crafts finished goods",
                        "",
                        "§5Advanced Factory",
                        "§7• High-tech production")));

        // Plugin version (slot 24)
        inv.setItem(24, createItem(Material.NAME_TAG, "§d§lPlugin Info",
                Arrays.asList(
                        "§7Version: §e" + plugin.getDescription().getVersion(),
                        "§7Author: §eaithor",
                        "",
                        "§7Thank you for using FactoryCore!")));
    }

    private void displayFactoriesGuide(Inventory inv) {
        // Factory overview (slot 19)
        inv.setItem(19, createItem(Material.FURNACE, "§6§lWhat are Factories?",
                Arrays.asList(
                        "§7Factories are production",
                        "§7facilities you can own and",
                        "§7operate to create valuable",
                        "§7resources and items.",
                        "",
                        "§7Each factory has:",
                        "§7• Type (determines recipes)",
                        "§7• Level (affects efficiency)",
                        "§7• Storage (for materials)",
                        "§7• Status (running/stopped)")));

        // Buying (slot 20)
        inv.setItem(20, createItem(Material.GOLD_NUGGET, "§a§lBuying Factories",
                Arrays.asList(
                        "§7To buy a factory:",
                        "",
                        "§71. Open §eFactory Browse§7 from hub",
                        "§72. Find a factory you want",
                        "§73. Check the price",
                        "§74. Click to purchase",
                        "",
                        "§7Factories are tied to",
                        "§7WorldGuard regions.")));

        // Selling (slot 21)
        inv.setItem(21, createItem(Material.EMERALD, "§c§lSelling Factories",
                Arrays.asList(
                        "§7To sell a factory:",
                        "",
                        "§71. Open §eMy Factories§7 from hub",
                        "§72. Find your factory",
                        "§73. Shift + Right Click",
                        "§74. Confirm the sale",
                        "",
                        "§7You receive §e50%§7 of the",
                        "§7original purchase price.",
                        "",
                        "§c§lWarning: All items in storage",
                        "§c§lwill be lost when selling!")));

        // Upgrading (slot 22)
        inv.setItem(22, createItem(Material.EXPERIENCE_BOTTLE, "§d§lUpgrading",
                Arrays.asList(
                        "§7Upgrading increases efficiency:",
                        "",
                        "§7§nPer Level Bonus:§r",
                        "§7• Time Reduction: §e" + plugin.getConfig().getDouble("factory.level-bonuses.time-reduction", 10) + "%",
                        "§7• Cost Reduction: §e" + plugin.getConfig().getDouble("factory.level-bonuses.cost-reduction", 5) + "%",
                        "§7• More Storage Slots",
                        "",
                        "§7§nUpgrade Cost:§r",
                        "§e50% × Price × Current Level",
                        "",
                        "§7Max Level: §e" + plugin.getConfig().getInt("factory.max-level", 5))));

        // Storage (slot 23)
        inv.setItem(23, createItem(Material.CHEST, "§b§lFactory Storage",
                Arrays.asList(
                        "§7Each factory has storage for:",
                        "",
                        "§7§nInput Storage:§r",
                        "§7Store raw materials here",
                        "§7for production use.",
                        "",
                        "§7§nOutput Storage:§r",
                        "§7Completed products appear here.",
                        "§7Access via NPC employee.",
                        "",
                        "§7Storage slots: §e" + plugin.getConfig().getInt("factory.base-storage-slots", 9),
                        "§7+ §e" + plugin.getConfig().getInt("factory.slots-per-level", 9) + "§7 per level")));

        // Status (slot 24)
        inv.setItem(24, createItem(Material.REDSTONE_TORCH, "§e§lFactory Status",
                Arrays.asList(
                        "§7Factory status types:",
                        "",
                        "§a§lRUNNING",
                        "§7Factory is producing items",
                        "",
                        "§c§lSTOPPED",
                        "§7No production in progress",
                        "",
                        "§e§lOUT OF PARTS",
                        "§7Needs machine parts to run")));
    }

    private void displayProductionGuide(Inventory inv) {
        // How Production Works (slot 19)
        inv.setItem(19, createItem(Material.CRAFTING_TABLE, "§6§lHow Production Works",
                Arrays.asList(
                        "§7Production converts input",
                        "§7materials into valuable outputs.",
                        "",
                        "§7§nProcess:§r",
                        "§71. Stock input materials",
                        "§72. Select a recipe",
                        "§73. Confirm production",
                        "§74. Wait for completion",
                        "§75. Collect from output")));

        // Recipes (slot 20)
        inv.setItem(20, createItem(Material.KNOWLEDGE_BOOK, "§a§lRecipes",
                Arrays.asList(
                        "§7Each factory type has",
                        "§7specific recipes available.",
                        "",
                        "§7Recipes show:",
                        "§7• Required inputs",
                        "§7• Output products",
                        "§7• Production time",
                        "",
                        "§7Higher factory levels",
                        "§7produce faster!")));

        // Time and Progress (slot 21)
        inv.setItem(21, createItem(Material.CLOCK, "§b§lTime & Progress",
                Arrays.asList(
                        "§7Production takes time based on",
                        "§7the recipe selected.",
                        "",
                        "§7§nViewing Progress:§r",
                        "§7• Boss bar shows progress",
                        "§7• Check factory status in GUI",
                        "",
                        "§7§nTime Reduction:§r",
                        "§7Each level reduces time by",
                        "§e" + plugin.getConfig().getDouble("factory.level-bonuses.time-reduction", 10) + "% §7per level")));

        // Collecting Output (slot 22)
        inv.setItem(22, createItem(Material.HOPPER, "§d§lCollecting Output",
                Arrays.asList(
                        "§7When production completes:",
                        "",
                        "§71. Find your factory NPC",
                        "§72. Right-click the NPC",
                        "§73. Take items from output",
                        "",
                        "§7Or access from §eMy Factories§7",
                        "§7in the hub menu.")));

        // Tips (slot 23)
        inv.setItem(23, createItem(Material.TORCH, "§e§lProduction Tips",
                Arrays.asList(
                        "§7§nMaximize Efficiency:§r",
                        "",
                        "§7• Upgrade factories for speed",
                        "§7• Keep storage stocked",
                        "§7• Use continuous production",
                        "§7• Balance input/output",
                        "",
                        "§7Higher levels = faster profit!")));
    }

    private void displayEconomyGuide(Inventory inv) {
        // Economy Overview (slot 19)
        inv.setItem(19, createItem(Material.GOLD_BLOCK, "§6§lEconomy Overview",
                Arrays.asList(
                        "§7FactoryCore uses Vault economy",
                        "§7for all transactions.",
                        "",
                        "§7§nMoney Sources:§r",
                        "§7• Selling factories",
                        "§7• Selling on marketplace",
                        "",
                        "§7§nMoney Sinks:§r",
                        "§7• Buying factories",
                        "§7• Paying taxes & salaries",
                        "§7• Upgrading factories")));

        // Taxes (slot 20)
        inv.setItem(20, createItem(Material.GOLD_NUGGET, "§c§lTaxes",
                Arrays.asList(
                        "§7Factories incur periodic taxes!",
                        "",
                        "§7§nTax Rate:§r",
                        "§7Base: §e" + plugin.getConfig().getDouble("tax.rate", 5) + "%",
                        "§7Per Level: §e+" + plugin.getConfig().getDouble("tax.level-multiplier", 2.5) + "%",
                        "",
                        "§7§nTax Formula:§r",
                        "§ePrice × (Base + Level×Multiplier)",
                        "",
                        "§7Due Period: §e" + plugin.getConfig().getLong("tax.due-days", 7) + " days",
                        "§7Late Fee: §c" + plugin.getConfig().getDouble("tax.late-fee-rate", 5) + "%",
                        "",
                        "§c§lPay on time to avoid fees!")));

        // Invoices (slot 21)
        inv.setItem(21, createItem(Material.PAPER, "§e§lInvoices",
                Arrays.asList(
                        "§7Invoices are bills you must pay.",
                        "",
                        "§7§nInvoice Types:§r",
                        "§e• Tax§7 - Government fees",
                        "§6• Salary§7 - Employee wages",
                        "",
                        "§7Access via §eInvoice Center",
                        "§7in the hub menu.",
                        "",
                        "§c§lOverdue invoices may cause",
                        "§c§lfactory operation issues!")));

        // Marketplace (slot 22)
        inv.setItem(22, createItem(Material.EMERALD, "§2§lMarketplace",
                Arrays.asList(
                        "§7Buy and sell resources!",
                        "",
                        "§7§nTo Sell:§r",
                        "§71. Open Marketplace from hub",
                        "§72. List items for sale",
                        "§73. Set your price",
                        "§74. Wait for buyers",
                        "",
                        "§7§nTo Buy:§r",
                        "§71. Browse marketplace",
                        "§72. Find items you need",
                        "§73. Click to purchase")));

        // Salaries (slot 23)
        inv.setItem(23, createItem(Material.IRON_INGOT, "§6§lEmployee Salaries",
                Arrays.asList(
                        "§7NPC employees need wages!",
                        "",
                        "§7Salary Rate: §e" + plugin.getConfig().getDouble("salary.rate", 1) + "%",
                        "§7of factory value per period.",
                        "",
                        "§7Salaries appear as invoices",
                        "§7in your Invoice Center.")));
    }

    private void displayCommandsGuide(Inventory inv) {
        // Player Commands (slot 19)
        inv.setItem(19, createItem(Material.OAK_SIGN, "§a§lPlayer Commands",
                Arrays.asList(
                        "§e/fc hub",
                        "§7Open the main hub menu",
                        "",
                        "§e/fc gui <factory_id>",
                        "§7Open a factory's GUI",
                        "",
                        "§e/fc buy <factory_id>",
                        "§7Purchase a factory",
                        "",
                        "§e/fc sell <factory_id>",
                        "§7Sell your factory")));

        // More Player Commands (slot 20)
        inv.setItem(20, createItem(Material.OAK_SIGN, "§a§lMore Commands",
                Arrays.asList(
                        "§e/fc info <factory_id>",
                        "§7View factory information",
                        "",
                        "§e/fc teleport <factory_id>",
                        "§7Teleport to your factory",
                        "",
                        "§e/fc version",
                        "§7Show plugin version",
                        "",
                        "§e/fc help",
                        "§7Show command help")));

        // Admin Commands (slot 22)
        if (player.hasPermission("factorycore.admin")) {
            inv.setItem(22, createItem(Material.COMMAND_BLOCK, "§c§lAdmin Commands",
                    Arrays.asList(
                            "§e/fc admin create",
                            "§7Create a new factory",
                            "",
                            "§e/fc admin remove <id>",
                            "§7Remove a factory",
                            "",
                            "§e/fc admin list",
                            "§7List all factories",
                            "",
                            "§e/fc admin reload",
                            "§7Reload configurations")));

            inv.setItem(23, createItem(Material.COMMAND_BLOCK, "§c§lMore Admin",
                    Arrays.asList(
                            "§e/fc admin npc spawn <fac> [npc]",
                            "§7Spawn an NPC for factory",
                            "",
                            "§e/fc admin npc remove <id>",
                            "§7Remove an NPC",
                            "",
                            "§e/fc admin give <res> <amt> <plr>",
                            "§7Give resources to player",
                            "",
                            "§e/fc admin setowner <id> <plr>",
                            "§7Set factory owner")));
        }
    }

    private void displayTipsGuide(Inventory inv) {
        // Beginner Tips (slot 19)
        inv.setItem(19, createItem(Material.TORCH, "§a§lBeginner Tips",
                Arrays.asList(
                        "§7Starting out? Try these:",
                        "",
                        "§e• Start with a cheaper factory",
                        "§e• Learn the production cycle",
                        "§e• Keep storage organized",
                        "§e• Don't forget to pay taxes!",
                        "§e• Check invoices regularly")));

        // Efficiency Tips (slot 20)
        inv.setItem(20, createItem(Material.REDSTONE, "§6§lEfficiency Tips",
                Arrays.asList(
                        "§7Maximize your profits:",
                        "",
                        "§e• Upgrade for faster production",
                        "§e• Use continuous production",
                        "§e• Balance multiple factories",
                        "§e• Higher level = less time",
                        "§e• Watch your tax brackets!")));

        // Economy Tips (slot 21)
        inv.setItem(21, createItem(Material.GOLD_INGOT, "§e§lEconomy Tips",
                Arrays.asList(
                        "§7Make more money:",
                        "",
                        "§e• Sell high-value outputs",
                        "§e• Buy resources in bulk",
                        "§e• Pay invoices on time",
                        "§e• Avoid late fees!",
                        "§e• Reinvest profits wisely")));

        // Advanced Tips (slot 22)
        inv.setItem(22, createItem(Material.DIAMOND, "§b§lAdvanced Tips",
                Arrays.asList(
                        "§7For experienced players:",
                        "",
                        "§e• Create production chains",
                        "§e• Specialize by factory type",
                        "§e• Optimize tax efficiency",
                        "§e• Use marketplace strategically",
                        "§e• Time upgrades with cash flow")));

        // Common Mistakes (slot 23)
        inv.setItem(23, createItem(Material.BARRIER, "§c§lAvoid These Mistakes",
                Arrays.asList(
                        "§7Don't make these errors:",
                        "",
                        "§c• Ignoring invoices",
                        "§c• Overextending too fast",
                        "§c• Forgetting to collect output",
                        "§c• Not checking production",
                        "§c• Selling factories with items")));
    }

    private ItemStack createPluginInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7Complete factory management");
        lore.add("§7and economy system.");
        lore.add("");
        lore.add("§7Select a category above");
        lore.add("§7to learn more!");

        return createItem(Material.BOOK, "§d§lHelp & Info", lore);
    }

    private ItemStack createCategoryItem(String category, Material material, String name, String description) {
        List<String> lore = new ArrayList<>();
        lore.add("§7" + description);
        lore.add("");
        if (currentCategory.equals(category)) {
            lore.add("§a§l► Currently viewing");
        } else {
            lore.add("§eClick to view!");
        }

        ItemStack item = createItem(material, name, lore);
        
        // Add glow effect if current category
        if (currentCategory.equals(category)) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }

        return item;
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
