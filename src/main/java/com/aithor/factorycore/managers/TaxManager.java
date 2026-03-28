package com.aithor.factorycore.managers;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TaxManager - Dedicated tax system management
 * Handles tax calculations, payments, and tax history
 */
public class TaxManager {

    private final FactoryCore plugin;
    private final Map<String, TaxRecord> taxRecords; // factoryId -> TaxRecord
    private final Map<UUID, List<TaxPayment>> paymentHistory; // playerId -> payment history
    private final File dataFile;
    private long lastTaxCollection;

    public TaxManager(FactoryCore plugin) {
        this.plugin = plugin;
        this.taxRecords = new HashMap<>();
        this.paymentHistory = new HashMap<>();
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.dataFile = new File(dataFolder, "taxes.yml");
        this.lastTaxCollection = System.currentTimeMillis();
        loadTaxData();
    }

    private void loadTaxData() {
        if (!dataFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        // Load tax records
        if (config.contains("tax-records")) {
            for (String factoryId : config.getConfigurationSection("tax-records").getKeys(false)) {
                String path = "tax-records." + factoryId;
                TaxRecord record = new TaxRecord(
                        factoryId,
                        config.getDouble(path + ".amount-due", 0),
                        config.getLong(path + ".last-assessment", 0),
                        config.getLong(path + ".due-date", 0),
                        config.getBoolean(path + ".overdue", false));
                taxRecords.put(factoryId, record);
            }
        }

        // Load payment history
        if (config.contains("payment-history")) {
            for (String uuidStr : config.getConfigurationSection("payment-history").getKeys(false)) {
                UUID playerId = UUID.fromString(uuidStr);
                List<TaxPayment> payments = new ArrayList<>();

                for (String paymentId : config.getConfigurationSection("payment-history." + uuidStr).getKeys(false)) {
                    String path = "payment-history." + uuidStr + "." + paymentId;
                    payments.add(new TaxPayment(
                            paymentId,
                            config.getString(path + ".factory-id"),
                            config.getDouble(path + ".amount"),
                            config.getLong(path + ".timestamp")));
                }

                paymentHistory.put(playerId, payments);
            }
        }

        lastTaxCollection = config.getLong("last-collection", System.currentTimeMillis());

        plugin.getLogger().info("Loaded " + taxRecords.size() + " tax records!");
    }

    public void saveAll() {
        FileConfiguration config = new YamlConfiguration();

        // Save tax records
        for (Map.Entry<String, TaxRecord> entry : taxRecords.entrySet()) {
            String path = "tax-records." + entry.getKey();
            TaxRecord record = entry.getValue();
            config.set(path + ".amount-due", record.amountDue);
            config.set(path + ".last-assessment", record.lastAssessment);
            config.set(path + ".due-date", record.dueDate);
            config.set(path + ".overdue", record.overdue);
        }

        // Save payment history
        for (Map.Entry<UUID, List<TaxPayment>> entry : paymentHistory.entrySet()) {
            for (TaxPayment payment : entry.getValue()) {
                String path = "payment-history." + entry.getKey() + "." + payment.id;
                config.set(path + ".factory-id", payment.factoryId);
                config.set(path + ".amount", payment.amount);
                config.set(path + ".timestamp", payment.timestamp);
            }
        }

        config.set("last-collection", lastTaxCollection);

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save tax data!");
            e.printStackTrace();
        }
    }

    /**
     * Calculate tax for a specific factory
     */
    public double calculateTax(Factory factory) {
        if (factory == null || factory.getOwner() == null) {
            return 0;
        }

        double baseRate = plugin.getConfig().getDouble("tax.rate", 5.0) / 100.0;
        double levelMultiplier = plugin.getConfig().getDouble("tax.level-multiplier", 2.5) / 100.0;

        // Total rate = base rate + (level - 1) * level multiplier
        double totalRate = baseRate + (levelMultiplier * (factory.getLevel() - 1));

        double taxAmount = factory.getPrice() * totalRate;

        // Apply Fiscal Optimization research buff
        if (plugin.getResearchManager() != null) {
            double reduction = plugin.getResearchManager().getTaxReduction(factory.getOwner());
            if (reduction > 0) {
                taxAmount *= (1 - (reduction / 100.0));
            }
        }

        return taxAmount;
    }

