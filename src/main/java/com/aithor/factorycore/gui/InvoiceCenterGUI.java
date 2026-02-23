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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import com.aithor.factorycore.managers.TaxManager;

/**
 * Invoice Center GUI - Centralized invoice management for all factories
 */
public class InvoiceCenterGUI {

    private final FactoryCore plugin;
    private final Player player;
    private int currentPage = 0;
    private String filterType = "all"; // all, tax, salary, overdue
    private String sortBy = "date"; // date, amount, type

    public InvoiceCenterGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openInvoiceCenterMenu() {
        openInvoiceCenterMenu(0);
    }

    public void openInvoiceCenterMenu(int page) {
        this.currentPage = page;

        Inventory inv = Bukkit.createInventory(null, 54, "§e§lInvoice Center §8- §ePage " + (page + 1));

        // Get all invoices for player
        List<Invoice> allInvoices = new ArrayList<>(
                plugin.getInvoiceManager().getInvoicesByOwner(player.getUniqueId()));

        // Add TaxManager records
        if (plugin.getTaxManager() != null) {
            for (TaxManager.TaxRecord record : plugin.getTaxManager().getPlayerTaxRecords(player.getUniqueId())) {
                if (record.amountDue > 0) {
                    Invoice taxInvoice = new Invoice(
                            "TM_" + record.factoryId,
                            player.getUniqueId(),
                            InvoiceType.TAX,
                            record.amountDue,
                            record.dueDate);
                    allInvoices.add(taxInvoice);
                }
            }
        }

        // Apply filter
        List<Invoice> filteredInvoices;
        switch (filterType) {
            case "tax":
                filteredInvoices = allInvoices.stream()
                        .filter(i -> i.getType() == InvoiceType.TAX)
                        .collect(Collectors.toList());
                break;
            case "salary":
                filteredInvoices = allInvoices.stream()
                        .filter(i -> i.getType() == InvoiceType.SALARY)
                        .collect(Collectors.toList());
                break;
            case "overdue":
                filteredInvoices = allInvoices.stream()
                        .filter(Invoice::isOverdue)
                        .collect(Collectors.toList());
                break;
            default:
                filteredInvoices = new ArrayList<>(allInvoices);
        }

        // Apply sorting
        switch (sortBy) {
            case "amount":
                filteredInvoices.sort((a, b) -> Double.compare(b.getAmount(), a.getAmount()));
                break;
            case "amount_asc":
                filteredInvoices.sort(Comparator.comparingDouble(Invoice::getAmount));
                break;
            case "type":
                filteredInvoices.sort(Comparator.comparing(i -> i.getType().name()));
                break;
            default: // date (due date, earliest first)
                filteredInvoices.sort(Comparator.comparingLong(Invoice::getDueDate));
        }

        // Fill borders
        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);

        // Header info and statistics
        double totalDue = allInvoices.stream().mapToDouble(Invoice::getAmount).sum();
        long overdueCount = allInvoices.stream().filter(Invoice::isOverdue).count();
        double overdueDue = allInvoices.stream().filter(Invoice::isOverdue).mapToDouble(Invoice::getAmount).sum();

        Material headerMat = overdueCount > 0 ? Material.RED_CONCRETE : Material.LIME_CONCRETE;
        inv.setItem(4, createItem(headerMat, "§e§lInvoice Center",
                Arrays.asList(
                        "§7Manage all your invoices",
                        "",
                        "§ePending Invoices: §6" + allInvoices.size(),
                        "§eTotal Due: §6$" + String.format("%.2f", totalDue),
                        "",
                        overdueCount > 0
                                ? "§c§lOverdue: " + overdueCount + " invoices ($" + String.format("%.2f", overdueDue)
                                        + ")"
                                : "§a✓ No overdue invoices",
                        "",
                        "§7Filter: §e" + getFilterDisplayName(),
                        "§7Sort: §e" + getSortDisplayName())));

