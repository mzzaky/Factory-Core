package com.aithor.factorycore.managers;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.hooks.MMOItemsHook;
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

        if (!resourceConfig.contains("resources"))
            return;

        int mmoItemCount = 0;

        for (String key : resourceConfig.getConfigurationSection("resources").getKeys(false)) {
            String path = "resources." + key;

            ResourceType type = ResourceType.valueOf(
                    resourceConfig.getString(path + ".type", "RAW_RESOURCES"));

            String material = resourceConfig.getString(path + ".material", "STONE");

            ResourceItem item = new ResourceItem(key, type, material);
            item.setName(resourceConfig.getString(path + ".name", "§fUnnamed").replace("&", "§"));
            item.setLore(resourceConfig.getStringList(path + ".lore").stream()
                    .map(s -> s.replace("&", "§"))
                    .collect(java.util.stream.Collectors.toList()));
            item.setCustomModelData(resourceConfig.getInt(path + ".custom-model-data", 0));
            item.setGlow(resourceConfig.getBoolean(path + ".glow", false));
            item.setSellPrice(resourceConfig.getDouble(path + ".suggested_price", 0.0));

            // MMOItems integration
            if (resourceConfig.contains(path + ".mmoitems-type") && resourceConfig.contains(path + ".mmoitems-id")) {
                item.setMMOItem(true);
                item.setMMOItemsType(resourceConfig.getString(path + ".mmoitems-type"));
                item.setMMOItemsId(resourceConfig.getString(path + ".mmoitems-id"));
                mmoItemCount++;
            }

            resources.put(key, item);
        }

        plugin.getLogger().info("Loaded " + resources.size() + " resources!" +
                (mmoItemCount > 0 ? " (" + mmoItemCount + " MMOItems linked)" : ""));
    }

    public ResourceItem getResource(String id) {
        return resources.get(id);
    }

    public ItemStack createItemStack(String resourceId, int amount) {
        ResourceItem resource = resources.get(resourceId);
        if (resource == null)
            return null;

        // If this resource is linked to an MMOItems item, use MMOItems API
        if (resource.isMMOItem()) {
            MMOItemsHook mmoHook = plugin.getMMOItemsHook();
            if (mmoHook != null && mmoHook.isEnabled()) {
                ItemStack mmoItem = mmoHook.getMMOItem(
                        resource.getMMOItemsType(),
                        resource.getMMOItemsId(),
                        amount
                );
                if (mmoItem != null) {
                    return mmoItem;
                }
                // Fallback to vanilla item if MMOItems item not found
                plugin.getLogger().warning("MMOItems item not found for resource '" + resourceId
                        + "', falling back to vanilla item.");
            } else {
                plugin.getLogger().warning("MMOItems not available for resource '" + resourceId
                        + "', falling back to vanilla item.");
            }
        }

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
        if (item == null)
            return false;

        player.getInventory().addItem(item);
        return true;
    }

    public String getResourceId(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;

        // Check MMOItems resources first
        MMOItemsHook mmoHook = plugin.getMMOItemsHook();
        if (mmoHook != null && mmoHook.isEnabled()) {
            for (Map.Entry<String, ResourceItem> entry : resources.entrySet()) {
                ResourceItem resource = entry.getValue();
                if (resource.isMMOItem()) {
                    if (mmoHook.isMMOItem(item, resource.getMMOItemsType(), resource.getMMOItemsId())) {
                        return entry.getKey();
                    }
                }
            }
        }

        // Then check vanilla resources
        ItemMeta meta = item.getItemMeta();
        String displayName = meta.getDisplayName();
        int customModelData = meta.hasCustomModelData() ? meta.getCustomModelData() : 0;

        for (Map.Entry<String, ResourceItem> entry : resources.entrySet()) {
            ResourceItem resource = entry.getValue();

            // Skip MMOItems resources (already checked above)
            if (resource.isMMOItem()) continue;

            // Compare critical properties
            if (resource.getMaterial().equals(item.getType().name()) &&
                    resource.getName().equals(displayName) &&
                    resource.getCustomModelData() == customModelData) {
                return entry.getKey(); // Return the ID
            }
        }

        return null; // No match found
    }

    public boolean takeResource(Player player, String resourceId, int amount) {
        int count = 0;

        // First count if player has enough
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                String id = getResourceId(item);
                if (resourceId.equals(id)) {
                    count += item.getAmount();
                }
            }
        }

        if (count < amount) {
            return false;
        }

        // Then actually take them
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null) {
                String id = getResourceId(item);
                if (resourceId.equals(id)) {
                    if (item.getAmount() <= remaining) {
                        remaining -= item.getAmount();
                        player.getInventory().setItem(i, null);
                    } else {
                        item.setAmount(item.getAmount() - remaining);
                        remaining = 0;
                    }

                    if (remaining <= 0)
                        break;
                }
            }
        }

        return true;
    }

    public Map<String, ResourceItem> getAllResources() {
        return new HashMap<>(resources);
    }
}