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

class PermissionChecker {

    public static boolean hasPermission(org.bukkit.entity.Player player, String permission) {
        return player.hasPermission(permission) || player.hasPermission("factorycore.*");
    }

    public static boolean isAdmin(org.bukkit.entity.Player player) {
        return player.hasPermission("factorycore.admin") || player.hasPermission("factorycore.*");
    }
}
