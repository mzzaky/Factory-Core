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

// ==================== LanguageManager.java ====================
public class LanguageManager {
    private final FactoryCore plugin;
    private FileConfiguration langConfig;
    private final File langFile;
    
    public LanguageManager(FactoryCore plugin) {
        this.plugin = plugin;
        this.langFile = new File(plugin.getDataFolder(), "language.yml");
        reload();
    }
    
    public void reload() {
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }
    
    public String getMessage(String path) {
        String msg = langConfig.getString("messages." + path);
        if (msg == null) {
            return "§cMessage not found: " + path;
        }
        
        String prefix = langConfig.getString("messages.prefix", "§8[§6FactoryCore§8]§r");
        return msg.replace("{prefix}", prefix).replace("&", "§");
    }
    
    public List<String> getMessageList(String path) {
        List<String> list = langConfig.getStringList("messages." + path);
        String prefix = langConfig.getString("messages.prefix", "§8[§6FactoryCore§8]§r");
        
        list.replaceAll(s -> s.replace("{prefix}", prefix).replace("&", "§"));
        return list;
    }
}