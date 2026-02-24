package com.aithor.factorycore.managers;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class InvoiceManager {
    private final FactoryCore plugin;
    private final Map<String, Invoice> invoices;
    private final File dataFile;

    public InvoiceManager(FactoryCore plugin) {
        this.plugin = plugin;
        this.invoices = new HashMap<>();
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.dataFile = new File(dataFolder, "invoices.yml");
        loadInvoices();
    }

    private void loadInvoices() {
        if (!dataFile.exists())
            return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        for (String key : config.getKeys(false)) {
            try {
                Invoice invoice = new Invoice(
                        key,
                        UUID.fromString(config.getString(key + ".factory-id")),
                        InvoiceType.valueOf(config.getString(key + ".type")),
                        config.getDouble(key + ".amount"),
                        config.getLong(key + ".due-date"));
                invoice.setPaid(config.getBoolean(key + ".paid", false));
                invoices.put(key, invoice);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load invoice: " + key);
            }
        }
    }

    public void saveAll() {
        FileConfiguration config = new YamlConfiguration();

        for (Invoice invoice : invoices.values()) {
            String path = invoice.getId();
            config.set(path + ".factory-id", invoice.getFactoryId().toString());
            config.set(path + ".type", invoice.getType().name());
            config.set(path + ".amount", invoice.getAmount());
            config.set(path + ".due-date", invoice.getDueDate());
            config.set(path + ".paid", invoice.isPaid());
        }

        try {
            config.save(dataFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save invoices!");
            e.printStackTrace();
        }
    }

    public void generateTaxInvoices() {
        for (Factory factory : plugin.getFactoryManager().getAllFactories()) {
            if (factory.getOwner() == null)
                continue;

            double taxRate = plugin.getConfig().getDouble("tax.rate", 5.0) / 100.0;
            double levelMultiplier = plugin.getConfig().getDouble("tax.level-multiplier", 2.5) / 100.0;
            double totalRate = taxRate + (levelMultiplier * (factory.getLevel() - 1));

            double amount = factory.getPrice() * totalRate;

            long dueDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000); // 7 days

            String invoiceId = "tax_" + factory.getId() + "_" + System.currentTimeMillis();
            Invoice invoice = new Invoice(invoiceId, factory.getOwner(), InvoiceType.TAX, amount, dueDate);
            invoices.put(invoiceId, invoice);

            Player owner = Bukkit.getPlayer(factory.getOwner());
            if (owner != null) {
                owner.sendMessage(plugin.getLanguageManager().getMessage("tax-generated")
                        .replace("{amount}", String.format("%.2f", amount)));
            }
        }

        saveAll();
    }

    public void generateSalaryInvoices() {
        for (Factory factory : plugin.getFactoryManager().getAllFactories()) {
            if (factory.getOwner() == null)
                continue;

            double salaryRate = plugin.getConfig().getDouble("salary.rate", 1.0) / 100.0;
            double amount = factory.getPrice() * salaryRate;

            // Apply AI Workforce Integration research buff
            if (plugin.getResearchManager() != null) {
                double reduction = plugin.getResearchManager().getSalaryReduction(factory.getOwner());
                if (reduction > 0) {
                    amount *= (1 - (reduction / 100.0));
                }
            }

            long dueDate = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000); // 3 days

            String invoiceId = "salary_" + factory.getId() + "_" + System.currentTimeMillis();
            Invoice invoice = new Invoice(invoiceId, factory.getOwner(), InvoiceType.SALARY, amount, dueDate);
            invoices.put(invoiceId, invoice);

            Player owner = Bukkit.getPlayer(factory.getOwner());
            if (owner != null) {
                owner.sendMessage(plugin.getLanguageManager().getMessage("salary-generated")
                        .replace("{amount}", String.format("%.2f", amount)));
            }
        }

        saveAll();
    }

    public boolean payInvoice(Player player, String invoiceId) {
        Invoice invoice = invoices.get(invoiceId);
        if (invoice == null || invoice.isPaid())
            return false;

        if (!plugin.getEconomy().has(player, invoice.getAmount()))
            return false;

        plugin.getEconomy().withdrawPlayer(player, invoice.getAmount());
        invoice.setPaid(true);
        saveAll();

        // Achievement: Big Spender - cumulative salary payments
        if (plugin.getAchievementManager() != null && invoice.getType() == InvoiceType.SALARY) {
            plugin.getAchievementManager().addProgress(player, "big_spender", invoice.getAmount());
        }

        return true;
    }

    public List<Invoice> getInvoicesByOwner(UUID owner) {
        List<Invoice> result = new ArrayList<>();
        for (Invoice invoice : invoices.values()) {
            if (invoice.getFactoryId().equals(owner) && !invoice.isPaid()) {
                result.add(invoice);
            }
        }
        return result;
    }

    public Invoice getInvoice(String id) {
        return invoices.get(id);
    }
}