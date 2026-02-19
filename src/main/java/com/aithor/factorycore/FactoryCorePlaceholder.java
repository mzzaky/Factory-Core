package com.aithor.factorycore;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import com.aithor.factorycore.models.Factory;
import com.aithor.factorycore.models.Invoice;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FactoryCorePlaceholder extends PlaceholderExpansion {
    
    private final FactoryCore plugin;
    
    public FactoryCorePlaceholder(FactoryCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    @NotNull
    public String getIdentifier() {
        return "factorycore";
    }
    
    @Override
    @NotNull
    public String getAuthor() {
        return "aithor";
    }
    
    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        // %factorycore_factories_owned%
        if (params.equals("factories_owned")) {
            List<Factory> factories = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());
            return String.valueOf(factories.size());
        }
        
        // %factorycore_factories_running%
        if (params.equals("factories_running")) {
            long running = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId())
                .stream()
                .filter(f -> f.getStatus() == com.aithor.factorycore.models.FactoryStatus.RUNNING)
                .count();
            return String.valueOf(running);
        }
        
        // %factorycore_total_value%
        if (params.equals("total_value")) {
            double total = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId())
                .stream()
                .mapToDouble(Factory::getPrice)
                .sum();
            return String.format("%.2f", total);
        }
        
        // %factorycore_invoices_pending%
        if (params.equals("invoices_pending")) {
            List<Invoice> invoices = plugin.getInvoiceManager().getInvoicesByOwner(player.getUniqueId());
            return String.valueOf(invoices.size());
        }
        
        // %factorycore_invoices_overdue%
        if (params.equals("invoices_overdue")) {
            long overdue = plugin.getInvoiceManager().getInvoicesByOwner(player.getUniqueId())
                .stream()
                .filter(Invoice::isOverdue)
                .count();
            return String.valueOf(overdue);
        }
        
        // %factorycore_total_invoice_amount%
        if (params.equals("total_invoice_amount")) {
            double total = plugin.getInvoiceManager().getInvoicesByOwner(player.getUniqueId())
                .stream()
                .mapToDouble(Invoice::getAmount)
                .sum();
            return String.format("%.2f", total);
        }
        
        // %factorycore_factory_<id>_status%
        if (params.startsWith("factory_") && params.endsWith("_status")) {
            String factoryId = params.replace("factory_", "").replace("_status", "");
            Factory factory = plugin.getFactoryManager().getFactory(factoryId);
            if (factory != null && factory.getOwner().equals(player.getUniqueId())) {
                return factory.getStatus().getDisplay();
            }
            return "N/A";
        }
        
        // %factorycore_factory_<id>_level%
        if (params.startsWith("factory_") && params.endsWith("_level")) {
            String factoryId = params.replace("factory_", "").replace("_level", "");
            Factory factory = plugin.getFactoryManager().getFactory(factoryId);
            if (factory != null && factory.getOwner().equals(player.getUniqueId())) {
                return String.valueOf(factory.getLevel());
            }
            return "0";
        }
        
        // %factorycore_factory_<id>_type%
        if (params.startsWith("factory_") && params.endsWith("_type")) {
            String factoryId = params.replace("factory_", "").replace("_type", "");
            Factory factory = plugin.getFactoryManager().getFactory(factoryId);
            if (factory != null && factory.getOwner().equals(player.getUniqueId())) {
                return factory.getType().getDisplayName();
            }
            return "N/A";
        }
        
        // %factorycore_factory_<id>_production%
        if (params.startsWith("factory_") && params.endsWith("_production")) {
            String factoryId = params.replace("factory_", "").replace("_production", "");
            Factory factory = plugin.getFactoryManager().getFactory(factoryId);
            if (factory != null && factory.getOwner().equals(player.getUniqueId())) {
                if (factory.getCurrentProduction() != null) {
                    com.aithor.factorycore.models.Recipe recipe = plugin.getRecipeManager()
                        .getRecipe(factory.getCurrentProduction().getRecipeId());
                    return recipe != null ? recipe.getName() : "Unknown";
                }
                return "None";
            }
            return "N/A";
        }
        
        // %factorycore_factory_<id>_progress%
        if (params.startsWith("factory_") && params.endsWith("_progress")) {
            String factoryId = params.replace("factory_", "").replace("_progress", "");
            Factory factory = plugin.getFactoryManager().getFactory(factoryId);
            if (factory != null && factory.getOwner().equals(player.getUniqueId())) {
                if (factory.getCurrentProduction() != null) {
                    int percent = (int) (factory.getCurrentProduction().getProgress() * 100);
                    return percent + "%";
                }
                return "0%";
            }
            return "N/A";
        }
        
        // %factorycore_highest_level%
        if (params.equals("highest_level")) {
            int highest = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId())
                .stream()
                .mapToInt(Factory::getLevel)
                .max()
                .orElse(0);
            return String.valueOf(highest);
        }
        
        // %factorycore_steel_mills_owned%
        if (params.equals("steel_mills_owned")) {
            long count = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId())
                .stream()
                .filter(f -> f.getType() == com.aithor.factorycore.models.FactoryType.STEEL_MILL)
                .count();
            return String.valueOf(count);
        }
        
        // %factorycore_refineries_owned%
        if (params.equals("refineries_owned")) {
            long count = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId())
                .stream()
                .filter(f -> f.getType() == com.aithor.factorycore.models.FactoryType.REFINERY)
                .count();
            return String.valueOf(count);
        }
        
        // %factorycore_workshops_owned%
        if (params.equals("workshops_owned")) {
            long count = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId())
                .stream()
                .filter(f -> f.getType() == com.aithor.factorycore.models.FactoryType.WORKSHOP)
                .count();
            return String.valueOf(count);
        }
        
        // %factorycore_advanced_factories_owned%
        if (params.equals("advanced_factories_owned")) {
            long count = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId())
                .stream()
                .filter(f -> f.getType() == com.aithor.factorycore.models.FactoryType.ADVANCED_FACTORY)
                .count();
            return String.valueOf(count);
        }
        
        return null;
    }
}