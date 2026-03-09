package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.managers.ResourceManager;
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

    // Separate GUI instances
    private final MainMenuGUI mainMenuGUI;
    private final RecipeSelectionGUI recipeSelectionGUI;
    private final RecipeConfirmationGUI recipeConfirmationGUI;
    private final StorageGUI storageGUI;
    private final InvoiceGUI invoiceGUI;
    private final UpgradeGUI upgradeGUI;

    public FactoryGUI(FactoryCore plugin, Player player, String factoryId) {
        this.plugin = plugin;
        this.player = player;
        this.currentFactoryId = factoryId;

        // Initialize GUI instances
        this.mainMenuGUI = new MainMenuGUI(plugin, player, factoryId);
        this.recipeSelectionGUI = new RecipeSelectionGUI(plugin, player, factoryId);
        this.recipeConfirmationGUI = new RecipeConfirmationGUI(plugin, player, factoryId);
        this.storageGUI = new StorageGUI(plugin, player, factoryId);
        this.invoiceGUI = new InvoiceGUI(plugin, player, factoryId);
        this.upgradeGUI = new UpgradeGUI(plugin, player, factoryId);
    }

    public void setCurrentRecipeId(String recipeId) {
        this.currentRecipeId = recipeId;
    }

    // ==================== MAIN FACTORY GUI ====================
    public void openMainMenu() {
        mainMenuGUI.openMainMenu();
    }

    // ==================== RECIPE SELECTION GUI ====================
    public void openRecipeMenu() {
        recipeSelectionGUI.openRecipeMenu();
    }

    // ==================== RECIPE CONFIRMATION GUI ====================
    public void openRecipeConfirm(String recipeId) {
        recipeConfirmationGUI.setRecipeId(recipeId);
        recipeConfirmationGUI.openRecipeConfirm(recipeId);
    }

    // ==================== STORAGE GUI ====================
    public void openStorageMenu() {
        storageGUI.openStorageMenu();
    }

    // ==================== INVOICE MANAGEMENT GUI ====================
    public void openInvoiceMenu() {
        invoiceGUI.openInvoiceMenu();
    }

    // ==================== UPGRADE FACTORY GUI ====================
    public void openUpgradeMenu() {
        upgradeGUI.openUpgradeMenu();
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

        // Always cancel the event to prevent Bukkit's default item movement.
        // All deposit/withdraw logic is handled manually.
        event.setCancelled(true);
        if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
            plugin.getLogger().info("Event cancelled to prevent default item movement");
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
        // Upgrade Factory (info panel) clicks
        else if (title.contains("Upgrade Factory")) {
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Handling upgrade menu click");
            }
            handleUpgradeClick(clicked);
        }
        // Upgrade confirmation clicks
        else if (title.contains("Confirm Upgrade")) {
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info("Handling confirm upgrade click");
            }
            handleConfirmUpgradeClick(clicked);
        }
    }

    private boolean isStorageItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return false;

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
        if (item == null || item.getType() == Material.AIR)
            return false;
        return item.getType() == Material.ARROW && item.getItemMeta().getDisplayName().contains("Back");
    }

    private void handleMainMenuClick(ItemStack clicked) {
        String name = clicked.getItemMeta().getDisplayName();

        if (name.contains("Back")) {
            new HubGUI(plugin, player).openHubMenu();
            return;
        }

        if (name.contains("Start Production")) {
            openRecipeMenu();
        } else if (name.contains("Manage Invoices")) {
            openInvoiceMenu();
        } else if (name.contains("Factory Storage")) {
            openStorageMenu();
        } else if (name.contains("Upgrade Factory")) {
            openUpgradeMenu();
        } else if (name.contains("Toggle Border")) {
            // Toggle border particle visualization
            if (plugin.getBorderParticleTask() != null) {
                boolean enabled = plugin.getBorderParticleTask().toggle(player.getUniqueId());
                if (enabled) {
                    player.sendMessage(plugin.getLanguageManager().getMessage("player-factory-border-enabled"));
                    if (plugin.getConfig().getBoolean("notifications.sound.enabled")) {
                        try {
                            player.playSound(player.getLocation(),
                                    plugin.getConfig().getString("notifications.sound.border-toggle",
                                            "BLOCK_NOTE_BLOCK_PLING"),
                                    1.0f, 1.5f);
                        } catch (Exception ignored) {
                        }
                    }
                    if (plugin.getConfig().getBoolean("notifications.titles.enabled")) {
                        player.sendTitle(
                                plugin.getLanguageManager().getMessage("titles.player-factory-border-on.title"),
                                plugin.getLanguageManager().getMessage("titles.player-factory-border-on.subtitle"),
                                10, 30, 10);
                    }
                } else {
                    player.sendMessage(plugin.getLanguageManager().getMessage("player-factory-border-disabled"));
                    if (plugin.getConfig().getBoolean("notifications.sound.enabled")) {
                        try {
                            player.playSound(player.getLocation(),
                                    plugin.getConfig().getString("notifications.sound.border-toggle",
                                            "BLOCK_NOTE_BLOCK_PLING"),
                                    1.0f, 0.5f);
                        } catch (Exception ignored) {
                        }
                    }
                    if (plugin.getConfig().getBoolean("notifications.titles.enabled")) {
                        player.sendTitle(
                                plugin.getLanguageManager().getMessage("titles.player-factory-border-off.title"),
                                plugin.getLanguageManager().getMessage("titles.player-factory-border-off.subtitle"),
                                10, 30, 10);
                    }
                }
                // Refresh the menu to show updated state
                openMainMenu();
            }
        } else if (name.contains("Fast Travel")) {
            player.closeInventory();

            // Check player-created factory first
            com.aithor.factorycore.models.PlayerFactory playerFactory = plugin.getPlayerFactoryManager() != null
                    ? plugin.getPlayerFactoryManager().getFactory(currentFactoryId)
                    : null;
            if (playerFactory != null) {
                org.bukkit.Location loc = playerFactory.getCenterLocation();
                if (loc != null) {
                    player.teleport(loc);
                    player.sendMessage("§aSuccessfully teleported to the factory!");
                } else {
                    player.sendMessage("§cFailed to teleport to the factory!");
                }
                return;
            }

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
        String recipeId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "recipe_id"),
                PersistentDataType.STRING);
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
                // Resolve factory type from either manager for logging
                String factoryTypeId = "unknown";
                Factory f = plugin.getFactoryManager().getFactory(currentFactoryId);
                if (f != null)
                    factoryTypeId = f.getType().getId();
                else if (plugin.getPlayerFactoryManager() != null) {
                    com.aithor.factorycore.models.PlayerFactory pf = plugin.getPlayerFactoryManager()
                            .getFactory(currentFactoryId);
                    if (pf != null)
                        factoryTypeId = pf.getType().getId();
                }
                plugin.getLogger()
                        .warning("Recipe not found for ID: " + recipeId + " in factory type: " + factoryTypeId);
                player.sendMessage("§cError: Recipe not found! (ID: " + recipeId + ")");
            }
        } else {
            // Log non-recipe items clicked in recipe menu for debugging
            if (plugin.getConfig().getBoolean("debug.gui-debug", false)) {
                plugin.getLogger().info(
                        "Clicked non-recipe item in recipe menu: " + name + " (Material: " + clicked.getType() + ")");
                // Resolve type safely
                String typeId = "unknown";
                Factory f = plugin.getFactoryManager().getFactory(currentFactoryId);
                if (f != null)
                    typeId = f.getType().getId();
                else if (plugin.getPlayerFactoryManager() != null) {
                    com.aithor.factorycore.models.PlayerFactory pf = plugin.getPlayerFactoryManager()
                            .getFactory(currentFactoryId);
                    if (pf != null)
                        typeId = pf.getType().getId();
                }
                plugin.getLogger().info("Available recipe IDs should be: " +
                        plugin.getRecipeManager().getRecipesByFactoryType(typeId).stream()
                                .map(Recipe::getId).toList());
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
        // Resolve factory from either admin or player factory
        Factory factory = plugin.getFactoryManager().getFactory(currentFactoryId);
        com.aithor.factorycore.models.PlayerFactory playerFactory = null;
        if (factory == null && plugin.getPlayerFactoryManager() != null) {
            playerFactory = plugin.getPlayerFactoryManager().getFactory(currentFactoryId);
        }
        Recipe recipe = plugin.getRecipeManager().getRecipe(currentRecipeId);

        if ((factory == null && playerFactory == null) || recipe == null)
            return;

        // ── Check: factory must have an employee assigned ──────────────────────
        if (!plugin.getNPCManager().factoryHasEmployee(currentFactoryId)) {
            player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "current_recipe_id"));
            player.sendMessage("§c§lProduction Blocked! §cThis factory has no employee assigned.");
            player.sendMessage("§7Go to §bEmployees Center §7→ §6Employee Shop §7to hire an employee.");
            player.closeInventory();
            return;
        }

        // ── Check money cost (with research buff) ──────────────────────────────
        double moneyCost = recipe.getMoneyCost();
        if (moneyCost > 0 && plugin.getResearchManager() != null) {
            double costReduction = plugin.getResearchManager().getProductionCostReduction(player.getUniqueId());
            if (costReduction > 0) {
                moneyCost *= (1 - (costReduction / 100.0));
            }
        }
        if (moneyCost > 0 && !plugin.getEconomy().has(player, moneyCost)) {
            player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "current_recipe_id"));
            player.sendMessage(plugin.getLanguageManager().getMessage("insufficient-funds")
                    .replace("{amount}", String.format("%.2f", moneyCost)));
            player.closeInventory();
            return;
        }

        // Check materials in input storage
        // Both plugin resources and vanilla: inputs are stored together in
        // input-storage.
        for (Map.Entry<String, Integer> input : recipe.getInputs().entrySet()) {
            int available = plugin.getStorageManager().getInputAmount(currentFactoryId, input.getKey());
            if (available < input.getValue()) {
                player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "current_recipe_id"));
                player.sendMessage(plugin.getLanguageManager().getMessage("production-insufficient-materials"));
                player.closeInventory();
                return;
            }
        }

        // Remove materials from input storage (same for both plugin resources and
        // vanilla inputs)
        for (Map.Entry<String, Integer> input : recipe.getInputs().entrySet()) {
            plugin.getStorageManager().removeInputItem(currentFactoryId, input.getKey(), input.getValue());
        }

        // ── Deduct money cost (with research buff applied) ─────────────────────
        if (moneyCost > 0) {
            plugin.getEconomy().withdrawPlayer(player, moneyCost);

            // Achievement: Relentless Grinder - cumulative production cost
            if (plugin.getAchievementManager() != null) {
                plugin.getAchievementManager().addProgress(player, "relentless_grinder", moneyCost);
            }
        }

        // Start production on the correct factory type
        if (factory != null) {
            plugin.getFactoryManager().startProduction(factory, currentRecipeId);
        } else {
            // PlayerFactory production start (mirrors FactoryManager logic)
            plugin.getPlayerFactoryManager().startProduction(playerFactory, currentRecipeId);
        }

        // Clear recipe ID after successful production start
        player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "current_recipe_id"));

        // Show employee buff info if applicable
        double reduction = plugin.getNPCManager().getProductionTimeReductionForFactory(currentFactoryId);
        String msg = plugin.getLanguageManager().getMessage("production-started")
                .replace("{recipe}", recipe.getName());
        player.sendMessage(msg);
        if (reduction > 0) {
            player.sendMessage("§a⚡ Employee Buff: §f-" + reduction + "% §aproduction time!");
        }
        // Show research buff info if applicable
        if (plugin.getResearchManager() != null) {
            double researchTimeReduction = plugin.getResearchManager().getProductionTimeReduction(player.getUniqueId());
            double researchCostReduction = plugin.getResearchManager().getProductionCostReduction(player.getUniqueId());
            if (researchTimeReduction > 0) {
                player.sendMessage("§d🔬 Research Buff: §f-" + String.format("%.0f", researchTimeReduction)
                        + "% §dproduction time!");
            }
            if (researchCostReduction > 0) {
                player.sendMessage("§d🔬 Research Buff: §f-" + String.format("%.0f", researchCostReduction)
                        + "% §dproduction cost!");
            }
        }
        player.closeInventory();
    }

    private void handleStorageClick(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        ItemMeta clickedMeta = clicked.getItemMeta();
        String name = clickedMeta != null ? clickedMeta.getDisplayName() : "";
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null)
            return;

        // ── Check for action buttons first (tagged with storage_action key) ──
        if (clickedMeta != null) {
            String action = clickedMeta.getPersistentDataContainer().get(
                    new NamespacedKey(plugin, "storage_action"), PersistentDataType.STRING);
            if (action != null) {
                switch (action) {
                    case "withdraw_all" -> handleWithdrawAll();
                    case "deposit_all" -> handleDepositAll();
                }
                return;
            }
        }

        // Handle back button
        if (clicked.getType() == Material.ARROW && name.contains("Back")) {
            openMainMenu();
            return;
        }

        if (clickedInventory == event.getView().getTopInventory()) {
            // WITHDRAW: player clicked a storage item in the top (factory) inventory
            // Left-click = take all, Right-click = take 1
            if (!isStorageItem(clicked))
                return; // ignore non-storage items (e.g. decorations)

            boolean rightClick = event.getClick().isRightClick();
            handleWithdraw(clicked, rightClick ? 1 : clicked.getAmount());

        } else if (clickedInventory == event.getView().getBottomInventory()) {
            // DEPOSIT: player shift-clicks an item from their own inventory
            // Only trigger on shift-click to avoid accidental deposits
            if (!event.isShiftClick())
                return;

            handleDeposit(clicked);
        }
    }

    private void handleWithdraw(ItemStack clickedItem, int amountToWithdraw) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null)
            return;

        String resourceId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "resource_id"),
                PersistentDataType.STRING);
        if (resourceId == null)
            return;

        // Clamp to what is actually stored
        int available = plugin.getStorageManager().getInputAmount(currentFactoryId, resourceId);
        amountToWithdraw = Math.min(amountToWithdraw, available);
        if (amountToWithdraw <= 0)
            return;

        // Build the ItemStack to return to the player
        ItemStack toGive;
        if (ResourceManager.isVanillaInput(resourceId)) {
            // Return a plain vanilla item (no custom name / custom-model-data)
            org.bukkit.Material mat = ResourceManager.getVanillaMaterial(resourceId);
            if (mat == null) {
                player.sendMessage("§cError: Unknown material for this vanilla resource.");
                return;
            }
            toGive = new ItemStack(mat, amountToWithdraw);
        } else {
            toGive = plugin.getResourceManager().createItemStack(resourceId, amountToWithdraw);
        }

        if (toGive == null) {
            player.sendMessage("§cError: Could not create item for this resource.");
            return;
        }

        // Remove from input storage first
        plugin.getStorageManager().removeInputItem(currentFactoryId, resourceId, amountToWithdraw);

        // Add to player's inventory
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);
        if (!leftover.isEmpty()) {
            int amountNotAdded = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            plugin.getStorageManager().addInputItem(currentFactoryId, resourceId, amountNotAdded);
            player.sendMessage(plugin.getLanguageManager().getMessage("storage-inventory-full"));
        } else {
            String displayName = ResourceManager.isVanillaInput(resourceId)
                    ? ResourceManager.getVanillaDisplayName(resourceId)
                    : (meta.hasDisplayName() ? meta.getDisplayName() : resourceId);
            player.sendMessage(plugin.getLanguageManager().getMessage("storage-item-withdrawn")
                    .replace("{amount}", String.valueOf(amountToWithdraw))
                    .replace("{item}", displayName));
        }

        // Refresh the GUI
        openStorageMenu();
    }

    private void handleDeposit(ItemStack clickedItem) {
        // ── Try to identify as a plugin resource first ──────────────────────────
        String resourceId = plugin.getResourceManager().getResourceId(clickedItem);

        if (resourceId != null) {
            // Normal plugin resource deposit path
            int amountToDeposit = clickedItem.getAmount();
            String itemName = clickedItem.getItemMeta() != null
                    ? clickedItem.getItemMeta().getDisplayName()
                    : clickedItem.getType().toString();

            plugin.getStorageManager().addInputItem(currentFactoryId, resourceId, amountToDeposit);
            clickedItem.setAmount(0);

            player.sendMessage(plugin.getLanguageManager().getMessage("storage-item-deposited")
                    .replace("{amount}", String.valueOf(amountToDeposit))
                    .replace("{item}", itemName));
            openStorageMenu();
            return;
        }

        // ── Try as a plain vanilla item (for Smeltery) ───────────────────────────────
        // Check whether the factory currently accepts any vanilla: input that matches
        // the clicked item's material (and the item must be unmodified — no
        // custom-model-data).
        // Resolve from either admin or player factory
        Factory factory = plugin.getFactoryManager().getFactory(currentFactoryId);
        com.aithor.factorycore.models.PlayerFactory playerFactory = null;
        if (factory == null && plugin.getPlayerFactoryManager() != null) {
            playerFactory = plugin.getPlayerFactoryManager().getFactory(currentFactoryId);
        }
        FactoryType resolvedType = factory != null
                ? factory.getType()
                : (playerFactory != null ? playerFactory.getType() : null);
        if (resolvedType != null) {
            org.bukkit.Material clickedMat = clickedItem.getType();
            boolean isCustomised = clickedItem.hasItemMeta() &&
                    (clickedItem.getItemMeta().hasCustomModelData() ||
                            clickedItem.getItemMeta().hasDisplayName());

            if (!isCustomised) {
                String matchedKey = null;
                for (Recipe r : plugin.getRecipeManager().getRecipesByFactoryType(resolvedType.getId())) {
                    for (String inputKey : r.getInputs().keySet()) {
                        if (ResourceManager.isVanillaInput(inputKey)) {
                            org.bukkit.Material mat = ResourceManager.getVanillaMaterial(inputKey);
                            if (mat == clickedMat) {
                                matchedKey = inputKey;
                                break;
                            }
                        }
                    }
                    if (matchedKey != null)
                        break;
                }

                if (matchedKey != null) {
                    int amountToDeposit = clickedItem.getAmount();
                    plugin.getStorageManager().addInputItem(currentFactoryId, matchedKey, amountToDeposit);
                    clickedItem.setAmount(0);
                    player.sendMessage(plugin.getLanguageManager().getMessage("storage-item-deposited")
                            .replace("{amount}", String.valueOf(amountToDeposit))
                            .replace("{item}", ResourceManager.getVanillaDisplayName(matchedKey)));
                    openStorageMenu();
                    return;
                }
            }
        }

        // Item is neither a plugin resource nor an accepted vanilla material
        player.sendMessage(plugin.getLanguageManager().getMessage("storage-invalid-item"));
    }

    /**
     * Withdraws every resource from the factory's input storage and gives it
     * to the player. Items that don't fit are left in storage.
     */
    private void handleWithdrawAll() {
        Map<String, Integer> storage = new HashMap<>(plugin.getStorageManager().getInputStorage(currentFactoryId));
        if (storage.isEmpty()) {
            player.sendMessage("§7The storage is already empty.");
            openStorageMenu();
            return;
        }

        int totalWithdrawn = 0;
        int totalLeftover = 0;

        for (Map.Entry<String, Integer> entry : storage.entrySet()) {
            String resourceId = entry.getKey();
            int amount = entry.getValue();
            if (amount <= 0)
                continue;

            // Build item to give back: handle vanilla inputs separately
            ItemStack toGive;
            if (ResourceManager.isVanillaInput(resourceId)) {
                org.bukkit.Material mat = ResourceManager.getVanillaMaterial(resourceId);
                if (mat == null)
                    continue;
                toGive = new ItemStack(mat, amount);
            } else {
                toGive = plugin.getResourceManager().createItemStack(resourceId, amount);
            }
            if (toGive == null)
                continue;

            // Remove from storage first
            plugin.getStorageManager().removeInputItem(currentFactoryId, resourceId, amount);

            // Try to add to player inventory
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);
            if (!leftover.isEmpty()) {
                int notAdded = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
                plugin.getStorageManager().addInputItem(currentFactoryId, resourceId, notAdded);
                totalWithdrawn += (amount - notAdded);
                totalLeftover += notAdded;
            } else {
                totalWithdrawn += amount;
            }
        }

        if (totalLeftover > 0) {
            player.sendMessage("§eWithdrew §a" + totalWithdrawn + "§e item(s). §c" + totalLeftover
                    + " item(s) could not fit in your inventory.");
        } else {
            player.sendMessage("§aSuccessfully withdrew all §e" + totalWithdrawn + "§a item(s) from storage.");
        }

        openStorageMenu();
    }

    /**
     * Scans the player's entire inventory for items registered in the plugin's
     * ResourceManager and deposits all of them into the factory's input storage.
     * For Smeltery factories, also accepts plain vanilla items that match any
     * vanilla: input required by the factory's recipes.
     */
    private void handleDepositAll() {
        ItemStack[] contents = player.getInventory().getContents();
        int totalDeposited = 0;
        boolean foundAny = false;

        // Build a set of vanilla material keys accepted by this factory (for Smeltery)
        // Resolve from either admin or player factory
        Factory factory = plugin.getFactoryManager().getFactory(currentFactoryId);
        com.aithor.factorycore.models.PlayerFactory pfForVanilla = null;
        if (factory == null && plugin.getPlayerFactoryManager() != null) {
            pfForVanilla = plugin.getPlayerFactoryManager().getFactory(currentFactoryId);
        }
        FactoryType resolvedTypeForDeposit = factory != null
                ? factory.getType()
                : (pfForVanilla != null ? pfForVanilla.getType() : null);
        Map<org.bukkit.Material, String> acceptedVanilla = new HashMap<>();
        if (resolvedTypeForDeposit != null) {
            for (Recipe r : plugin.getRecipeManager().getRecipesByFactoryType(resolvedTypeForDeposit.getId())) {
                for (String inputKey : r.getInputs().keySet()) {
                    if (ResourceManager.isVanillaInput(inputKey)) {
                        org.bukkit.Material mat = ResourceManager.getVanillaMaterial(inputKey);
                        if (mat != null)
                            acceptedVanilla.put(mat, inputKey);
                    }
                }
            }
        }

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR)
                continue;

            // ── Try as a plugin resource first ────────────────────────────────
            String resourceId = plugin.getResourceManager().getResourceId(item);
            if (resourceId != null) {
                foundAny = true;
                int amount = item.getAmount();
                plugin.getStorageManager().addInputItem(currentFactoryId, resourceId, amount);
                player.getInventory().setItem(i, null);
                totalDeposited += amount;
                continue;
            }

            // ── Try as a plain vanilla item ───────────────────────────────────
            if (!acceptedVanilla.isEmpty()) {
                boolean isCustomised = item.hasItemMeta() &&
                        (item.getItemMeta().hasCustomModelData() ||
                                item.getItemMeta().hasDisplayName());
                if (!isCustomised) {
                    String vanillaKey = acceptedVanilla.get(item.getType());
                    if (vanillaKey != null) {
                        foundAny = true;
                        int amount = item.getAmount();
                        plugin.getStorageManager().addInputItem(currentFactoryId, vanillaKey, amount);
                        player.getInventory().setItem(i, null);
                        totalDeposited += amount;
                    }
                }
            }
        }

        if (!foundAny) {
            player.sendMessage("§7No valid items found in your inventory to deposit.");
        } else {
            player.sendMessage("§aSuccessfully deposited §e" + totalDeposited + "§a item(s) into storage.");
        }

        openStorageMenu();
    }

    private void handleInvoiceClick(ItemStack clicked) {
        String name = clicked.getItemMeta().getDisplayName();

        if (name.contains("Back")) {
            openMainMenu();
            return;
        }

        // Get invoice ID from item
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null)
            return;

        String invoiceId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "invoice_id"),
                PersistentDataType.STRING);
        if (invoiceId == null)
            return;

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

        // Player clicked the next-level EMERALD → open confirmation GUI
        if (name.contains("Level") && clicked.getType() == Material.EMERALD) {
            // Check admin factory first, then player factory
            Factory factory = plugin.getFactoryManager().getFactory(currentFactoryId);
            if (factory != null && !factory.isUpgrading()) {
                UpgradeGUI upgradeGUI = new UpgradeGUI(plugin, player, currentFactoryId);
                upgradeGUI.openUpgradeConfirm();
            } else if (factory != null && factory.isUpgrading()) {
                player.sendMessage("§c⚠ Factory is already being upgraded!");
            } else if (plugin.getPlayerFactoryManager() != null) {
                com.aithor.factorycore.models.PlayerFactory pf = plugin.getPlayerFactoryManager()
                        .getFactory(currentFactoryId);
                if (pf != null && !pf.isUpgrading()) {
                    UpgradeGUI upgradeGUI = new UpgradeGUI(plugin, player, currentFactoryId);
                    upgradeGUI.openUpgradeConfirm();
                } else if (pf != null && pf.isUpgrading()) {
                    player.sendMessage("§c⚠ Factory is already being upgraded!");
                }
            }
        }
    }

    private void handleConfirmUpgradeClick(ItemStack clicked) {
        if (!clicked.hasItemMeta())
            return;
        String name = clicked.getItemMeta().getDisplayName();

        if (name.contains("Confirm Upgrade")) {
            // Try admin factory first, then player factory
            Factory factory = plugin.getFactoryManager().getFactory(currentFactoryId);
            if (factory != null) {
                if (plugin.getFactoryManager().startUpgrade(player, currentFactoryId)) {
                    player.sendMessage("§a✔ §7Upgrade started! Your factory will reach the next level shortly.");
                    openUpgradeMenu();
                }
            } else if (plugin.getPlayerFactoryManager() != null) {
                com.aithor.factorycore.models.PlayerFactory pf = plugin.getPlayerFactoryManager()
                        .getFactory(currentFactoryId);
                if (pf != null) {
                    // PlayerFactory upgrade: same logic as FactoryManager.startUpgrade but for
                    // PlayerFactory
                    int maxLevel = plugin.getConfig().getInt("factory.max-level", 5);
                    if (pf.getLevel() >= maxLevel) {
                        player.sendMessage("§cFactory is already at max level!");
                        return;
                    }
                    double upgradeCost = pf.getPrice() * 0.5 * pf.getLevel();
                    if (!plugin.getEconomy().has(player, upgradeCost)) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("insufficient-funds")
                                .replace("{amount}", String.format("%.2f", upgradeCost)));
                        return;
                    }
                    plugin.getEconomy().withdrawPlayer(player, upgradeCost);
                    int targetLevel = pf.getLevel() + 1;
                    int duration = plugin.getConfig().getInt("factory.upgrade-time." + targetLevel, 60);
                    if (plugin.getResearchManager() != null) {
                        double rd = plugin.getResearchManager().getUpgradeTimeReduction(player.getUniqueId());
                        if (rd > 0)
                            duration = (int) (duration * (1 - rd / 100.0));
                        duration = Math.max(1, duration);
                    }
                    pf.setUpgradeStartTime(System.currentTimeMillis());
                    pf.setUpgradeDurationSeconds(duration);
                    plugin.getPlayerFactoryManager().saveAll();
                    player.sendMessage("§a✔ §7Upgrade started! Your factory will reach the next level shortly.");
                    openUpgradeMenu();
                }
            }
            // If startUpgrade returned false, the reason was already messaged to player
        } else if (name.contains("Cancel")) {
            openUpgradeMenu();
        }
    }

    // ==================== UTILITY METHODS ====================
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