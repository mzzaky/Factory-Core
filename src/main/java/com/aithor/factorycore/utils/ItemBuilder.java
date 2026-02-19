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

class ItemBuilder {
    
    private final org.bukkit.inventory.ItemStack item;
    
    public ItemBuilder(org.bukkit.Material material) {
        this.item = new org.bukkit.inventory.ItemStack(material);
    }
    
    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }
    
    public ItemBuilder name(String name) {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name.replace("&", "§"));
            item.setItemMeta(meta);
        }
        return this;
    }
    
    public ItemBuilder lore(String... lore) {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            java.util.List<String> loreList = new java.util.ArrayList<>();
            for (String line : lore) {
                loreList.add(line.replace("&", "§"));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return this;
    }
    
    public ItemBuilder lore(java.util.List<String> lore) {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            java.util.List<String> coloredLore = new java.util.ArrayList<>();
            for (String line : lore) {
                coloredLore.add(line.replace("&", "§"));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return this;
    }
    
    public ItemBuilder glow() {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return this;
    }
    
    public ItemBuilder customModelData(int data) {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(data);
            item.setItemMeta(meta);
        }
        return this;
    }
    
    public org.bukkit.inventory.ItemStack build() {
        return item;
    }
}
