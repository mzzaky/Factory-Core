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

// ==================== Logger.java ====================
public class Logger {
    
    private static File logFile;
    private static boolean enabled = true;
    
    public static void init(FactoryCore plugin) {
        enabled = plugin.getConfig().getBoolean("logging.enabled", true);
        
        if (!enabled) return;
        
        File logDir = new File(plugin.getDataFolder(), "logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        
        String fileName = plugin.getConfig().getString("logging.file-name", "factorycore.log");
        logFile = new File(logDir, fileName);
        
        // Check file size and rotate if needed
        long maxSize = plugin.getConfig().getLong("logging.max-file-size", 10485760); // 10MB
        if (logFile.exists() && logFile.length() > maxSize) {
            rotateLog();
        }
    }
    
    private static void rotateLog() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String timestamp = sdf.format(new Date());
        File backup = new File(logFile.getParent(), "factorycore_" + timestamp + ".log");
        logFile.renameTo(backup);
    }
    
    public static void log(String message) {
        if (!enabled) return;
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(new Date());
        String logMessage = "[" + timestamp + "] " + message;
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(logMessage);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void logAdminCommand(String admin, String command) {
        FactoryCore plugin = FactoryCore.getInstance();
        if (plugin.getConfig().getBoolean("logging.log-admin-commands", true)) {
            log("[ADMIN] " + admin + " executed: " + command);
        }
    }
    
    public static void logTransaction(String player, String type, double amount) {
        FactoryCore plugin = FactoryCore.getInstance();
        if (plugin.getConfig().getBoolean("logging.log-transactions", true)) {
            log("[TRANSACTION] Player: " + player + " | Type: " + type + " | Amount: $" + amount);
        }
    }
    
    public static void logProduction(String factoryId, String recipe, String status) {
        FactoryCore plugin = FactoryCore.getInstance();
        if (plugin.getConfig().getBoolean("logging.log-production", true)) {
            log("[PRODUCTION] Factory: " + factoryId + " | Recipe: " + recipe + " | Status: " + status);
        }
    }
    
    public static void logInvoice(String type, String factoryId, double amount) {
        FactoryCore plugin = FactoryCore.getInstance();
        if (plugin.getConfig().getBoolean("logging.log-invoices", true)) {
            log("[INVOICE] Type: " + type + " | Factory: " + factoryId + " | Amount: $" + amount);
        }
    }
}