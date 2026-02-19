package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.*;

public class FactoryGUI {

    private final FactoryCore plugin;
    private final Player player;
    private String currentFactoryId;
    private String currentRecipeId;

    public FactoryGUI(FactoryCore plugin, Player player, String factoryId) {
        this.plugin = plugin;
        this.player = player;
        this.currentFactoryId = factoryId;
    }

    public void setCurrentRecipeId(String recipeId) {
        this.currentRecipeId = recipeId;
    }
    
    // ==================== MAIN FACTORY GUI ====================
    public void openMainMenu() {
        Factory factory = plugin.getFactoryManager().getFactory(currentFactoryId);
        if (factory == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return;
        }

        if (!player.getUniqueId().equals(factory.getOwner())) {
            player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-owned"));
            return;
        }

        // Additional validation
        if (factory.getType() == null) {
            player.sendMessage("§cError: Invalid Factory type!");
            return;
        }

        // Store factory ID in player's persistent data for click handling
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_factory_id"), PersistentDataType.STRING, currentFactoryId);

        Inventory inv = Bukkit.createInventory(null, 27,
            "§6§lFactory: §e" + factory.getType().getDisplayName());

        // Fill with border
        Material borderMat = Material.matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        // Factory Status (Slot 4)
        inv.setItem(4, createFactoryStatusItem(factory));

        // Start Production (Slot 10)
        inv.setItem(10, createItem(Material.CRAFTING_TABLE,
            "§a§lStart Production",
            Arrays.asList(
                "§7Click to start production",
                "§7and select a recipe"
            )
        ));

        // Invoice Management (Slot 12)
        int invoiceCount = plugin.getInvoiceManager()
            .getInvoicesByOwner(player.getUniqueId()).size();
        inv.setItem(12, createItem(Material.PAPER,
            "§e§lManage Invoices",
            Arrays.asList(
                "§7View and pay invoices",
                "§7that are pending",
                "",
                "§eActive Invoices: §6" + invoiceCount
            )
        ));

        // Factory Storage (Slot 14)
        int slots = plugin.getConfig().getInt("factory.base-storage-slots", 9) +
                   (factory.getLevel() - 1) * plugin.getConfig().getInt("factory.slots-per-level", 9);
        inv.setItem(14, createItem(Material.CHEST,
            "§b§lFactory Storage",
            Arrays.asList(
                "§7Access factory storage",
                "§7Slots: §e" + slots
            )
        ));

        // Upgrade Factory (Slot 16)
        inv.setItem(16, createItem(Material.NETHER_STAR,
            "§d§lUpgrade Factory",
            Arrays.asList(
                "§7Current level: §e" + factory.getLevel(),
                "§7Click to upgrade"
            )
        ));

        // Fast Travel (Slot 22)
        inv.setItem(22, createItem(Material.ENDER_PEARL,
            "§6§lFast Travel",
            Arrays.asList(
                "§7Teleport to the factory"
            )
        ));

        player.openInventory(inv);
    }

    private ItemStack createFactoryStatusItem(Factory factory) {
        Material material = factory.getStatus() == FactoryStatus.RUNNING ?
            Material.GREEN_WOOL : Material.RED_WOOL;

        List<String> lore = new ArrayList<>();
        lore.add("§7Status: " + factory.getStatus().getDisplay());
        lore.add("§7Level: §e" + factory.getLevel());
        lore.add("§7Type: " + factory.getType().getDisplayName());

        if (factory.getCurrentProduction() != null) {
            ProductionTask task = factory.getCurrentProduction();
            Recipe recipe = plugin.getRecipeManager().getRecipe(task.getRecipeId());
            lore.add("");
            lore.add("§6Active Production:");
            lore.add("§e" + recipe.getName());
            lore.add("§7Time remaining: §e" + task.getRemainingTime() + "s");
            lore.add("§7Progress: §e" + (int)(task.getProgress() * 100) + "%");
        }

        return createItem(material, "§6§lFactory Status", lore);
    }

