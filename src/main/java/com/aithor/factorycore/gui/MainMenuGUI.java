package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.managers.StorageManager;
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

public class MainMenuGUI {

        private final FactoryCore plugin;
        private final Player player;
        private final String factoryId;

        public MainMenuGUI(FactoryCore plugin, Player player, String factoryId) {
                this.plugin = plugin;
                this.player = player;
                this.factoryId = factoryId;
        }

        public void openMainMenu() {
                // Check regular factory first, then player-created factory
                Factory factory = plugin.getFactoryManager().getFactory(factoryId);
                com.aithor.factorycore.models.PlayerFactory playerFactory = null;

                if (factory == null && plugin.getPlayerFactoryManager() != null) {
                        playerFactory = plugin.getPlayerFactoryManager().getFactory(factoryId);
                }

                if (factory == null && playerFactory == null) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
                        return;
                }

                // Determine owner and type from whichever factory type we found
                java.util.UUID factoryOwner = factory != null ? factory.getOwner() : playerFactory.getOwner();
                FactoryType factoryType = factory != null ? factory.getType() : playerFactory.getType();
                int factoryLevel = factory != null ? factory.getLevel() : playerFactory.getLevel();

                if (!player.getUniqueId().equals(factoryOwner)) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("factory-not-owned"));
                        return;
                }

                if (factoryType == null) {
                        player.sendMessage("§cError: Invalid Factory type!");
                        return;
                }

                // Store factory ID in player's persistent data for click handling
                player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_factory_id"),
                                PersistentDataType.STRING, factoryId);

                // ------------------------------------------------------------------
                // Resolve display name and title
                // ------------------------------------------------------------------
                String customDisplayName = factory != null ? factory.getDisplayName() : playerFactory.getDisplayName();
                String guiTitle = customDisplayName != null && !customDisplayName.isBlank()
                                ? "§6§lFactory: §e" + customDisplayName
                                : "§6§lFactory: §e" + factoryType.getDisplayName();

                Inventory inv = Bukkit.createInventory(null, 54, guiTitle);

                // Fill with border
                Material borderMat = Material
                                .matchMaterial(plugin.getConfig().getString("gui.border-item",
                                                "BLACK_STAINED_GLASS_PANE"));
                ItemStack border = createItem(borderMat != null ? borderMat : Material.BLACK_STAINED_GLASS_PANE, " ",
                                null);
                for (int i = 0; i < 54; i++) {
                        inv.setItem(i, border);
                }

                // ------------------------------------------------------------------
                // Row 0 (decorative top bar) – slot 4: Factory Status
                // ------------------------------------------------------------------
                inv.setItem(4, createFactoryStatusItem(factory, playerFactory));

                // ------------------------------------------------------------------
                // Row 1 – main action buttons (slots 10-16)
                // Slot 10: Start Production
                // Slot 12: Factory Storage
                // Slot 14: Upgrade Factory
                // Slot 16: Fast Travel
                // ------------------------------------------------------------------
                inv.setItem(10, createItem(Material.CRAFTING_TABLE,
                                "§a§lStart Production",
                                Arrays.asList(
                                                "§7Click to start production",
                                                "§7and select a recipe")));

                int storageSlots = plugin.getConfig().getInt("factory.base-storage-slots", 9);
                inv.setItem(12, createItem(Material.CHEST,
                                "§b§lFactory Storage",
                                Arrays.asList(
                                                "§7Access factory storage",
                                                "§7Slots: §e" + storageSlots,
                                                "",
                                                "§eLeft Click §7to open Input Storage",
                                                "§eRight Click §7to open Output Storage",
                                                "§c(Output requires employee)")));

                inv.setItem(14, createItem(Material.NETHER_STAR,
                                "§d§lUpgrade Factory",
                                Arrays.asList(
                                                "§7Current level: §e" + factoryLevel,
                                                "§7Click to upgrade")));

                inv.setItem(16, createItem(Material.ENDER_PEARL,
                                "§6§lFast Travel",
                                Arrays.asList(
                                                "§7Teleport to the factory")));

                // ------------------------------------------------------------------
                // Row 2 – customisation buttons (slots 28-34)
                // Slot 28: Set Display Name
                // Slot 30: Set Factory Icon
                // Slot 32: Toggle Border (player factories only)
                // Slot 34: Sell Factory
                // ------------------------------------------------------------------

                // Set Display Name
                String currentName = factory != null ? factory.getDisplayName() : playerFactory.getDisplayName();
                inv.setItem(28, createItem(Material.NAME_TAG,
                                "§e§lSet Display Name",
                                Arrays.asList(
                                                "§7Change the name displayed",
                                                "§7for this factory in menus.",
                                                "",
                                                "§7Current: §f" + (currentName != null ? currentName : "§o(default)"),
                                                "",
                                                "§eClick to rename!")));

                // Set Factory Icon
                String currentIcon = factory != null ? factory.getCustomIcon() : playerFactory.getCustomIcon();
                Material previewMat = currentIcon != null ? Material.matchMaterial(currentIcon) : null;
                if (previewMat == null)
                        previewMat = Material.ITEM_FRAME;
                inv.setItem(30, createItemWithTag(previewMat,
                                "§e§lSet Factory Icon",
                                Arrays.asList(
                                                "§7Change the icon material",
                                                "§7displayed in My Factories.",
                                                "",
                                                "§7Current: §f" + (currentIcon != null ? currentIcon : "§o(default)"),
                                                "",
                                                "§eLeft Click §7to set new icon",
                                                "§eRight Click §7to reset to default"),
                                "main_menu_action", "set_icon"));

                // Toggle Border – only for player-created factories
                if (plugin.getPlayerFactoryManager() != null
                                && plugin.getPlayerFactoryManager().getFactory(factoryId) != null) {
                        boolean borderOn = plugin.getBorderParticleTask() != null
                                        && plugin.getBorderParticleTask().isEnabled(player.getUniqueId());
                        inv.setItem(32, createItem(
                                        borderOn ? Material.GLOWSTONE : Material.GLASS,
                                        borderOn ? "§e§lToggle Border §a(ON)" : "§e§lToggle Border §c(OFF)",
                                        Arrays.asList(
                                                        "§7Show/hide your factory",
                                                        "§7region borders with particles.",
                                                        "",
                                                        "§7Status: " + (borderOn ? "§aEnabled" : "§cDisabled"),
                                                        "",
                                                        "§eClick to toggle!")));
                }

                // Sell Factory
                double factoryPrice = factory != null ? factory.getPrice() : playerFactory.getPrice();
                double sellValue = factoryPrice * plugin.getConfig().getDouble("factory.sell-price-multiplier", 0.5);
                inv.setItem(34, createItemWithTag(Material.GOLD_INGOT,
                                "§c§lSell Factory",
                                Arrays.asList(
                                                "§7Sell this factory back",
                                                "§7to the system.",
                                                "",
                                                "§7Original Price: §6$" + String.format("%.2f", factoryPrice),
                                                "§7You will receive: §a$" + String.format("%.2f", sellValue),
                                                "",
                                                "§cWarning: All stored items",
                                                "§cwill be lost!",
                                                "",
                                                "§eClick to sell!"),
                                "main_menu_action", "sell_factory"));

                // ------------------------------------------------------------------
                // Bottom bar – Back button (slot 45), Output Destination toggle (slot 52)
                // ------------------------------------------------------------------
                inv.setItem(45, createItem(Material.ARROW,
                                "§c§lBack",
                                Arrays.asList(
                                                "§7Return to My Factories")));

                double transferTaxRate = plugin.getConfig().getDouble("factory.transfer-owner-tax-rate", 5.0);
                double transferTax = factoryPrice * (transferTaxRate / 100.0);
                inv.setItem(50, createItemWithTag(Material.PLAYER_HEAD,
                                "§e§lTransfer Owner",
                                Arrays.asList(
                                                "§7Transfer this factory ownership",
                                                "§7to another active player.",
                                                "",
                                                "§7Transfer tax: §c" + String.format("%.2f", transferTaxRate) + "%",
                                                "§7Tax amount: §6$" + String.format("%.2f", transferTax),
                                                "",
                                                "§eClick to choose new owner"),
                                "main_menu_action", "transfer_owner_menu"));

                // Output destination toggle (slot 52)
                StorageManager.OutputDestination currentDest = plugin.getStorageManager()
                                .getOutputDestination(factoryId);
                boolean toInv = currentDest == StorageManager.OutputDestination.PLAYER_INVENTORY;
                inv.setItem(52, createItemWithTag(
                                toInv ? Material.PLAYER_HEAD : Material.CHEST,
                                toInv ? "§b§lOutput: §aInventory" : "§b§lOutput: §eStorage",
                                Arrays.asList(
                                                "§7Where finished products",
                                                "§7will be sent after production.",
                                                "",
                                                "§7Current: " + (toInv ? "§aPlayer Inventory" : "§eOutput Storage"),
                                                "",
                                                "§eClick to toggle!"),
                                "main_menu_action", "toggle_output_dest"));

                player.openInventory(inv);
        }

        private ItemStack createFactoryStatusItem(Factory factory,
                        com.aithor.factorycore.models.PlayerFactory playerFactory) {
                FactoryStatus status = factory != null ? factory.getStatus() : playerFactory.getStatus();
                int level = factory != null ? factory.getLevel() : playerFactory.getLevel();
                FactoryType type = factory != null ? factory.getType() : playerFactory.getType();
                ProductionTask production = factory != null ? factory.getCurrentProduction()
                                : playerFactory.getCurrentProduction();

                Material material = status == FactoryStatus.RUNNING ? Material.GREEN_WOOL : Material.RED_WOOL;

                List<String> lore = new ArrayList<>();
                lore.add("§7Status: " + status.getDisplay());
                lore.add("§7Level: §e" + level);
                lore.add("§7Type: " + type.getDisplayName());
                if (playerFactory != null) {
                        lore.add("§7Origin: §dPlayer-Created");
                }

                if (production != null) {
                        Recipe recipe = plugin.getRecipeManager().getRecipe(production.getRecipeId());
                        lore.add("");
                        lore.add("§6Active Production:");
                        if (recipe != null) {
                                lore.add("§e" + recipe.getName());
                                lore.add("§7Time remaining: §e" + production.getRemainingTime() + "s");
                                lore.add("§7Progress: §e" + (int) (production.getProgress() * 100) + "%");
                        }
                }

                return createItem(material, "§6§lFactory Status", lore);
        }

        // ------------------------------------------------------------------
        // Helpers
        // ------------------------------------------------------------------

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

        /**
         * Creates an item and tags it with a custom PDC key=value for action routing.
         */
        private ItemStack createItemWithTag(Material material, String name, List<String> lore,
                        String tagKey, String tagValue) {
                ItemStack item = createItem(material, name, lore);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                        meta.getPersistentDataContainer().set(
                                        new NamespacedKey(plugin, tagKey),
                                        PersistentDataType.STRING, tagValue);
                        item.setItemMeta(meta);
                }
                return item;
        }
}