    /**
     * Calculate tax for a specific player-created factory
     */
    public double calculateTax(PlayerFactory pf) {
        if (pf == null || pf.getOwner() == null) {
            return 0;
        }

        double baseRate = plugin.getConfig().getDouble("tax.rate", 5.0) / 100.0;
        double levelMultiplier = plugin.getConfig().getDouble("tax.level-multiplier", 2.5) / 100.0;

        // Total rate = base rate + (level - 1) * level multiplier
        double totalRate = baseRate + (levelMultiplier * (pf.getLevel() - 1));

        double taxAmount = pf.getPrice() * totalRate;

        // Apply Fiscal Optimization research buff
        if (plugin.getResearchManager() != null) {
            double reduction = plugin.getResearchManager().getTaxReduction(pf.getOwner());
            if (reduction > 0) {
                taxAmount *= (1 - (reduction / 100.0));
            }
        }

        return taxAmount;
    }

    /**
     * Assess taxes for all owned factories
     */
    public void assessTaxes() {
        // Assess admin factories
        for (Factory factory : plugin.getFactoryManager().getAllFactories()) {
            if (factory.getOwner() != null) {
                assessTaxForFactory(factory.getId());
            }
        }

        // Assess player factories
        if (plugin.getPlayerFactoryManager() != null) {
            for (PlayerFactory pf : plugin.getPlayerFactoryManager().getAllFactories()) {
                if (pf.getOwner() != null) {
                    assessTaxForFactory(pf.getId());
                }
            }
        }

        lastTaxCollection = System.currentTimeMillis();
        saveAll();
    }

    /**
     * Assess tax for a specific factory ID
     * @param factoryId ID of the factory (admin or player created)
     * @return true if successful, false if factory not found or has no owner
     */
    public boolean assessTaxForFactory(String factoryId) {
        long currentTime = System.currentTimeMillis();
        long taxDueDays = plugin.getConfig().getLong("tax.due-days", 7);
        long dueDate = currentTime + (taxDueDays * 24 * 60 * 60 * 1000);

        Factory adminFactory = plugin.getFactoryManager().getFactory(factoryId);
        PlayerFactory playerFactory = (plugin.getPlayerFactoryManager() != null)
                ? plugin.getPlayerFactoryManager().getFactory(factoryId)
                : null;

        if (adminFactory != null && adminFactory.getOwner() != null) {
            double taxAmount = calculateTax(adminFactory);
            applyTaxAssessment(adminFactory.getId(), adminFactory.getOwner(), taxAmount, currentTime, dueDate);
            return true;
        } else if (playerFactory != null && playerFactory.getOwner() != null) {
            double taxAmount = calculateTax(playerFactory);
            applyTaxAssessment(playerFactory.getId(), playerFactory.getOwner(), taxAmount, currentTime, dueDate);
            return true;
        }

        return false;
    }

