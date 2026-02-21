package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.managers.TaxManager;
import com.aithor.factorycore.models.*;
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

/**
 * Tax Center GUI - Comprehensive tax management interface
 */
public class TaxCenterGUI {

    private final FactoryCore plugin;
    private final Player player;
    private int currentPage = 0;

    public TaxCenterGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openTaxCenterMenu() {
        openTaxCenterMenu(0);
    }

    public void openTaxCenterMenu(int page) {
        this.currentPage = page;

        Inventory inv = Bukkit.createInventory(null, 54, "§c§lTax Center §8- §ePage " + (page + 1));

        TaxManager taxManager = plugin.getTaxManager();
        List<Factory> playerFactories = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());

        // Calculate totals
        double totalTaxDue = taxManager != null ? taxManager.getTotalTaxDue(player.getUniqueId()) : 0;
        int overdueCount = taxManager != null ? taxManager.getOverdueTaxes(player.getUniqueId()).size() : 0;

        // Fill borders
        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);

        // Header info with tax overview
        Material headerMat = overdueCount > 0 ? Material.RED_CONCRETE
                : (totalTaxDue > 0 ? Material.YELLOW_CONCRETE : Material.LIME_CONCRETE);
        List<String> headerLore = new ArrayList<>();
        headerLore.add("§7Your tax overview");
        headerLore.add("");
        headerLore.add("§eTotal Tax Due: §6$" + String.format("%.2f", totalTaxDue));
        headerLore.add("§eFactories Taxed: §e" + playerFactories.size());
        if (overdueCount > 0) {
            headerLore.add("");
            headerLore.add("§c§lWARNING: " + overdueCount + " overdue taxes!");
        }
        headerLore.add("");
        headerLore.add("§7Tax Rate: §e" + plugin.getConfig().getDouble("tax.rate", 5.0) + "%");
        headerLore.add(
                "§7Level Multiplier: §e" + plugin.getConfig().getDouble("tax.level-multiplier", 2.5) + "% per level");

        double glbReduction = plugin.getResearchManager() != null
                ? plugin.getResearchManager().getTaxReduction(player.getUniqueId())
                : 0;
        if (glbReduction > 0) {
            headerLore.add("§d🔬 Research Buff: §f-" + String.format("%.0f", glbReduction) + "% §dTax Reduction!");
        }

        // Next collection time
        if (taxManager != null) {
            long timeUntilNext = taxManager.getTimeUntilNextCollection();
            if (timeUntilNext > 0) {
                long hours = timeUntilNext / (60 * 60 * 1000);
                long minutes = (timeUntilNext % (60 * 60 * 1000)) / (60 * 1000);
                headerLore.add("");
                headerLore.add("§7Next Assessment: §e" + hours + "h " + minutes + "m");
            }
        }

        inv.setItem(4, createItem(headerMat, "§c§lTax Center", headerLore));

        // Tax info slots (slots 9-44)
        int slotsPerPage = 36;
        int startIndex = page * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, playerFactories.size());

        int slot = 9;
        for (int i = startIndex; i < endIndex && slot < 45; i++) {
            Factory factory = playerFactories.get(i);
            inv.setItem(slot++, createFactoryTaxItem(factory));
        }

        // Show message if no factories
        if (playerFactories.isEmpty()) {
            inv.setItem(22, createItem(Material.BARRIER, "§c§lNo Factories",
                    Arrays.asList(
                            "§7You don't own any factories!",
                            "",
                            "§7No taxes to pay.")));
        }

        // Navigation and control buttons

        // Previous page (slot 45)
        if (page > 0) {
            inv.setItem(45, createNavigationItem(Material.ARROW, "§e§l◄ Previous Page", page - 1));
        }

        // Tax Calculator (slot 46)
        inv.setItem(46, createTaxCalculatorItem());

        // Payment History (slot 47)
        inv.setItem(47, createPaymentHistoryItem());

        // Pay All button (slot 48)
        if (totalTaxDue > 0) {
            boolean canPayAll = plugin.getEconomy().has(player, totalTaxDue);
            Material payAllMat = canPayAll ? Material.GOLD_BLOCK : Material.IRON_BLOCK;
            List<String> payAllLore = new ArrayList<>();
            payAllLore.add("§7Pay all outstanding taxes");
            payAllLore.add("");
            payAllLore.add("§eTotal Due: §6$" + String.format("%.2f", totalTaxDue));
            payAllLore.add("§eYour Balance: §6$" + String.format("%.2f", plugin.getEconomy().getBalance(player)));
            payAllLore.add("");
            if (canPayAll) {
                payAllLore.add("§a§lClick to pay all taxes!");
            } else {
                payAllLore.add("§c§lInsufficient funds!");
            }
            inv.setItem(48, createItem(payAllMat, "§6§lPay All Taxes", payAllLore));
        }

        // Tax Rate Info (slot 49)
        inv.setItem(49, createTaxRateInfoItem());

        // Page info (slot 50)
        int totalPages = (int) Math.ceil((double) playerFactories.size() / slotsPerPage);
        inv.setItem(50, createItem(Material.PAPER, "§e§lPage Info",
                Arrays.asList(
                        "§7Page: §e" + (page + 1) + " / " + Math.max(1, totalPages),
                        "§7Factories: §e" + playerFactories.size())));

        // Back to hub (slot 51)
        inv.setItem(51, createItem(Material.DARK_OAK_DOOR, "§c§lBack to Hub",
                Arrays.asList("§7Return to main menu")));

        // Next page (slot 53)
        if (endIndex < playerFactories.size()) {
            inv.setItem(53, createNavigationItem(Material.ARROW, "§e§lNext Page ►", page + 1));
        }

        player.openInventory(inv);
    }

    private ItemStack createFactoryTaxItem(Factory factory) {
        TaxManager taxManager = plugin.getTaxManager();
        TaxManager.TaxRecord record = taxManager != null ? taxManager.getTaxRecord(factory.getId()) : null;

        double taxDue = record != null ? record.amountDue : 0;
        double nextTax = taxManager != null ? taxManager.calculateTax(factory) : 0;
        boolean isOverdue = record != null && record.overdue;

        Material material;
        if (isOverdue) {
            material = Material.RED_CONCRETE;
        } else if (taxDue > 0) {
            material = Material.YELLOW_CONCRETE;
        } else {
            material = Material.LIME_CONCRETE;
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7Type: " + factory.getType().getDisplayName());
        lore.add("§7Level: §e" + factory.getLevel());
        lore.add("§7Value: §6$" + String.format("%.2f", factory.getPrice()));
        lore.add("");

        // Current tax status
        if (taxDue > 0) {
            lore.add("§eCurrent Tax Due: §6$" + String.format("%.2f", taxDue));
            if (isOverdue) {
                lore.add("§c§l⚠ OVERDUE!");
            } else if (record != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
                lore.add("§7Due: §e" + sdf.format(new Date(record.dueDate)));
            }
        } else {
            lore.add("§a✓ No tax due");
        }

        lore.add("");
        double factoryTaxReduction = plugin.getResearchManager() != null
                ? plugin.getResearchManager().getTaxReduction(player.getUniqueId())
                : 0;

        if (factoryTaxReduction > 0 && nextTax > 0) {
            double unbuffedTax = nextTax / (1 - (factoryTaxReduction / 100.0));
            lore.add("§7Next Tax Assessment: §c§m$" + String.format("%.2f", unbuffedTax) + "§r §a$"
                    + String.format("%.2f", nextTax) + " §d(-" + String.format("%.0f", factoryTaxReduction) + "%)");
        } else {
            lore.add("§7Next Tax Assessment: §e$" + String.format("%.2f", nextTax));
        }

        lore.add("§7Tax Rate: §e" + String.format("%.1f", (plugin.getConfig().getDouble("tax.rate", 5.0) +
                plugin.getConfig().getDouble("tax.level-multiplier", 2.5) * (factory.getLevel() - 1))) + "%");

        if (taxDue > 0) {
            lore.add("");
            boolean canPay = plugin.getEconomy().has(player, taxDue);
            if (canPay) {
                lore.add("§a§lClick to pay tax!");
            } else {
                lore.add("§c§lInsufficient funds!");
            }
        }

        String title = (isOverdue ? "§c§l⚠ " : "") + factory.getType().getDisplayName() + " §7- §e" + factory.getId();
        ItemStack item = createItem(material, title, lore);

        // Store factory ID for click handling
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "tax_factory_id"),
                    PersistentDataType.STRING,
                    factory.getId());
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createTaxCalculatorItem() {
        List<Factory> factories = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());
        TaxManager taxManager = plugin.getTaxManager();

        List<String> lore = new ArrayList<>();
        lore.add("§7Preview your upcoming taxes");
        lore.add("");

        double totalNextTax = 0;
        double unbuffedTotalTax = 0;
        double taxReduc = plugin.getResearchManager() != null
                ? plugin.getResearchManager().getTaxReduction(player.getUniqueId())
                : 0;

        for (Factory factory : factories) {
            double tax = taxManager != null ? taxManager.calculateTax(factory) : 0;
            totalNextTax += tax;
            if (taxReduc > 0 && tax > 0) {
                unbuffedTotalTax += tax / (1 - (taxReduc / 100.0));
            } else {
                unbuffedTotalTax += tax;
            }
        }

        if (taxReduc > 0 && totalNextTax > 0) {
            lore.add("§eNext Assessment Total: §c§m$" + String.format("%.2f", unbuffedTotalTax) + "§r §a$"
                    + String.format("%.2f", totalNextTax) + " §d(-" + String.format("%.0f", taxReduc) + "%)");
        } else {
            lore.add("§eNext Assessment Total: §6$" + String.format("%.2f", totalNextTax));
        }
        lore.add("");
        lore.add("§7Breakdown by factory type:");

        // Group by type
        Map<FactoryType, Double> taxByType = new HashMap<>();
        for (Factory factory : factories) {
            double tax = taxManager != null ? taxManager.calculateTax(factory) : 0;
            taxByType.merge(factory.getType(), tax, Double::sum);
        }

        for (Map.Entry<FactoryType, Double> entry : taxByType.entrySet()) {
            lore.add("§7• " + entry.getKey().getDisplayName() + "§7: §e$" + String.format("%.2f", entry.getValue()));
        }

        return createItem(Material.GOLD_NUGGET, "§e§lTax Calculator", lore);
    }

    private ItemStack createPaymentHistoryItem() {
        TaxManager taxManager = plugin.getTaxManager();
        List<TaxManager.TaxPayment> history = taxManager != null ? taxManager.getPaymentHistory(player.getUniqueId())
                : new ArrayList<>();

        List<String> lore = new ArrayList<>();
        lore.add("§7Your recent tax payments");
        lore.add("");

        if (history.isEmpty()) {
            lore.add("§7No payment history");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
            // Show last 5 payments
            int count = 0;
            for (int i = history.size() - 1; i >= 0 && count < 5; i--, count++) {
                TaxManager.TaxPayment payment = history.get(i);
                lore.add("§e" + sdf.format(new Date(payment.timestamp)) + " §7- §6$"
                        + String.format("%.2f", payment.amount));
            }

            if (history.size() > 5) {
                lore.add("§7... and " + (history.size() - 5) + " more");
            }

            double totalPaid = history.stream().mapToDouble(p -> p.amount).sum();
            lore.add("");
            lore.add("§eTotal Paid: §6$" + String.format("%.2f", totalPaid));
        }

        return createItem(Material.BOOK, "§d§lPayment History", lore);
    }

    private ItemStack createTaxRateInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7How taxes are calculated");
        lore.add("");
        lore.add("§7Base Rate: §e" + plugin.getConfig().getDouble("tax.rate", 5.0) + "%");
        lore.add("§7Per Level: §e+" + plugin.getConfig().getDouble("tax.level-multiplier", 2.5) + "%");
        lore.add("§7Due Period: §e" + plugin.getConfig().getLong("tax.due-days", 7) + " days");
        lore.add("§7Late Fee: §e" + plugin.getConfig().getDouble("tax.late-fee-rate", 5.0) + "%");

        double buff = plugin.getResearchManager() != null
                ? plugin.getResearchManager().getTaxReduction(player.getUniqueId())
                : 0;
        if (buff > 0) {
            lore.add("§dResearch Buff: §f-" + String.format("%.0f", buff) + "% §dtax reduction");
        }

        lore.add("");
        lore.add("§7Formula:");
        lore.add("§eTax = Price × (Base + Level × Multiplier)");
        if (buff > 0) {
            lore.add("§dFinal Tax = Tax - " + String.format("%.0f", buff) + "%");
        }
        lore.add("");
        lore.add("§7Example for Level 3 factory ($10,000):");
        double exampleRate = plugin.getConfig().getDouble("tax.rate", 5.0) +
                2 * plugin.getConfig().getDouble("tax.level-multiplier", 2.5);
        lore.add("§e$10,000 × " + String.format("%.1f", exampleRate) + "% = §6$"
                + String.format("%.2f", 10000 * exampleRate / 100));

        return createItem(Material.KNOWLEDGE_BOOK, "§b§lTax Information", lore);
    }

    private ItemStack createNavigationItem(Material material, String name, int targetPage) {
        ItemStack item = createItem(material, name, Arrays.asList("§7Go to page " + (targetPage + 1)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "tax_center_page"),
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
