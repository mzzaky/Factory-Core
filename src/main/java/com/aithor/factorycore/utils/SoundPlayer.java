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

class SoundPlayer {
    
    public static void playSound(org.bukkit.entity.Player player, String soundName) {
        FactoryCore plugin = FactoryCore.getInstance();
        
        if (!plugin.getConfig().getBoolean("notifications.sound.enabled", true)) {
            return;
        }
        
        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + soundName);
        }
    }
    
    public static void playProductionComplete(org.bukkit.entity.Player player) {
        FactoryCore plugin = FactoryCore.getInstance();
        String sound = plugin.getConfig().getString("notifications.sound.production-complete", 
            "ENTITY_EXPERIENCE_ORB_PICKUP");
        playSound(player, sound);
    }
    
    public static void playInvoiceCreated(org.bukkit.entity.Player player) {
        FactoryCore plugin = FactoryCore.getInstance();
        String sound = plugin.getConfig().getString("notifications.sound.invoice-created", 
            "BLOCK_NOTE_BLOCK_PLING");
        playSound(player, sound);
    }
    
    public static void playLevelUp(org.bukkit.entity.Player player) {
        FactoryCore plugin = FactoryCore.getInstance();
        String sound = plugin.getConfig().getString("notifications.sound.level-up", 
            "ENTITY_PLAYER_LEVELUP");
        playSound(player, sound);
    }
}