    private void applyTaxAssessment(String factoryId, UUID ownerId, double taxAmount, long currentTime, long dueDate) {
        TaxRecord record = taxRecords.getOrDefault(factoryId,
                new TaxRecord(factoryId, 0, 0, 0, false));

        record.amountDue += taxAmount;
        record.lastAssessment = currentTime;
        record.dueDate = dueDate;

        taxRecords.put(factoryId, record);

        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null) {
            owner.sendMessage(plugin.getLanguageManager().getMessage("tax-assessed")
                    .replace("{factory}", factoryId)
                    .replace("{amount}", String.format("%.2f", taxAmount)));
        }
        saveAll();
    }

    /**
     * Check and mark overdue taxes
     */
    public void checkOverdueTaxes() {
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<String, TaxRecord> entry : taxRecords.entrySet()) {
            TaxRecord record = entry.getValue();
            if (record.amountDue > 0 && currentTime > record.dueDate) {
                record.overdue = true;

                double lateFeeRate = plugin.getConfig().getDouble("tax.late-fee-rate", 5.0) / 100.0;
                if (lateFeeRate > 0 && !record.lateFeeApplied) {
                    record.amountDue += record.amountDue * lateFeeRate;
                    record.lateFeeApplied = true;

                    // Resolve owner from either admin or player factory
                    UUID ownerId = null;
                    String factoryDisplayId = record.factoryId;
                    Factory factory = plugin.getFactoryManager().getFactory(record.factoryId);
                    if (factory != null && factory.getOwner() != null) {
                        ownerId = factory.getOwner();
                        factoryDisplayId = factory.getId();
                    } else if (plugin.getPlayerFactoryManager() != null) {
                        PlayerFactory pf = plugin.getPlayerFactoryManager().getFactory(record.factoryId);
                        if (pf != null && pf.getOwner() != null) {
                            ownerId = pf.getOwner();
                            factoryDisplayId = pf.getId();
                        }
                    }

                    if (ownerId != null) {
                        Player owner = Bukkit.getPlayer(ownerId);
                        if (owner != null) {
                            owner.sendMessage("§c§lTax Overdue! §7Late fee applied to factory §e" + factoryDisplayId);
                        }
                    }
                }
            }
        }

        saveAll();
    }

    /**
     * Pay tax for a specific factory
     */
    public boolean payTax(Player player, String factoryId) {
        TaxRecord record = taxRecords.get(factoryId);
        if (record == null || record.amountDue <= 0) {
            return false;
        }

        // Resolve owner from either admin factory or player factory
        UUID ownerId = null;
        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory != null) {
            ownerId = factory.getOwner();
        } else if (plugin.getPlayerFactoryManager() != null) {
            PlayerFactory pf = plugin.getPlayerFactoryManager().getFactory(factoryId);
            if (pf != null)
                ownerId = pf.getOwner();
        }

        if (ownerId == null || !ownerId.equals(player.getUniqueId())) {
            return false;
        }

        if (!plugin.getEconomy().has(player, record.amountDue)) {
            return false;
        }

        // Process payment
        double paidAmount = record.amountDue;
        plugin.getEconomy().withdrawPlayer(player, paidAmount);

        // Record payment in history
        TaxPayment payment = new TaxPayment(
                UUID.randomUUID().toString().substring(0, 8),
                factoryId,
                paidAmount,
                System.currentTimeMillis());

        paymentHistory.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(payment);

        // Clear tax record
        record.amountDue = 0;
        record.overdue = false;
        record.lateFeeApplied = false;

        saveAll();

        // Achievement: Tax Contributor - first tax payment
        // Achievement: Disciplined Businessman - cumulative tax paid
        if (plugin.getAchievementManager() != null) {
            plugin.getAchievementManager().awardAchievement(player, "tax_contributor");
            plugin.getAchievementManager().addProgress(player, "disciplined_businessman", paidAmount);
        }

        // Daily Quest: Tax Compliance Duty (TAX_PAYMENT)
        if (plugin.getDailyQuestManager() != null) {
            plugin.getDailyQuestManager().addProgressByType(player, "TAX_PAYMENT", 1);
        }

        return true;
    }

    /**
     * Pay all taxes for a player
     */
    public boolean payAllTaxes(Player player) {
        double totalDue = getTotalTaxDue(player.getUniqueId());
        if (totalDue <= 0) {
            return false;
        }

        if (!plugin.getEconomy().has(player, totalDue)) {
            return false;
        }

        // Pay admin factories
        for (Factory factory : plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId())) {
            TaxRecord record = taxRecords.get(factory.getId());
            if (record != null && record.amountDue > 0) {
                payTax(player, factory.getId());
            }
        }

        // Pay player factories
        if (plugin.getPlayerFactoryManager() != null) {
            for (PlayerFactory pf : plugin.getPlayerFactoryManager().getAllFactories()) {
                if (!pf.getOwner().equals(player.getUniqueId()))
                    continue;
                TaxRecord record = taxRecords.get(pf.getId());
                if (record != null && record.amountDue > 0) {
                    payTax(player, pf.getId());
                }
            }
        }

        return true;
    }

    /**
     * Get total tax due for a player
     */
    public double getTotalTaxDue(UUID playerId) {
        double total = 0;

        // Admin factories
        for (Factory factory : plugin.getFactoryManager().getFactoriesByOwner(playerId)) {
            TaxRecord record = taxRecords.get(factory.getId());
            if (record != null)
                total += record.amountDue;
        }

        // Player factories
        if (plugin.getPlayerFactoryManager() != null) {
            for (PlayerFactory pf : plugin.getPlayerFactoryManager().getAllFactories()) {
                if (!pf.getOwner().equals(playerId))
                    continue;
                TaxRecord record = taxRecords.get(pf.getId());
                if (record != null)
                    total += record.amountDue;
            }
        }

        return total;
    }

    /**
     * Get tax record for a factory
     */
    public TaxRecord getTaxRecord(String factoryId) {
        return taxRecords.get(factoryId);
    }

    /**
     * Get all tax records for a player's factories
     */
    public List<TaxRecord> getPlayerTaxRecords(UUID playerId) {
        List<TaxRecord> records = new ArrayList<>();

        // Admin factories
        for (Factory factory : plugin.getFactoryManager().getFactoriesByOwner(playerId)) {
            TaxRecord record = taxRecords.get(factory.getId());
            if (record != null && record.amountDue > 0)
                records.add(record);
        }

        // Player factories
        if (plugin.getPlayerFactoryManager() != null) {
            for (PlayerFactory pf : plugin.getPlayerFactoryManager().getAllFactories()) {
                if (!pf.getOwner().equals(playerId))
                    continue;
                TaxRecord record = taxRecords.get(pf.getId());
                if (record != null && record.amountDue > 0)
                    records.add(record);
            }
        }

        return records;
    }

    /**
     * Get payment history for a player
     */
    public List<TaxPayment> getPaymentHistory(UUID playerId) {
        return paymentHistory.getOrDefault(playerId, new ArrayList<>());
    }

    /**
     * Get overdue tax records for a player
     */
    public List<TaxRecord> getOverdueTaxes(UUID playerId) {
        return getPlayerTaxRecords(playerId).stream()
                .filter(r -> r.overdue)
                .collect(Collectors.toList());
    }

    /**
     * Get time until next tax collection
     */
    public long getTimeUntilNextCollection() {
        long interval = plugin.getConfig().getLong("tax.interval-ticks", 144000) * 50; // Convert ticks to ms
        return (lastTaxCollection + interval) - System.currentTimeMillis();
    }

    // Inner classes for tax data
    public static class TaxRecord {
        public final String factoryId;
        public double amountDue;
        public long lastAssessment;
        public long dueDate;
        public boolean overdue;
        public boolean lateFeeApplied;

        public TaxRecord(String factoryId, double amountDue, long lastAssessment, long dueDate, boolean overdue) {
            this.factoryId = factoryId;
            this.amountDue = amountDue;
            this.lastAssessment = lastAssessment;
            this.dueDate = dueDate;
            this.overdue = overdue;
            this.lateFeeApplied = false;
        }
    }

    public static class TaxPayment {
        public final String id;
        public final String factoryId;
        public final double amount;
        public final long timestamp;

        public TaxPayment(String id, String factoryId, double amount, long timestamp) {
            this.id = id;
            this.factoryId = factoryId;
            this.amount = amount;
            this.timestamp = timestamp;
        }
    }
}
