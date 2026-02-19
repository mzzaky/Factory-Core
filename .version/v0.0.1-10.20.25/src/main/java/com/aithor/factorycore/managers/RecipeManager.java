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

public class RecipeManager {
    private final FactoryCore plugin;
    private final Map<String, Recipe> recipes;
    private FileConfiguration recipeConfig;
    private final File recipeFile;
    
    public RecipeManager(FactoryCore plugin) {
        this.plugin = plugin;
        this.recipes = new HashMap<>();
        this.recipeFile = new File(plugin.getDataFolder(), "recipes.yml");
        reload();
    }
    
    public void reload() {
        plugin.getLogger().info("=== RECIPE MANAGER RELOAD DEBUG ===");
        plugin.getLogger().info("Recipe file exists: " + recipeFile.exists());
        plugin.getLogger().info("Recipe file path: " + recipeFile.getAbsolutePath());

        recipes.clear();
        recipeConfig = YamlConfiguration.loadConfiguration(recipeFile);

        if (!recipeConfig.contains("recipes")) {
            plugin.getLogger().warning("No 'recipes' section found in recipes.yml!");
            return;
        }

        Set<String> recipeKeys = recipeConfig.getConfigurationSection("recipes").getKeys(false);
        plugin.getLogger().info("Found recipe keys in config: " + recipeKeys);

        if (recipeKeys.isEmpty()) {
            plugin.getLogger().warning("No recipes defined in recipes.yml!");
            return;
        }

        plugin.getLogger().info("Loading " + recipeKeys.size() + " recipes from recipes.yml...");

        for (String key : recipeKeys) {
            String path = "recipes." + key;
            
            String name = recipeConfig.getString(path + ".name", key);
            String factoryType = recipeConfig.getString(path + ".factory-type");
            int productionTime = recipeConfig.getInt(path + ".production-time", 60);

            // Validate required fields
            if (factoryType == null || factoryType.isEmpty()) {
                plugin.getLogger().warning("Recipe " + key + " missing factory-type, skipping...");
                continue;
            }

            Recipe recipe = new Recipe(key, name, factoryType, productionTime);
            
            // Load inputs
            if (recipeConfig.contains(path + ".inputs")) {
                for (String inputKey : recipeConfig.getConfigurationSection(path + ".inputs").getKeys(false)) {
                    int amount = recipeConfig.getInt(path + ".inputs." + inputKey);
                    recipe.addInput(inputKey, amount);
                }
            }
            
            // Load outputs
            if (recipeConfig.contains(path + ".outputs")) {
                for (String outputKey : recipeConfig.getConfigurationSection(path + ".outputs").getKeys(false)) {
                    int amount = recipeConfig.getInt(path + ".outputs." + outputKey);
                    recipe.addOutput(outputKey, amount);
                }
            }
            
            // Load console commands
            List<String> commands = recipeConfig.getStringList(path + ".console-commands");
            commands.forEach(recipe::addConsoleCommand);
            
            // Load icon
            recipe.setIcon(recipeConfig.getString(path + ".icon", "STONE"));
            
            recipes.put(key, recipe);
        }
        
        plugin.getLogger().info("Loaded " + recipes.size() + " recipes!");
        if (recipes.isEmpty()) {
            plugin.getLogger().warning("No recipes were loaded! Check recipes.yml configuration.");
        } else {
            plugin.getLogger().info("Loaded recipes: " + recipes.keySet());
            plugin.getLogger().info("=== RECIPE RELOAD SUMMARY ===");
            for (Recipe recipe : recipes.values()) {
                plugin.getLogger().info("Recipe: " + recipe.getName() + " | Type: " + recipe.getFactoryType() + " | Time: " + recipe.getProductionTime());
            }
        }
    }
    
    public Recipe getRecipe(String id) {
        return recipes.get(id);
    }
    
    public List<Recipe> getRecipesByFactoryType(String factoryType) {
        List<Recipe> result = new ArrayList<>();
        for (Recipe recipe : recipes.values()) {
            if (recipe.getFactoryType().equals(factoryType)) {
                result.add(recipe);
            }
        }

        // Debug logging
        plugin.getLogger().info("Found " + result.size() + " recipes for factory type: " + factoryType);
        if (result.isEmpty()) {
            plugin.getLogger().warning("No recipes found for factory type: " + factoryType + ". Available types: " +
                recipes.values().stream().map(Recipe::getFactoryType).distinct().toList());
        }

        return result;
    }
    
    public Map<String, Recipe> getAllRecipes() {
        return new HashMap<>(recipes);
    }
}