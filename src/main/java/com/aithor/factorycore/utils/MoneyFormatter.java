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

class MoneyFormatter {
    
    public static String format(double amount) {
        FactoryCore plugin = FactoryCore.getInstance();
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        int decimals = plugin.getConfig().getInt("economy.decimal-places", 2);
        
        return symbol + String.format("%." + decimals + "f", amount);
    }
    
    public static String formatWithCommas(double amount) {
        FactoryCore plugin = FactoryCore.getInstance();
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        
        return symbol + String.format("%,.2f", amount);
    }
}