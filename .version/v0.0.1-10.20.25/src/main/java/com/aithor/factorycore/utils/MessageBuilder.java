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

class MessageBuilder {
    
    private final StringBuilder message;
    
    public MessageBuilder() {
        this.message = new StringBuilder();
    }
    
    public MessageBuilder append(String text) {
        message.append(text);
        return this;
    }
    
    public MessageBuilder appendLine(String text) {
        message.append(text).append("\n");
        return this;
    }
    
    public MessageBuilder color(String text) {
        message.append(text.replace("&", "§"));
        return this;
    }
    
    public String build() {
        return message.toString();
    }
    
    public String[] buildArray() {
        return message.toString().split("\n");
    }
}