    // ==================== RECIPE SELECTION GUI ====================
    public void openRecipeMenu() {
        Factory factory = plugin.getFactoryManager().getFactory(currentFactoryId);
        if (factory == null) return;

        // Store factory ID in player's persistent data for click handling
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_factory_id"), PersistentDataType.STRING, currentFactoryId);

        List<Recipe> recipes = plugin.getRecipeManager()
            .getRecipesByFactoryType(factory.getType().getId());

        if (recipes.isEmpty()) {
            player.sendMessage("§cError: No recipes found for factory type: " + factory.getType().getId());
            plugin.getLogger().warning("No recipes loaded for factory type: " + factory.getType().getId() +
                ". Available recipes: " + plugin.getRecipeManager().getAllRecipes().keySet());
            plugin.getLogger().warning("Factory type: " + factory.getType().getId() +
                ", Factory ID: " + currentFactoryId);
            return;
        }

        int size = ((recipes.size() + 8) / 9) * 9;
        if (size > 54) size = 54;
        if (size < 27) size = 27;

        Inventory inv = Bukkit.createInventory(null, size, "§6§lSelect Production Recipe");

        int slot = 0;
        for (Recipe recipe : recipes) {
            if (slot >= size - 9) break;

            List<String> lore = new ArrayList<>();
            lore.add("§7Production time: §e" + recipe.getProductionTime() + "s");
            lore.add("");
            lore.add("§7Required Inputs:");

            for (Map.Entry<String, Integer> input : recipe.getInputs().entrySet()) {
                ResourceItem resource = plugin.getResourceManager().getResource(input.getKey());
                String name = resource != null ? resource.getName() : input.getKey();
                lore.add("  §e" + input.getValue() + "x " + name);
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
                plugin.getLogger().warning("Invalid material for recipe " + recipe.getId() + ": " + recipe.getIcon() + ". Using STONE as fallback.");
            }
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(recipe.getName());
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "recipe_id"), PersistentDataType.STRING, recipe.getId());
                item.setItemMeta(meta);

                // Debug logging untuk recipe item creation
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

