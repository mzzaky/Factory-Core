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

public class ResourceManager {
    private final FactoryCore plugin;
    private final Map<String, ResourceItem> resources;
    private FileConfiguration resourceConfig;
    private final File resourceFile;
    
    public ResourceManager(FactoryCore plugin) {
        this.plugin = plugin;
        this.resources = new HashMap<>();
        this.resourceFile = new File(plugin.getDataFolder(), "resources.yml");
        reload();
    }
    
    public void reload() {
        resources.clear();
        resourceConfig = YamlConfiguration.loadConfiguration(resourceFile);
        
        if (!resourceConfig.contains("resources")) return;
        
        for (String key : resourceConfig.getConfigurationSection("resources").getKeys(false)) {
            String path = "resources." + key;
            
            ResourceType type = ResourceType.valueOf(
                resourceConfig.getString(path + ".type", "RAW_RESOURCES")
            );
            
            String material = resourceConfig.getString(path + ".material", "STONE");
            
            ResourceItem item = new ResourceItem(key, type, material);
            item.setName(resourceConfig.getString(path + ".name", "§fUnnamed").replace("&", "§"));
            item.setLore(resourceConfig.getStringList(path + ".lore").stream()
                .map(s -> s.replace("&", "§"))
                .collect(java.util.stream.Collectors.toList()));
            item.setCustomModelData(resourceConfig.getInt(path + ".custom-model-data", 0));
            item.setGlow(resourceConfig.getBoolean(path + ".glow", false));
            item.setSellPrice(resourceConfig.getDouble(path + ".sell-price", 0.0));
            
            resources.put(key, item);
        }
        
        plugin.getLogger().info("Loaded " + resources.size() + " resources!");
    }
    
    public ResourceItem getResource(String id) {
        return resources.get(id);
    }
    
    public ItemStack createItemStack(String resourceId, int amount) {
        ResourceItem resource = resources.get(resourceId);
        if (resource == null) return null;
        
        Material material = Material.valueOf(resource.getMaterial());
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(resource.getName());
            meta.setLore(resource.getLore());
            
            if (resource.getCustomModelData() > 0) {
                meta.setCustomModelData(resource.getCustomModelData());
            }
            
            if (resource.isGlow()) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    public boolean giveResource(Player player, String resourceId, int amount) {
        ItemStack item = createItemStack(resourceId, amount);
        if (item == null) return false;
        
        player.getInventory().addItem(item);
        return true;
    }
    
    public String getResourceId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        String displayName = meta.getDisplayName();
        int customModelData = meta.hasCustomModelData() ? meta.getCustomModelData() : 0;

        for (Map.Entry<String, ResourceItem> entry : resources.entrySet()) {
            ResourceItem resource = entry.getValue();
            
            // Compare critical properties
            if (resource.getMaterial().equals(item.getType().name()) &&
                resource.getName().equals(displayName) &&
                resource.getCustomModelData() == customModelData) {
                return entry.getKey(); // Return the ID
            }
        }
        
        return null; // No match found
    }
    
    public Map<String, ResourceItem> getAllResources() {
        return new HashMap<>(resources);
    }
}