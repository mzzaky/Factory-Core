package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class RecipeSelectionGUI {

    private final FactoryCore plugin;
    private final Player player;
    private final String factoryId;

    public RecipeSelectionGUI(FactoryCore plugin, Player player, String factoryId) {
        this.plugin = plugin;
        this.player = player;
        this.factoryId = factoryId;
    }

    public void openRecipeMenu() {
        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory == null)
            return;

        // Store factory ID in player's persistent data for click handling
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_factory_id"),
                PersistentDataType.STRING, factoryId);

        List<Recipe> recipes = plugin.getRecipeManager()
                .getRecipesByFactoryType(factory.getType().getId());

        if (recipes.isEmpty()) {
            player.sendMessage("§cError: No recipes found for factory type: " + factory.getType().getId());
            plugin.getLogger().warning("No recipes loaded for factory type: " + factory.getType().getId() +
                    ". Available recipes: " + plugin.getRecipeManager().getAllRecipes().keySet());
            plugin.getLogger().warning("Factory type: " + factory.getType().getId() +
                    ", Factory ID: " + factoryId);
            return;
        }

        int size = ((recipes.size() + 8) / 9) * 9;
        if (size > 54)
            size = 54;
        if (size < 27)
            size = 27;

        Inventory inv = Bukkit.createInventory(null, size, "§6§lSelect Production Recipe");

        int slot = 0;
        for (Recipe recipe : recipes) {
            if (slot >= size - 9)
                break;

            List<String> lore = new ArrayList<>();
            lore.add("§7Production time: §e" + recipe.getProductionTime() + "s");
            lore.add("");
            lore.add("§7Required Inputs:");

            for (Map.Entry<String, Integer> input : recipe.getInputs().entrySet()) {
                ResourceItem resource = plugin.getResourceManager().getResource(input.getKey());
                String name = resource != null ? resource.getName() : input.getKey();
                int currentStock = plugin.getStorageManager().getInputAmount(factoryId, input.getKey());
                String stockColor = currentStock >= input.getValue() ? "§a" : "§c";
                lore.add("  §e" + input.getValue() + "x " + name + " §7(Owned: " + stockColor + currentStock + "§7)");
            }

            lore.add("");
            lore.add("§7Generated Outputs:");

            for (Map.Entry<String, Integer> output : recipe.getOutputs().entrySet()) {
                ResourceItem resource = plugin.getResourceManager().getResource(output.getKey());
                String name = resource != null ? resource.getName() : output.getKey();
                lore.add("  §a" + output.getValue() + "x " + name);
            }

            lore.add("");
            lore.add("§eClick to confirm");

            Material material;
            try {
                material = Material.valueOf(recipe.getIcon());
            } catch (IllegalArgumentException e) {
                // Fallback to a default material if the icon is invalid
                material = Material.STONE;
                plugin.getLogger().warning("Invalid material for recipe " + recipe.getId() + ": " + recipe.getIcon()
                        + ". Using STONE as fallback.");
            }
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(recipe.getName());
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "recipe_id"), PersistentDataType.STRING,
                        recipe.getId());
                item.setItemMeta(meta);

                // Debug logging for recipe item creation
                if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                    plugin.getLogger().info("Created recipe item: " + recipe.getName() + " with ID: " + recipe.getId());
                }
            } else {
                if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                    plugin.getLogger().warning("Could not get ItemMeta for recipe: " + recipe.getName());
                }
            }

            inv.setItem(slot++, item);
        }

        // Back button
        Material backMat = Material.matchMaterial(plugin.getConfig().getString("gui.back-item", "ARROW"));
        inv.setItem(size - 1, createItem(backMat != null ? backMat : Material.ARROW, "§e§lBack", null));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (name != null)
                meta.setDisplayName(name);
            if (lore != null)
                meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }
}