    // ==================== RECIPE CONFIRMATION GUI ====================
    public void openRecipeConfirm(String recipeId) {
        this.currentRecipeId = recipeId;

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
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_recipe_id"), PersistentDataType.STRING, recipeId);
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_factory_id"), PersistentDataType.STRING, currentFactoryId);

        Inventory inv = Bukkit.createInventory(null, 27,
            "§6§lConfirm: §e" + recipe.getName());

        // Fill with border
        Material borderMat = Material.matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        // Recipe info
        List<String> lore = new ArrayList<>();
        lore.add("§7Production time: §e" + recipe.getProductionTime() + "s");
        lore.add("");
        lore.add("§7Input:");
        for (Map.Entry<String, Integer> input : recipe.getInputs().entrySet()) {
            int available = plugin.getStorageManager().getInputAmount(currentFactoryId, input.getKey());
            ResourceItem resource = plugin.getResourceManager().getResource(input.getKey());
            String name = resource != null ? resource.getName() : input.getKey();

            String color = available >= input.getValue() ? "§a" : "§c";
            lore.add("  " + color + input.getValue() + "x " + name + " §7(" + available + " available)");
        }

        Material confirmMaterial;
        try {
            confirmMaterial = Material.valueOf(recipe.getIcon());
        } catch (IllegalArgumentException e) {
            // Fallback to a default material if the icon is invalid
            confirmMaterial = Material.STONE;
            plugin.getLogger().warning("Invalid material for recipe confirmation " + recipe.getId() + ": " + recipe.getIcon() + ". Using STONE as fallback.");
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

    // ==================== STORAGE GUI ====================
    public void openStorageMenu() {
        Factory factory = plugin.getFactoryManager().getFactory(currentFactoryId);
        if (factory == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return;
        }

        if (plugin.getStorageManager() == null) {
            player.sendMessage("§cError: Storage manager not available!");
            return;
        }

        // Store factory ID in player's persistent data for click handling
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_factory_id"), PersistentDataType.STRING, currentFactoryId);

        int slots = plugin.getConfig().getInt("factory.base-storage-slots", 9) +
                    (factory.getLevel() - 1) * plugin.getConfig().getInt("factory.slots-per-level", 9);

        int size = ((slots + 8) / 9) * 9;
        if (size > 54) size = 54;

        Inventory inv = Bukkit.createInventory(null, size, "§6§lFactory Storage");

        Map<String, Integer> storage = plugin.getStorageManager().getInputStorage(currentFactoryId);
        int slot = 0;

        if (storage != null) {
            for (Map.Entry<String, Integer> entry : storage.entrySet()) {
                if (slot >= size - 9) break;

                ItemStack item = plugin.getResourceManager().createItemStack(entry.getKey(), entry.getValue());
                if (item != null) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        // Store resource ID in the item
                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "resource_id"), PersistentDataType.STRING, entry.getKey());

                        // Add lore for clarity
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        lore.add("");
                        lore.add("§eLeft Click: §aTake All");
                        lore.add("§eRight Click: §aTake 1");
                        meta.setLore(lore);

                        item.setItemMeta(meta);
                    }
                    inv.setItem(slot++, item);
                }
            }
        }

        // Back button
        Material backMat = Material.matchMaterial(plugin.getConfig().getString("gui.back-item", "ARROW"));
        inv.setItem(size - 1, createItem(backMat != null ? backMat : Material.ARROW, "§e§lBack", null));

        player.openInventory(inv);
    }

    // ==================== INVOICE MANAGEMENT GUI ====================
    public void openInvoiceMenu() {
        // Store factory ID in player's persistent data for click handling
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_factory_id"), PersistentDataType.STRING, currentFactoryId);

        List<Invoice> invoices = plugin.getInvoiceManager()
            .getInvoicesByOwner(player.getUniqueId());

        int size = ((invoices.size() + 8) / 9) * 9;
        if (size > 54) size = 54;
        if (size < 27) size = 27;

        Inventory inv = Bukkit.createInventory(null, size, "§6§lInvoice Management");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        int slot = 0;

        for (Invoice invoice : invoices) {
            if (slot >= size - 9) break;

            List<String> lore = new ArrayList<>();
            lore.add("§7Type: " + invoice.getType().getDisplay());
            lore.add("§7Amount: §6$" + String.format("%.2f", invoice.getAmount()));
            lore.add("§7Due date: §c" + sdf.format(new Date(invoice.getDueDate())));
            lore.add("");

            if (invoice.isOverdue()) {
                lore.add("§c§lOVERDUE!");
            }

            lore.add("§eClick to pay");

            Material material = invoice.isOverdue() ? Material.RED_STAINED_GLASS_PANE : Material.PAPER;

            ItemStack item = createItem(material,
                "§eInvoice #" + invoice.getId().substring(0, 8), lore);

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "invoice_id"), PersistentDataType.STRING, invoice.getId());
                item.setItemMeta(meta);
            }

            inv.setItem(slot++, item);
        }

        // Back button
        inv.setItem(size - 1, createItem(Material.ARROW, "§e§lBack", null));

        player.openInventory(inv);
    }

    // ==================== UPGRADE FACTORY GUI ====================
    public void openUpgradeMenu() {
        Factory factory = plugin.getFactoryManager().getFactory(currentFactoryId);
        if (factory == null) return;

        // Store factory ID in player's persistent data for click handling
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_factory_id"), PersistentDataType.STRING, currentFactoryId);

        int maxLevel = plugin.getConfig().getInt("factory.max-level", 5);

        Inventory inv = Bukkit.createInventory(null, 27, "§6§lUpgrade Factory");

        // Fill with border
        Material borderMat = Material.matchMaterial(plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        // Current level
        List<String> currentLore = new ArrayList<>();
        currentLore.add("§7Current Level");
        currentLore.add("");
        currentLore.add("§aCost reduction: §e" +
            (factory.getLevel() - 1) * plugin.getConfig().getDouble("factory.level-bonuses.cost-reduction") + "%");
        currentLore.add("§aTime reduction: §e" +
            (factory.getLevel() - 1) * plugin.getConfig().getDouble("factory.level-bonuses.time-reduction") + "%");

        inv.setItem(11, createItem(Material.DIAMOND,
            "§6Level " + factory.getLevel(), currentLore));

        if (factory.getLevel() < maxLevel) {
            // Next level
            int nextLevel = factory.getLevel() + 1;
            double upgradeCost = factory.getPrice() * 0.5 * factory.getLevel();

            List<String> nextLore = new ArrayList<>();
            nextLore.add("§7Next Level");
            nextLore.add("");
            nextLore.add("§7Upgrade cost: §6$" + String.format("%.2f", upgradeCost));
            nextLore.add("");
            nextLore.add("§7Next level bonuses:");
            nextLore.add("§a+ Cost reduction: §e" +
                plugin.getConfig().getDouble("factory.level-bonuses.cost-reduction") + "%");
            nextLore.add("§a+ Time reduction: §e" +
                plugin.getConfig().getDouble("factory.level-bonuses.time-reduction") + "%");
            nextLore.add("§c+ Additional tax: §e" +
                plugin.getConfig().getDouble("tax.level-multiplier") + "%");
            nextLore.add("");
            nextLore.add("§eClick to upgrade");

            inv.setItem(15, createItem(Material.EMERALD,
                "§aLevel " + nextLevel, nextLore));
        } else {
            inv.setItem(15, createItem(Material.BARRIER,
                "§cMax Level", Arrays.asList("§7Factory has reached max level")));
        }

        // Back button
        Material backMat = Material.matchMaterial(plugin.getConfig().getString("gui.back-item", "ARROW"));
        inv.setItem(26, createItem(backMat != null ? backMat : Material.ARROW, "§e§lBack", null));

        player.openInventory(inv);
    }

    // ==================== CLICK HANDLER ====================
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null || event.getClickedInventory() == null) {
            plugin.getLogger().info("handleClick: null item or inventory");
            return;
        }

        String title = event.getView().getTitle();
        ItemStack clicked = event.getCurrentItem();

        if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
            plugin.getLogger().info("FactoryGUI handleClick called for title: " + title);
        }

        // Cancel all events by default to prevent item dragging/taking
        // Only storage GUI allows specific interactions
        if (!title.contains("Storage") && !title.toLowerCase().contains("select")) {
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Cancelling event for non-storage GUI");
            }
            event.setCancelled(true);
        } else {
            // For storage GUI, only cancel specific interactions that shouldn't be allowed
            // Allow clicking on actual storage items and back button, but prevent other interactions
            if (event.isShiftClick() || event.getClick().toString().contains("DROP") ||
                (event.getClickedInventory().equals(event.getView().getTopInventory()) &&
                 !isStorageItem(clicked) && !isBackButton(clicked))) {
                if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                    plugin.getLogger().info("Cancelling specific storage interaction");
                }
                event.setCancelled(true);
            } else {
                if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                    plugin.getLogger().info("Allowing storage interaction");
                }
            }
        }

        // Handle clicks based on GUI type
        // Main Menu clicks
        if (title.contains("Factory:")) {
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Handling main menu click");
            }
            handleMainMenuClick(clicked);
        }
        // Recipe selection clicks
        else if (title.toLowerCase().contains("select") || title.contains("Production Recipe")) {
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Handling recipe menu click");
            }
            handleRecipeMenuClick(clicked);
        }
        // Recipe confirmation clicks
        else if (title.contains("Confirm:")) {
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Handling confirm click");
            }
            handleConfirmClick(clicked);
        }
        // Storage clicks
        else if (title.contains("Storage")) {
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Handling storage click");
            }
            handleStorageClick(event);
        }
        // Invoice clicks
        else if (title.contains("Invoice")) {
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Handling invoice click");
            }
            handleInvoiceClick(clicked);
        }
        // Upgrade clicks
        else if (title.contains("Upgrade")) {
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Handling upgrade click");
            }
            handleUpgradeClick(clicked);
        }
    }

    private boolean isStorageItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;

        // Check if item has resource_id (indicates it's a storage item)
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String resourceId = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "resource_id"), PersistentDataType.STRING);
            return resourceId != null;
        }
        return false;
    }

    private boolean isBackButton(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        return item.getType() == Material.ARROW && item.getItemMeta().getDisplayName().contains("Back");
    }

    private void handleMainMenuClick(ItemStack clicked) {
        String name = clicked.getItemMeta().getDisplayName();

        if (name.contains("Start Production")) {
            openRecipeMenu();
        } else if (name.contains("Manage Invoices")) {
            openInvoiceMenu();
        } else if (name.contains("Factory Storage")) {
            openStorageMenu();
        } else if (name.contains("Upgrade Factory")) {
            openUpgradeMenu();
        } else if (name.contains("Fast Travel")) {
            player.closeInventory();

            // Get factory and check ownership
            Factory factory = plugin.getFactoryManager().getFactory(currentFactoryId);
            if (factory == null) {
                player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
                return;
            }

            if (!player.getUniqueId().equals(factory.getOwner())) {
                player.sendMessage("§cYou are not the owner of this factory!");
                return;
            }

            // Check if fast travel location is set
            if (factory.getFastTravelLocation() == null) {
                player.sendMessage("§cFast travel location not set for this factory!");
                player.sendMessage("§7Contact an admin to set the fast travel location.");
                return;
            }

            // Teleport player to factory
            if (plugin.getFactoryManager().teleportPlayer(player, currentFactoryId)) {
                player.sendMessage("§aSuccessfully teleported to the factory!");
            } else {
                player.sendMessage("§cFailed to teleport to the factory!");
            }
        }
    }

    private void handleRecipeMenuClick(ItemStack clicked) {
        if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
            plugin.getLogger().info("=== RECIPE MENU CLICK HANDLER ===");
        }

        if (clicked == null || clicked.getType() == Material.AIR) {
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Clicked item is null or air");
            }
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Item meta is null");
            }
            return;
        }

        String name = meta.getDisplayName();
        if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
            plugin.getLogger().info("Clicked item name: " + name);
            plugin.getLogger().info("Clicked item type: " + clicked.getType());
        }

        // Handle back button
        if (name != null && name.contains("Back")) {
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Back button clicked, opening main menu");
            }
            openMainMenu();
            return;
        }

        // Check if this is a recipe item by looking for recipe_id in persistent data
        String recipeId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "recipe_id"), PersistentDataType.STRING);
        if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
            plugin.getLogger().info("Recipe ID from persistent data: " + recipeId);
        }

        if (recipeId != null && !recipeId.isEmpty()) {
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Found recipe ID, validating recipe...");
            }
            // Validate that the recipe exists before opening confirmation
            Recipe recipe = plugin.getRecipeManager().getRecipe(recipeId);
            if (recipe != null) {
                if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                    plugin.getLogger().info("Recipe found: " + recipe.getName() + ", opening confirmation");
                }
                openRecipeConfirm(recipeId);
            } else {
                plugin.getLogger().warning("Recipe not found for ID: " + recipeId + " in factory type: " +
                    plugin.getFactoryManager().getFactory(currentFactoryId).getType().getId());
                player.sendMessage("§cError: Recipe not found! (ID: " + recipeId + ")");
            }
        } else {
            // Log non-recipe items clicked in recipe menu for debugging
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Clicked non-recipe item in recipe menu: " + name + " (Material: " + clicked.getType() + ")");
                plugin.getLogger().info("Available recipe IDs should be: " +
                    plugin.getRecipeManager().getRecipesByFactoryType(
                        plugin.getFactoryManager().getFactory(currentFactoryId).getType().getId()
                    ).stream().map(Recipe::getId).toList());
            }
        }
    }

    private void handleConfirmClick(ItemStack clicked) {
        String name = clicked.getItemMeta().getDisplayName();

        if (name.contains("Confirm")) {
            startProduction();
        } else if (name.contains("Cancel")) {
            // Clear recipe ID when canceling
            player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "current_recipe_id"));
            openRecipeMenu();
        }
    }

    private void startProduction() {
        Factory factory = plugin.getFactoryManager().getFactory(currentFactoryId);
        Recipe recipe = plugin.getRecipeManager().getRecipe(currentRecipeId);

        if (factory == null || recipe == null) return;

        // Check materials in input storage
        for (Map.Entry<String, Integer> input : recipe.getInputs().entrySet()) {
            int available = plugin.getStorageManager().getInputAmount(currentFactoryId, input.getKey());
            if (available < input.getValue()) {
                // Clear recipe ID when there are insufficient materials
                player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "current_recipe_id"));
                player.sendMessage(plugin.getLanguageManager().getMessage("production-insufficient-materials"));
                player.closeInventory();
                return;
            }
        }

        // Remove materials from input storage
        for (Map.Entry<String, Integer> input : recipe.getInputs().entrySet()) {
            plugin.getStorageManager().removeInputItem(currentFactoryId, input.getKey(), input.getValue());
        }

        // Start production
        plugin.getFactoryManager().startProduction(factory, currentRecipeId);

        // Clear recipe ID after successful production start
        player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "current_recipe_id"));

        player.sendMessage(plugin.getLanguageManager().getMessage("production-started")
            .replace("{recipe}", recipe.getName()));
        player.closeInventory();
    }

    private void handleStorageClick(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String name = clicked.getItemMeta().getDisplayName();
        Inventory clickedInventory = event.getClickedInventory();

        // Handle back button
        if (clicked.getType() == Material.ARROW && name.contains("Back")) {
            openMainMenu();
            return;
        }

        // Check if click is inside the GUI or player inventory
        if (clickedInventory == event.getView().getTopInventory()) {
            // WITHDRAW (taking items from factory storage)
            handleWithdraw(clicked);
        } else if (clickedInventory == event.getView().getBottomInventory()) {
            // DEPOSIT (putting items into factory storage)
            handleDeposit(clicked);
        }
    }

    private void handleWithdraw(ItemStack clickedItem) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        String resourceId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "resource_id"), PersistentDataType.STRING);
        if (resourceId == null) return;

        int amountToWithdraw = clickedItem.getAmount(); // Default to the full stack size

        // Logic to give item to player
        ItemStack toGive = plugin.getResourceManager().createItemStack(resourceId, amountToWithdraw);
        if (toGive == null) {
            player.sendMessage("§cError: Could not create item for this resource.");
            return;
        }

        // Remove from input storage
        plugin.getStorageManager().removeInputItem(currentFactoryId, resourceId, amountToWithdraw);

        // Add to player's inventory
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);
        if (!leftover.isEmpty()) {
            // If inventory is full, return items to storage
            int amountNotAdded = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            plugin.getStorageManager().addItem(currentFactoryId, resourceId, amountNotAdded);
            player.sendMessage(plugin.getLanguageManager().getMessage("storage-inventory-full"));
        } else {
            player.sendMessage(plugin.getLanguageManager().getMessage("storage-item-withdrawn")
                .replace("{amount}", String.valueOf(amountToWithdraw))
                .replace("{item}", meta.getDisplayName()));
        }

        // Refresh the GUI
        openStorageMenu();
    }

    private void handleDeposit(ItemStack clickedItem) {
        String resourceId = plugin.getResourceManager().getResourceId(clickedItem);
        if (resourceId == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("storage-invalid-item"));
            return;
        }

        int amountToDeposit = clickedItem.getAmount();

        // Add to input storage
        plugin.getStorageManager().addInputItem(currentFactoryId, resourceId, amountToDeposit);

        // Remove from player's inventory
        clickedItem.setAmount(0);

        player.sendMessage(plugin.getLanguageManager().getMessage("storage-item-deposited")
            .replace("{amount}", String.valueOf(amountToDeposit))
            .replace("{item}", clickedItem.getItemMeta().getDisplayName()));

        // Do not refresh the GUI - items should remain visible in inventory
        // openStorageMenu();
    }

    private void handleInvoiceClick(ItemStack clicked) {
        String name = clicked.getItemMeta().getDisplayName();

        if (name.contains("Back")) {
            openMainMenu();
            return;
        }

        // Get invoice ID from item
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String invoiceId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "invoice_id"), PersistentDataType.STRING);
        if (invoiceId == null) return;

        if (plugin.getInvoiceManager().payInvoice(player, invoiceId)) {
            // Extract amount safely from lore with better error handling
            String amount = extractAmountFromLore(clicked.getItemMeta().getLore());
            player.sendMessage(plugin.getLanguageManager().getMessage("invoice-paid")
                .replace("{amount}", amount));
            openInvoiceMenu(); // Refresh
        } else {
            // Extract amount safely from lore with better error handling
            String amount = extractAmountFromLore(clicked.getItemMeta().getLore());
            player.sendMessage(plugin.getLanguageManager().getMessage("insufficient-funds")
                .replace("{amount}", amount));
        }
    }

    private String extractAmountFromLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return "0.00";
        }

        // Find the line that contains "Amount:" and extract the value
        for (String line : lore) {
            if (line.contains("Amount:")) {
                try {
                    // Extract number after "$" symbol
                    String[] parts = line.split("\\$");
                    if (parts.length > 1) {
                        // Get the number part and parse it
                        String numberStr = parts[1].replaceAll("[^0-9\\\\.]", "");
                        return numberStr;
                    }
                } catch (Exception e) {
                    // If parsing fails, return default
                    break;
                }
            }
        }

        return "0.00";
    }

    private void handleUpgradeClick(ItemStack clicked) {
        String name = clicked.getItemMeta().getDisplayName();

        if (name.contains("Back")) {
            openMainMenu();
            return;
        }

        if (name.contains("Level") && clicked.getType() == Material.EMERALD) {
            if (plugin.getFactoryManager().upgradeFactory(player, currentFactoryId)) {
                player.sendMessage(plugin.getLanguageManager().getMessage("level-up")
                    .replace("{level}", String.valueOf(
                        plugin.getFactoryManager().getFactory(currentFactoryId).getLevel()
                    )));
                openUpgradeMenu(); // Refresh
            } else {
                Factory factory = plugin.getFactoryManager().getFactory(currentFactoryId);
                double cost = factory.getPrice() * 0.5 * factory.getLevel();
                player.sendMessage(plugin.getLanguageManager().getMessage("insufficient-funds")
                    .replace("{amount}", String.format("%.2f", cost)));
            }
        }
    }

    // ==================== UTILITY METHODS ====================
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (name != null) meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }
}