        // Invoice slots (slots 9-44)
        int slotsPerPage = 36;
        int startIndex = page * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, filteredInvoices.size());

        int slot = 9;
        for (int i = startIndex; i < endIndex && slot < 45; i++) {
            Invoice invoice = filteredInvoices.get(i);
            inv.setItem(slot++, createInvoiceItem(invoice));
        }

        // Show empty message if no invoices
        if (filteredInvoices.isEmpty()) {
            inv.setItem(22, createItem(Material.EMERALD_BLOCK, "§a§lNo Invoices!",
                    Arrays.asList(
                            filterType.equals("all") ? "§7You have no pending invoices!"
                                    : "§7No invoices match the current filter",
                            "",
                            "§aGreat job keeping up with payments!")));
        }

        // Navigation and control buttons

        // Previous page (slot 45)
        if (page > 0) {
            inv.setItem(45, createNavigationItem(Material.ARROW, "§e§l◄ Previous Page", page - 1));
        }

        // Filter button (slot 46)
        inv.setItem(46, createFilterItem());

        // Sort button (slot 47)
        inv.setItem(47, createSortItem());

        // Pay All button (slot 48)
        if (!allInvoices.isEmpty()) {
            boolean canPayAll = plugin.getEconomy().has(player, totalDue);
            Material payAllMat = canPayAll ? Material.GOLD_BLOCK : Material.IRON_BLOCK;
            List<String> payAllLore = new ArrayList<>();
            payAllLore.add("§7Pay all pending invoices at once");
            payAllLore.add("");
            payAllLore.add("§eTotal: §6$" + String.format("%.2f", totalDue));
            payAllLore.add("§eYour Balance: §6$" + String.format("%.2f", plugin.getEconomy().getBalance(player)));
            payAllLore.add("");
            if (canPayAll) {
                payAllLore.add("§a§lClick to pay all!");
            } else {
                payAllLore.add("§c§lInsufficient funds!");
            }
            inv.setItem(48, createItem(payAllMat, "§6§lPay All Invoices", payAllLore));
        }

        // Page info (slot 49)
        int totalPages = (int) Math.ceil((double) filteredInvoices.size() / slotsPerPage);
        inv.setItem(49, createItem(Material.PAPER, "§e§lPage Info",
                Arrays.asList(
                        "§7Page: §e" + (page + 1) + " / " + Math.max(1, totalPages),
                        "§7Showing: §e" + filteredInvoices.size() + " invoices")));

        // Summary by type (slot 50)
        inv.setItem(50, createSummaryItem(allInvoices));

        // Back to hub (slot 51)
        inv.setItem(51, createItem(Material.DARK_OAK_DOOR, "§c§lBack to Hub",
                Arrays.asList("§7Return to main menu")));

        // Next page (slot 53)
        if (endIndex < filteredInvoices.size()) {
            inv.setItem(53, createNavigationItem(Material.ARROW, "§e§lNext Page ►", page + 1));
        }

        player.openInventory(inv);
    }

    private ItemStack createInvoiceItem(Invoice invoice) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        boolean isOverdue = invoice.isOverdue();

        Material material;
        if (isOverdue) {
            material = Material.RED_STAINED_GLASS_PANE;
        } else {
            material = invoice.getType() == InvoiceType.TAX ? Material.GOLD_NUGGET : Material.PAPER;
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7Type: " + invoice.getType().getDisplay());
        lore.add("§7Amount: §6$" + String.format("%.2f", invoice.getAmount()));
        lore.add("");
        lore.add("§7Due Date: §e" + sdf.format(new Date(invoice.getDueDate())));

        // Time remaining or overdue
        long timeRemaining = invoice.getDueDate() - System.currentTimeMillis();
        if (timeRemaining > 0) {
            long days = timeRemaining / (24 * 60 * 60 * 1000);
            long hours = (timeRemaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
            lore.add("§7Time Left: §e" + days + "d " + hours + "h");
        } else {
            long overdueDays = Math.abs(timeRemaining) / (24 * 60 * 60 * 1000);
            lore.add("§c§lOVERDUE by " + overdueDays + " days!");
        }

        lore.add("");

        // Get associated factory info
        String invoiceId = invoice.getId();
        Factory factory = null;
        if (invoiceId.startsWith("TM_")) {
            factory = plugin.getFactoryManager().getFactory(invoiceId.substring(3));
        } else if (invoiceId.contains("_")) {
            String[] parts = invoiceId.split("_");
            if (parts.length >= 3) {
                String factoryIdStr = String.join("_", Arrays.copyOfRange(parts, 1, parts.length - 1));
                factory = plugin.getFactoryManager().getFactory(factoryIdStr);
            }
        }

        if (factory == null) {
            factory = plugin.getFactoryManager().getFactory(invoice.getFactoryId().toString());
        }

        if (factory != null) {
            lore.add("§7Factory: §e" + factory.getId());
        } else {
            lore.add("§7Factory: §eUnknown");
        }

        lore.add("");

        boolean canPay = plugin.getEconomy().has(player, invoice.getAmount());
        if (canPay) {
            lore.add("§a§lClick to pay!");
        } else {
            lore.add("§c§lInsufficient funds!");
            lore.add("§7Need: §6$" + String.format("%.2f", invoice.getAmount() - plugin.getEconomy().getBalance(player))
                    + " more");
        }

        String title = (isOverdue ? "§c§l⚠ " : "") + invoice.getType().getDisplay() + " §7- §6$"
                + String.format("%.2f", invoice.getAmount());
        ItemStack item = createItem(material, title, lore);

        // Store invoice ID for click handling
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "center_invoice_id"),
                    PersistentDataType.STRING,
                    invoice.getId());
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createFilterItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7Current: §e" + getFilterDisplayName());
        lore.add("");
        lore.add("§7Filters:");
        lore.add((filterType.equals("all") ? "§a► " : "§7• ") + "All Invoices");
        lore.add((filterType.equals("tax") ? "§a► " : "§7• ") + "Tax Only");
        lore.add((filterType.equals("salary") ? "§a► " : "§7• ") + "Salary Only");
        lore.add((filterType.equals("overdue") ? "§a► " : "§7• ") + "Overdue Only");
        lore.add("");
        lore.add("§eClick to cycle filter!");

        return createItem(Material.HOPPER, "§b§lFilter", lore);
    }

    private ItemStack createSortItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7Current: §e" + getSortDisplayName());
        lore.add("");
        lore.add("§7Sort Options:");
        lore.add((sortBy.equals("date") ? "§a► " : "§7• ") + "Due Date (Earliest)");
        lore.add((sortBy.equals("amount") ? "§a► " : "§7• ") + "Amount (Highest)");
        lore.add((sortBy.equals("amount_asc") ? "§a► " : "§7• ") + "Amount (Lowest)");
        lore.add((sortBy.equals("type") ? "§a► " : "§7• ") + "Type");
        lore.add("");
        lore.add("§eClick to cycle sort!");

        return createItem(Material.COMPARATOR, "§d§lSort", lore);
    }

    private ItemStack createSummaryItem(List<Invoice> invoices) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Invoice breakdown by type");
        lore.add("");

        // Count by type
        Map<InvoiceType, List<Invoice>> byType = invoices.stream()
                .collect(Collectors.groupingBy(Invoice::getType));

        for (InvoiceType type : InvoiceType.values()) {
            List<Invoice> typeInvoices = byType.getOrDefault(type, new ArrayList<>());
            double typeTotal = typeInvoices.stream().mapToDouble(Invoice::getAmount).sum();
            lore.add(type.getDisplay() + "§7: §e" + typeInvoices.size() + " §7($" + String.format("%.2f", typeTotal)
                    + ")");
        }

        return createItem(Material.BOOK, "§e§lSummary", lore);
    }

    private String getFilterDisplayName() {
        switch (filterType) {
            case "tax":
                return "Tax Only";
            case "salary":
                return "Salary Only";
            case "overdue":
                return "Overdue Only";
            default:
                return "All Invoices";
        }
    }

    private String getSortDisplayName() {
        switch (sortBy) {
            case "amount":
                return "Amount (Highest)";
            case "amount_asc":
                return "Amount (Lowest)";
            case "type":
                return "Type";
            default:
                return "Due Date (Earliest)";
        }
    }

    public void cycleFilter() {
        switch (filterType) {
            case "all":
                filterType = "tax";
                break;
            case "tax":
                filterType = "salary";
                break;
            case "salary":
                filterType = "overdue";
                break;
            default:
                filterType = "all";
                break;
        }
    }

    public void cycleSort() {
        switch (sortBy) {
            case "date":
                sortBy = "amount";
                break;
            case "amount":
                sortBy = "amount_asc";
                break;
            case "amount_asc":
                sortBy = "type";
                break;
            default:
                sortBy = "date";
                break;
        }
    }

    private ItemStack createNavigationItem(Material material, String name, int targetPage) {
        ItemStack item = createItem(material, name, Arrays.asList("§7Go to page " + (targetPage + 1)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "invoice_center_page"),
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
