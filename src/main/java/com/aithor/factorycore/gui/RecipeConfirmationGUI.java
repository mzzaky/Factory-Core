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

public class RecipeConfirmationGUI {

    private final FactoryCore plugin;
    private final Player player;
    private final String factoryId;
    private String recipeId;

    public RecipeConfirmationGUI(FactoryCore plugin, Player player, String factoryId) {
        this.plugin = plugin;
        this.player = player;
        this.factoryId = factoryId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public void openRecipeConfirm(String recipeId) {
        this.recipeId = recipeId;

        // Validate recipe exists first
        Recipe recipe = plugin.getRecipeManager().getRecipe(recipeId);
        if (recipe == null) {
            player.sendMessage("§cError: Recipe not found!");
            return;
        }

        // Validate recipe data
        if (recipe.getInputs().isEmpty() && recipe.getOutputs().isEmpty()) {
            player.sendMessage("§cError: Recipe does not have valid inputs or outputs!");
            return;
        }

        // Store recipe ID and factory ID in player's persistent data for click handling
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_recipe_id"),
                PersistentDataType.STRING, recipeId);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_factory_id"),
                PersistentDataType.STRING, factoryId);

        Inventory inv = Bukkit.createInventory(null, 27,
                "§6§lConfirm: §e" + recipe.getName());

        // Fill with border
        Material borderMat = Material
                .matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        // Recipe info
        List<String> lore = new ArrayList<>();
        lore.add("§7Production time: §e" + recipe.getProductionTime() + "s");

        // Money cost line
        if (recipe.getMoneyCost() > 0) {
            double balance = plugin.getEconomy().getBalance(player);
            String costColor = balance >= recipe.getMoneyCost() ? "§a" : "§c";
            lore.add("§7Money cost: " + costColor + "$" + String.format("%.2f", recipe.getMoneyCost()));
        }

        lore.add("");
        lore.add("§7Input:");
        for (Map.Entry<String, Integer> input : recipe.getInputs().entrySet()) {
            int available = plugin.getStorageManager().getInputAmount(factoryId, input.getKey());
            ResourceItem resource = plugin.getResourceManager().getResource(input.getKey());
            String name = resource != null ? resource.getName() : input.getKey();

            String color = available >= input.getValue() ? "§a" : "§c";
            lore.add("  " + color + input.getValue() + "x " + name + " §7(" + available + " available)");
        }

        Material confirmMaterial;
        try {
            confirmMaterial = Material.valueOf(recipe.getIcon());
        } catch (IllegalArgumentException e) {
            confirmMaterial = Material.STONE;
            plugin.getLogger().warning("Invalid material for recipe confirmation " + recipe.getId() + ": "
                    + recipe.getIcon() + ". Using STONE as fallback.");
        }
        inv.setItem(13, createItem(confirmMaterial, recipe.getName(), lore));

        // Confirm button
        Material confirmMat = Material.matchMaterial(plugin.getConfig().getString("gui.confirm-item", "GREEN_WOOL"));
        inv.setItem(11, createItem(confirmMat != null ? confirmMat : Material.GREEN_WOOL, "§a§lConfirm",
                Arrays.asList("§7Start production")));

        // Cancel button
        Material cancelMat = Material.matchMaterial(plugin.getConfig().getString("gui.cancel-item", "RED_WOOL"));
        inv.setItem(15, createItem(cancelMat != null ? cancelMat : Material.RED_WOOL, "§c§lCancel",
                Arrays.asList("§7Back to recipe menu")));

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