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
     * Assess taxes for all owned factories
     */
    public void assessTaxes() {
        long currentTime = System.currentTimeMillis();
        long taxDueDays = plugin.getConfig().getLong("tax.due-days", 7);
        long dueDate = currentTime + (taxDueDays * 24 * 60 * 60 * 1000);

        for (Factory factory : plugin.getFactoryManager().getAllFactories()) {
            if (factory.getOwner() == null)
                continue;

            double taxAmount = calculateTax(factory);

            TaxRecord record = taxRecords.getOrDefault(factory.getId(),
                    new TaxRecord(factory.getId(), 0, 0, 0, false));

            // Add new tax to existing due amount
            record.amountDue += taxAmount;
            record.lastAssessment = currentTime;
            record.dueDate = dueDate;

            taxRecords.put(factory.getId(), record);

            // Notify owner
            Player owner = Bukkit.getPlayer(factory.getOwner());
            if (owner != null) {
                owner.sendMessage(plugin.getLanguageManager().getMessage("tax-assessed")
                        .replace("{factory}", factory.getId())
                        .replace("{amount}", String.format("%.2f", taxAmount)));
            }
        }

        lastTaxCollection = currentTime;
        saveAll();
    }

    /**
     * Check and mark overdue taxes
     */
    public void checkOverdueTaxes() {
        long currentTime = System.currentTimeMillis();

        for (TaxRecord record : taxRecords.values()) {
            if (record.amountDue > 0 && currentTime > record.dueDate) {
                record.overdue = true;

                // Apply late fee if configured
                double lateFeeRate = plugin.getConfig().getDouble("tax.late-fee-rate", 5.0) / 100.0;
                if (lateFeeRate > 0 && !record.lateFeeApplied) {
                    record.amountDue += record.amountDue * lateFeeRate;
                    record.lateFeeApplied = true;

                    Factory factory = plugin.getFactoryManager().getFactory(record.factoryId);
                    if (factory != null && factory.getOwner() != null) {
                        Player owner = Bukkit.getPlayer(factory.getOwner());
                        if (owner != null) {
                            owner.sendMessage("§c§lTax Overdue! §7Late fee applied to factory §e" + factory.getId());
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

        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory == null || !factory.getOwner().equals(player.getUniqueId())) {
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

        List<Factory> playerFactories = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());
        for (Factory factory : playerFactories) {
            TaxRecord record = taxRecords.get(factory.getId());
            if (record != null && record.amountDue > 0) {
                payTax(player, factory.getId());
            }
        }

        return true;
    }

    /**
     * Get total tax due for a player
     */
    public double getTotalTaxDue(UUID playerId) {
        List<Factory> playerFactories = plugin.getFactoryManager().getFactoriesByOwner(playerId);
        double total = 0;

        for (Factory factory : playerFactories) {
            TaxRecord record = taxRecords.get(factory.getId());
            if (record != null) {
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
        List<Factory> playerFactories = plugin.getFactoryManager().getFactoriesByOwner(playerId);
        List<TaxRecord> records = new ArrayList<>();

        for (Factory factory : playerFactories) {
            TaxRecord record = taxRecords.get(factory.getId());
            if (record != null && record.amountDue > 0) {
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
