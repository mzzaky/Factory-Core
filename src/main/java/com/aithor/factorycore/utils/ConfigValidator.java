package com.aithor.factorycore.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.aithor.factorycore.FactoryCore;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

class ConfigValidator {
    
    private final FactoryCore plugin;
    
    public ConfigValidator(FactoryCore plugin) {
        this.plugin = plugin;
    }
    
    public boolean validate() {
        boolean valid = true;
        
        // Validate tax settings
        if (plugin.getConfig().getDouble("tax.rate") < 0) {
            plugin.getLogger().warning("Invalid tax.rate in config.yml (must be >= 0)");
            valid = false;
        }
        
        // Validate salary settings
        if (plugin.getConfig().getDouble("salary.rate") < 0) {
            plugin.getLogger().warning("Invalid salary.rate in config.yml (must be >= 0)");
            valid = false;
        }
        
        // Validate factory settings
        if (plugin.getConfig().getInt("factory.max-level") < 1) {
            plugin.getLogger().warning("Invalid factory.max-level in config.yml (must be >= 1)");
            valid = false;
        }
        
        // Validate factory types
        String[] types = {"steel_mill", "refinery", "workshop", "advanced_factory"};
        for (String type : types) {
            String path = "factory-types." + type;
            if (!plugin.getConfig().contains(path)) {
                plugin.getLogger().warning("Missing factory type: " + type);
                valid = false;
            }
        }
        
        return valid;
    }
}
