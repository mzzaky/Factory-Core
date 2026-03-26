package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.managers.DistributionManager;
import com.aithor.factorycore.models.ActiveDistributionRequest;
import com.aithor.factorycore.models.Company;
import com.aithor.factorycore.models.DistributionEvent;
import com.aithor.factorycore.models.ResourceItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * DistributionCenterGUI - Custom GUI for the Distribution Center feature.
 * Displays active delivery requests, company info, and allows resource
 * delivery.
 */
public class DistributionCenterGUI {

    private final FactoryCore plugin;
    private final Player player;

    // Request display slots (row 2 and row 3, centered)
    private static final int[] REQUEST_SLOTS = { 20, 21, 22, 23, 24 };

    public DistributionCenterGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    // ==================== MAIN MENU ====================

    public void openDistributionCenter() {
        DistributionManager manager = plugin.getDistributionManager();
        List<ActiveDistributionRequest> requests = manager.getActiveRequests(player.getUniqueId());

        String title = "\u00a76\u00a7lDistribution Center \u00a78(" + requests.size() + " active)";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Fill borders
        fillBorder(inv);

        // Header info (slot 4)
        inv.setItem(4, createHeaderItem(requests));

        // Toggle Timer Bossbar (slot 31)
        boolean bbEnabled = manager.isBossbarEnabled(player.getUniqueId());
        ItemStack bbToggle = createItem(Material.CLOCK, "§e§lToggle Timer Reminder",
                Arrays.asList(
                        "§7Toggle the visibility of a bossbar",
                        "§7timer for your active requests.",
                        "",
                        "§eStatus: " + (bbEnabled ? "§aEnabled" : "§cDisabled"),
                        "",
                        "§eClick to toggle!"));
        ItemMeta bbMeta = bbToggle.getItemMeta();
        if (bbMeta != null) {
            bbMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "dist_toggle_bossbar"),
                    PersistentDataType.INTEGER, bbEnabled ? 1 : 0);
            bbToggle.setItemMeta(bbMeta);
        }
        inv.setItem(31, bbToggle);

        // Toggle Distribution Offers (slot 13)
        boolean distEnabled = manager.isDistributionEnabled(player.getUniqueId());
        ItemStack distToggle = createItem(Material.ENDER_PEARL, "§d§lToggle Distribution Offers",
                Arrays.asList(
                        "§7Toggle whether you want to receive",
                        "§7new distribution event offers.",
                        "",
                        "§eStatus: " + (distEnabled ? "§aEnabled" : "§cDisabled"),
                        "",
                        "§eClick to toggle!"));
        ItemMeta distMeta = distToggle.getItemMeta();
        if (distMeta != null) {
            distMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "dist_toggle_offers"),
                    PersistentDataType.INTEGER, distEnabled ? 1 : 0);
            distToggle.setItemMeta(distMeta);
        }
        inv.setItem(13, distToggle);

        // Active requests (row 2-3)
        for (int i = 0; i < Math.min(requests.size(), REQUEST_SLOTS.length); i++) {
            inv.setItem(REQUEST_SLOTS[i], createRequestItem(requests.get(i)));
        }

        // Empty request slots
        for (int i = requests.size(); i < REQUEST_SLOTS.length; i++) {
            inv.setItem(REQUEST_SLOTS[i], createEmptySlotItem());
        }

        // Tax info (slot 30)
        inv.setItem(30, createTaxInfoItem());

        // Stats panel (slot 32)
        inv.setItem(32, createStatsItem());

        // Back to Hub (slot 49)
        inv.setItem(49, createItem(Material.ARROW, "\u00a7c\u00a7lBack to Hub",
                Arrays.asList("\u00a77Return to main menu")));

        player.openInventory(inv);
    }

    // ==================== REQUEST DETAIL VIEW ====================

    public void openRequestDetail(ActiveDistributionRequest request) {
        DistributionManager manager = plugin.getDistributionManager();
        DistributionEvent event = manager.getEvent(request.getEventId());
        if (event == null) {
            player.sendMessage("\u00a7cThis request is no longer valid.");
            return;
        }

        Company company = manager.getCompany(event.getCompanyId());
        String companyName = company != null ? company.getName() : "\u00a77Unknown";

        String title = "\u00a76\u00a7lRequest: " + event.getName();
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Fill borders
        fillBorder(inv);

        // Company info (slot 4)
        if (company != null) {
            inv.setItem(4, createCompanyInfoItem(company));
        }

        // Request overview (slot 13)
        inv.setItem(13, createRequestOverviewItem(request, event));

        // Timer display (slot 14)
        inv.setItem(14, createTimerItem(request, event));

        // Reward info (slot 15)
        inv.setItem(15, createRewardInfoItem(request, event));

        // Demand items (row 3) - clickable to deliver
        List<String> demands = new ArrayList<>(request.getDeliveredResources().keySet());
        int[] demandSlots = { 29, 30, 31, 32, 33 };
        for (int i = 0; i < Math.min(demands.size(), demandSlots.length); i++) {
            String resourceId = demands.get(i);
            boolean delivered = request.isResourceDelivered(resourceId);
            inv.setItem(demandSlots[i], createDemandItem(request, resourceId, delivered));
        }

        // Deliver All button (slot 40) - only if there are undelivered items
        boolean hasUndelivered = request.getDeliveredResources().values().stream().anyMatch(v -> !v);
        if (hasUndelivered && !request.isExpired()) {
            ItemStack deliverAll = createItem(Material.HOPPER, "\u00a7a\u00a7lDeliver All Resources",
                    Arrays.asList(
                            "\u00a77Deliver all demanded resources",
                            "\u00a77from your inventory at once.",
                            "",
                            "\u00a7eClick to deliver all!"));
            ItemMeta meta = deliverAll.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "dist_deliver_all"),
                        PersistentDataType.STRING, request.getRequestId());
                deliverAll.setItemMeta(meta);
            }
            inv.setItem(40, deliverAll);
        }

        // Back button (slot 49)
        inv.setItem(49, createItem(Material.ARROW, "\u00a7c\u00a7lBack to Distribution Center",
                Arrays.asList("\u00a77Return to request list")));

        player.openInventory(inv);
    }

    // ==================== ITEM BUILDERS ====================

    private ItemStack createHeaderItem(List<ActiveDistributionRequest> requests) {
        DistributionManager manager = plugin.getDistributionManager();
        int maxRequests = plugin.getConfig().getInt("distribution.max-active-requests", 3);

        List<String> lore = new ArrayList<>();
        lore.add("\u00a77Companies deliver resource requests");
        lore.add("\u00a77to you. Fulfill them for rewards!");
        lore.add("");
        lore.add("\u00a7eActive Requests: \u00a76" + requests.size() + "\u00a77/\u00a76" + maxRequests);
        lore.add("\u00a7eCompanies: \u00a76" + manager.getTotalCompanyCount());
        lore.add("");

        if (requests.isEmpty()) {
            lore.add("\u00a78No active requests right now.");
            lore.add("\u00a78New requests appear periodically!");
        } else {
            lore.add("\u00a7aClick on a request to view details");
            lore.add("\u00a7aand deliver resources!");
        }

        return createItem(Material.CHEST_MINECART, "\u00a76\u00a7lDistribution Center", lore);
    }

    private ItemStack createRequestItem(ActiveDistributionRequest request) {
        DistributionManager manager = plugin.getDistributionManager();
        DistributionEvent event = manager.getEvent(request.getEventId());
        if (event == null) {
            return createItem(Material.BARRIER, "\u00a7cInvalid Request",
                    Arrays.asList("\u00a77This request is corrupted."));
        }

        Company company = manager.getCompany(event.getCompanyId());
        String companyName = company != null ? company.getName() : "\u00a77Unknown Company";

        // Determine material based on status
        Material material;
        if (request.isExpired()) {
            material = Material.RED_STAINED_GLASS_PANE;
        } else if (request.getDeliveredCount() > 0) {
            material = Material.YELLOW_STAINED_GLASS_PANE;
        } else {
            material = Material.LIME_STAINED_GLASS_PANE;
        }

        List<String> lore = new ArrayList<>();
        lore.addAll(event.getLore());
        lore.add("");
        lore.add("\u00a77Company: " + companyName);
        lore.add("");

        // Progress
        lore.add("\u00a7eProgress: \u00a76" + request.getDeliveredCount() + "\u00a77/\u00a76" + request.getTotalDemand()
                + " resources");

        // Progress bar
        int barLen = 20;
        double progress = request.getTotalDemand() > 0
                ? (double) request.getDeliveredCount() / request.getTotalDemand()
                : 0;
        int filled = (int) (barLen * progress);
        StringBuilder bar = new StringBuilder("\u00a7a");
        for (int i = 0; i < barLen; i++) {
            if (i == filled)
                bar.append("\u00a77");
            bar.append("|");
        }
        lore.add(bar.toString());
        lore.add("");

        // Time remaining
        if (request.isExpired()) {
            lore.add("\u00a7c\u00a7lEXPIRED");
        } else {
            lore.add("\u00a7eTime Left: \u00a7b" + DistributionManager.formatTime(request.getRemainingTimeMillis()));
        }

        // Reward
        double taxRate = plugin.getConfig().getDouble("distribution.tax_distribution_rate", 25.0);
        double baseReward = event.getPriceOffer();
        double afterTax = baseReward * (1 - taxRate / 100.0);
        lore.add("\u00a7eReward: \u00a76$" + String.format("%.2f", afterTax) + " \u00a78(after "
                + String.format("%.0f", taxRate) + "% tax)");

        // Bonus info
        if (event.getBonus() > 1.0) {
            lore.add("\u00a7eSpeed Bonus: \u00a7ax" + event.getBonus() + " \u00a77if " + event.getBonusCondition()
                    + "% time remains");
        }

        lore.add("");
        lore.add("\u00a7eClick to view details & deliver!");

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(event.getName());
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "dist_request_id"),
                    PersistentDataType.STRING, request.getRequestId());
            if (request.getDeliveredCount() > 0 && !request.isCompleted()) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createEmptySlotItem() {
        List<String> lore = Arrays.asList(
                "\u00a78No request in this slot.",
                "\u00a78New requests appear periodically!");
        return createItem(Material.GRAY_STAINED_GLASS_PANE, "\u00a78\u00a7lEmpty Slot", lore);
    }

    private ItemStack createTaxInfoItem() {
        DistributionManager manager = plugin.getDistributionManager();
        double taxRate = plugin.getConfig().getDouble("distribution.tax_distribution_rate", 25.0);
        double totalTaxPaid = manager.getTotalTaxPaid(player.getUniqueId());

        List<String> lore = new ArrayList<>();
        lore.add("\u00a77Tax deducted from distribution earnings.");
        lore.add("");
        lore.add("\u00a7eCurrent Rate: \u00a7c" + String.format("%.1f", taxRate) + "%");
        lore.add("\u00a7eTotal Tax Paid: \u00a7c$" + String.format("%.2f", totalTaxPaid));
        lore.add("");
        lore.add("\u00a78Tax helps maintain the economy!");

        return createItem(Material.GOLD_NUGGET, "\u00a7e\u00a7lDistribution Tax", lore);
    }

    private ItemStack createStatsItem() {
        DistributionManager manager = plugin.getDistributionManager();

        List<String> lore = new ArrayList<>();
        lore.add("\u00a77Your distribution statistics.");
        lore.add("");
        lore.add("\u00a7eActive Requests: \u00a76" + manager.getActiveRequestCount(player.getUniqueId()));
        lore.add("\u00a7eTotal Companies: \u00a76" + manager.getTotalCompanyCount());
        lore.add("\u00a7eAvailable Events: \u00a76" + manager.getTotalEventCount());

        return createItem(Material.PAINTING, "\u00a7b\u00a7lStatistics", lore);
    }

    private ItemStack createCompanyInfoItem(Company company) {
        List<String> lore = new ArrayList<>();
        lore.add("\u00a77" + company.getDescription());
        lore.add("");
        lore.add("\u00a7eIndustry: \u00a7f" + company.getIndustry());
        lore.add("\u00a7eCEO: \u00a7f" + company.getCeo());
        lore.add("\u00a7eHQ: \u00a7f" + company.getHeadquarters());
        lore.add("");

        // Star rating
        StringBuilder stars = new StringBuilder("\u00a7eReputation: ");
        for (int i = 0; i < 5; i++) {
            stars.append(i < company.getReputation() ? "\u00a76\u2605" : "\u00a78\u2605");
        }
        lore.add(stars.toString());

        if (company.getLore() != null && !company.getLore().isEmpty()) {
            lore.add("");
            lore.addAll(company.getLore());
        }

        Material iconMat;
        try {
            iconMat = Material.valueOf(company.getIcon());
        } catch (IllegalArgumentException e) {
            iconMat = Material.CHEST;
        }

        return createItem(iconMat, company.getName(), lore);
    }

    private ItemStack createRequestOverviewItem(ActiveDistributionRequest request, DistributionEvent event) {
        List<String> lore = new ArrayList<>();
        lore.addAll(event.getLore());
        lore.add("");
        lore.add("\u00a7eDemanded Resources:");

        for (Map.Entry<String, Boolean> entry : request.getDeliveredResources().entrySet()) {
            String demandStr = entry.getKey();
            String actualId = demandStr;
            int amount = 1;
            if (demandStr.contains(" ")) {
                String[] parts = demandStr.split(" ");
                actualId = parts[0];
                try {
                    amount = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                }
            }
            ResourceItem resource = plugin.getResourceManager().getResource(actualId);
            String resName = resource != null ? resource.getName() : actualId;
            String status = entry.getValue() ? "\u00a7a\u2714 " : "\u00a7c\u2716 ";
            lore.add("  " + status + amount + "x " + resName);
        }

        lore.add("");
        lore.add("\u00a7eProgress: \u00a76" + request.getDeliveredCount() + "\u00a77/\u00a76"
                + request.getTotalDemand());

        return createItem(Material.PAPER, "\u00a7e\u00a7lRequest Details", lore);
    }

    private ItemStack createTimerItem(ActiveDistributionRequest request, DistributionEvent event) {
        List<String> lore = new ArrayList<>();

        if (request.isExpired()) {
            lore.add("\u00a7c\u00a7lThis request has expired!");
            lore.add("");
            lore.add("\u00a78The time limit was exceeded.");
            return createItem(Material.RED_DYE, "\u00a7c\u00a7lEXPIRED", lore);
        }

        long remaining = request.getRemainingTimeMillis();
        double percent = request.getTimeRemainingPercent();

        lore.add("\u00a7eTime Remaining: \u00a7b" + DistributionManager.formatTime(remaining));
        lore.add("\u00a7eTime Left: \u00a7b" + String.format("%.1f", percent) + "%");
        lore.add("");

        // Bonus eligibility
        if (percent >= event.getBonusCondition()) {
            lore.add("\u00a7a\u00a7lSpeed Bonus ACTIVE!");
            lore.add("\u00a77Complete now for x" + event.getBonus() + " reward!");
        } else {
            lore.add("\u00a77Speed Bonus requires " + event.getBonusCondition() + "% time remaining");
            lore.add("\u00a78(Currently " + String.format("%.1f", percent) + "%)");
        }

        Material timerMat = percent > 50 ? Material.LIME_DYE : (percent > 25 ? Material.YELLOW_DYE : Material.RED_DYE);
        return createItem(timerMat, "\u00a7e\u00a7lTimer", lore);
    }

    private ItemStack createRewardInfoItem(ActiveDistributionRequest request, DistributionEvent event) {
        double taxRate = plugin.getConfig().getDouble("distribution.tax_distribution_rate", 25.0);
        double baseReward = event.getPriceOffer();
        double taxAmount = baseReward * (taxRate / 100.0);
        double afterTax = baseReward - taxAmount;
        double bonusReward = baseReward * event.getBonus();
        double bonusTax = bonusReward * (taxRate / 100.0);
        double bonusAfterTax = bonusReward - bonusTax;

        List<String> lore = new ArrayList<>();
        lore.add("\u00a7eBase Payment: \u00a76$" + String.format("%.2f", baseReward));
        lore.add("\u00a7cTax (" + String.format("%.0f", taxRate) + "%): \u00a7c-$" + String.format("%.2f", taxAmount));
        lore.add("\u00a7aNet Payment: \u00a76$" + String.format("%.2f", afterTax));
        lore.add("");
        if (event.getBonus() > 1.0) {
            lore.add("\u00a7e\u00a7lWith Speed Bonus (x" + event.getBonus() + "):");
            lore.add("\u00a7eBoosted Payment: \u00a76$" + String.format("%.2f", bonusReward));
            lore.add("\u00a7cTax (" + String.format("%.0f", taxRate) + "%): \u00a7c-$"
                    + String.format("%.2f", bonusTax));
            lore.add("\u00a7a\u00a7lBoosted Net: \u00a76\u00a7l$" + String.format("%.2f", bonusAfterTax));
            lore.add("");
            lore.add("\u00a77Bonus requires " + event.getBonusCondition() + "% time remaining");
        }

        return createItem(Material.SUNFLOWER, "\u00a76\u00a7lReward Info", lore);
    }

    private ItemStack createDemandItem(ActiveDistributionRequest request, String demandStr, boolean delivered) {
        String actualId = demandStr;
        int amount = 1;
        if (demandStr.contains(" ")) {
            String[] parts = demandStr.split(" ");
            actualId = parts[0];
            try {
                amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
            }
        }

        ResourceItem resource = plugin.getResourceManager().getResource(actualId);
        String resName = resource != null ? resource.getName() : actualId;
        Material material = Material.BARRIER;
        if (resource != null) {
            try {
                material = Material.valueOf(resource.getMaterial());
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<String> lore = new ArrayList<>();

        if (delivered) {
            lore.add("");
            lore.add("\u00a7a\u00a7lDELIVERED!");
            lore.add("");
            lore.add("\u00a78This resource has been delivered.");

            ItemStack item = new ItemStack(Material.LIME_DYE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("\u00a7a\u2714 " + amount + "x " + resName + " \u00a7a(Delivered)");
                meta.setLore(lore);
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
            return item;

        }

        // Not delivered
        lore.add("");
        lore.add("\u00a7c\u00a7lNOT DELIVERED");
        lore.add("");
        lore.add("\u00a77You need to have this resource");
        lore.add("\u00a77in your inventory to deliver it.");
        lore.add("");
        lore.add("\u00a7e>>> Click to deliver! <<<");

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00a7c\u2716 " + amount + "x " + resName + " \u00a7c(Needed)");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "dist_demand_resource"),
                    PersistentDataType.STRING, demandStr);
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "dist_demand_request"),

                    PersistentDataType.STRING, request.getRequestId());
            item.setItemMeta(meta);
        }

        return item;
    }

    // ==================== UTILITY ====================

    private void fillBorder(Inventory inv) {
        Material borderMat = Material.matchMaterial(
                plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        if (borderMat == null)
            borderMat = Material.BLACK_STAINED_GLASS_PANE;
        ItemStack border = createItem(borderMat, " ", null);

        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9)
            inv.setItem(i, border);
        for (int i = 17; i < 45; i += 9)
            inv.setItem(i, border);
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
