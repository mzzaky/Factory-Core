package com.aithor.factorycore.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;

public class WorldGuardUtils {
    
    public static ProtectedRegion getRegion(String regionName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        // Check world-specific regions
        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = container.get(BukkitAdapter.adapt(world));
            if (manager != null) {
                ProtectedRegion region = manager.getRegion(regionName);
                if (region != null) {
                    return region;
                }
            }
        }

        return null;
    }
    
    public static boolean regionExists(String regionName) {
        return getRegion(regionName) != null;
    }

    public static Set<String> getAllRegionNames() {
        Set<String> regionNames = new HashSet<>();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        // Add world-specific regions first
        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = container.get(BukkitAdapter.adapt(world));
            if (manager != null) {
                regionNames.addAll(manager.getRegions().keySet());
            }
        }

        return regionNames;
    }
}