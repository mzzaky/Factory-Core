package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.managers.ResourceManager;
import com.aithor.factorycore.models.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Recipes Wiki GUI - Displays all registered resources with sorting,
 * filtering, and detailed recipe/usage info via sub-menus.
 * <p>
 * Data is read live from {@link ResourceManager} and
 * {@link com.aithor.factorycore.managers.RecipeManager},
 * so it automatically refreshes after {@code /fc admin reload}.
 */
public class RecipesWikiGUI {

    private final FactoryCore plugin;
    private final Player player;
    private int currentPage = 0;
    private SortMode sortMode = SortMode.NAME;

    // Sort modes
    public enum SortMode {
        NAME("Name", Material.NAME_TAG),
        PRICE("Price", Material.GOLD_INGOT),
        RARITY("Rarity", Material.DIAMOND),
        TYPE("Type", Material.CHEST),
        SOURCE("Source", Material.FURNACE);

        private final String display;
        private final Material icon;

        SortMode(String display, Material icon) {
            this.display = display;
            this.icon = icon;
        }

        public String getDisplay() {
            return display;
        }

        public Material getIcon() {
            return icon;
        }
    }

    // Rarity order for sorting
    private static final Map<String, Integer> RARITY_ORDER = new LinkedHashMap<>();
    static {
        RARITY_ORDER.put("common", 0);
        RARITY_ORDER.put("uncommon", 1);
        RARITY_ORDER.put("rare", 2);
        RARITY_ORDER.put("epic", 3);
    }

    public RecipesWikiGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openWikiMenu() {
        openWikiMenu(0);
    }

    public void openWikiMenu(int page) {
        this.currentPage = page;

        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.translateAlternateColorCodes('&', "&6&lRecipes Wiki &8- &ePage " + (page + 1)));

        // Border
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9)
            inv.setItem(i, border);
        for (int i = 17; i < 45; i += 9)
            inv.setItem(i, border);

        // Header info
        ItemStack header = createItem(Material.KNOWLEDGE_BOOK, "&6&lRecipes Wiki",
                Arrays.asList(
                        "&7Browse all registered resources",
                        "&7and their crafting recipes.",
                        "",
                        "&e Left Click &7- View how to craft",
                        "&e Right Click &7- View what it crafts",
                        "",
                        "&7Total Resources: &e" + plugin.getResourceManager().getAllResources().size()));
        inv.setItem(4, header);

        // Get sorted resources
        List<ResourceItem> sortedResources = getSortedResources();
        int totalPages = Math.max(1, (int) Math.ceil(sortedResources.size() / 21.0));
        if (page >= totalPages)
            this.currentPage = totalPages - 1;
        if (this.currentPage < 0)
            this.currentPage = 0;

        // Content slots: 7 per row, 3 rows (slots 10-16, 19-25, 28-34)
        int[] contentSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        int startIndex = this.currentPage * 21;
        for (int i = 0; i < contentSlots.length; i++) {
            int resIndex = startIndex + i;
            if (resIndex < sortedResources.size()) {
                ResourceItem resource = sortedResources.get(resIndex);
                inv.setItem(contentSlots[i], createResourceDisplayItem(resource));
            }
        }

        // Sort button (slot 47)
        ItemStack sortButton = createItem(sortMode.getIcon(),
                "&e&lSort: &f" + sortMode.getDisplay(),
                Arrays.asList(
                        "",
                        "&7Current: &e" + sortMode.getDisplay(),
                        "",
                        "&a\u25B6 Name" + (sortMode == SortMode.NAME ? " &7(Selected)" : ""),
                        "&6\u25B6 Price" + (sortMode == SortMode.PRICE ? " &7(Selected)" : ""),
                        "&b\u25B6 Rarity" + (sortMode == SortMode.RARITY ? " &7(Selected)" : ""),
                        "&d\u25B6 Type" + (sortMode == SortMode.TYPE ? " &7(Selected)" : ""),
                        "&c\u25B6 Source" + (sortMode == SortMode.SOURCE ? " &7(Selected)" : ""),
                        "",
                        "&eClick to cycle sort mode!"));
        stampPDC(sortButton, "wiki_action", "sort");
        inv.setItem(47, sortButton);

        // Stats/Info (slot 51)
        ItemStack statsItem = createItem(Material.PAPER, "&b&lStatistics",
                Arrays.asList(
                        "",
                        "&7Resources: &e" + sortedResources.size(),
                        "&7Recipes: &e" + plugin.getRecipeManager().getAllRecipes().size(),
                        "&7Page: &e" + (this.currentPage + 1) + "&7/&e" + totalPages,
                        "",
                        "&7Sort: &e" + sortMode.getDisplay()));
        inv.setItem(51, statsItem);

        // Navigation
        if (this.currentPage > 0) {
            ItemStack prevPage = createItem(Material.ARROW, "&a\u00AB Previous Page",
                    Arrays.asList("&7Go to page " + this.currentPage));
            stampPDC(prevPage, "wiki_action", "prev_page");
            inv.setItem(48, prevPage);
        }

        // Page indicator (slot 49)
        ItemStack pageInfo = createItem(Material.MAP,
                "&e&lPage " + (this.currentPage + 1) + " / " + totalPages,
                Arrays.asList("&7Showing " + Math.min(21, sortedResources.size() - startIndex)
                        + " of " + sortedResources.size() + " resources"));
        inv.setItem(49, pageInfo);

        if (this.currentPage < totalPages - 1) {
            ItemStack nextPage = createItem(Material.ARROW, "&a Next Page \u00BB",
                    Arrays.asList("&7Go to page " + (this.currentPage + 2)));
            stampPDC(nextPage, "wiki_action", "next_page");
            inv.setItem(50, nextPage);
        }

        // Back to hub (slot 45)
        ItemStack backButton = createItem(Material.DARK_OAK_DOOR, "&c&lBack to Hub",
                Arrays.asList("&7Return to the main menu"));
        stampPDC(backButton, "wiki_action", "back_to_hub");
        inv.setItem(45, backButton);

        // Close (slot 53)
        ItemStack closeButton = createItem(Material.BARRIER, "&c&lClose",
                Arrays.asList("&7Click to close"));
        stampPDC(closeButton, "wiki_action", "close");
        inv.setItem(53, closeButton);

        player.openInventory(inv);
    }

    /**
     * Opens the recipe detail sub-menu for a specific resource.
     * Shows how this item is crafted (recipe inputs/outputs).
     */
    public void openRecipeDetail(String resourceId) {
        ResourceItem resource = plugin.getResourceManager().getResource(resourceId);
        if (resource == null)
            return;

        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.translateAlternateColorCodes('&',
                        "&6&lRecipes Wiki &8- &eCrafting"));

        // Border
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9)
            inv.setItem(i, border);
        for (int i = 17; i < 45; i += 9)
            inv.setItem(i, border);

        // Header: the resource itself
        ItemStack headerItem = createResourceInfoItem(resource);
        inv.setItem(4, headerItem);

        // Title
        ItemStack titleItem = createItem(Material.CRAFTING_TABLE, "&a&lHow to Craft: " + resource.getName(),
                Arrays.asList(
                        "&7Recipes that produce this item",
                        "",
                        "&7Resource ID: &e" + resourceId,
                        "&7Type: &e" + formatType(resource.getType()),
                        "&7Rarity: " + getRarityColor(resource.getRarity()) + capitalize(resource.getRarity())));
        inv.setItem(2, titleItem);

        // Find recipes that output this resource
        List<Recipe> recipes = findRecipesProducing(resourceId);

        if (recipes.isEmpty()) {
            ItemStack noRecipe = createItem(Material.BARRIER, "&c&lNo Recipes Found",
                    Arrays.asList(
                            "&7This resource has no crafting recipe.",
                            "&7It may be obtained through other means."));
            inv.setItem(22, noRecipe);
        } else {
            int[] recipeSlots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
            int slotIdx = 0;

            for (Recipe recipe : recipes) {
                if (slotIdx >= recipeSlots.length)
                    break;
                inv.setItem(recipeSlots[slotIdx], createRecipeItem(recipe, resourceId));
                slotIdx++;
            }
        }

        // Back button
        ItemStack backButton = createItem(Material.ARROW, "&e&lBack to Wiki",
                Arrays.asList("&7Return to the wiki list"));
        stampPDC(backButton, "wiki_action", "back_to_wiki");
        inv.setItem(45, backButton);

        // Close
        ItemStack closeButton = createItem(Material.BARRIER, "&c&lClose",
                Arrays.asList("&7Click to close"));
        stampPDC(closeButton, "wiki_action", "close");
        inv.setItem(53, closeButton);

        player.openInventory(inv);
    }

    /**
     * Opens the usage detail sub-menu for a specific resource.
     * Shows what recipes use this item as an input.
     */
    public void openUsageDetail(String resourceId) {
        ResourceItem resource = plugin.getResourceManager().getResource(resourceId);
        if (resource == null)
            return;

        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.translateAlternateColorCodes('&',
                        "&6&lRecipes Wiki &8- &eUsages"));

        // Border
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9)
            inv.setItem(i, border);
        for (int i = 17; i < 45; i += 9)
            inv.setItem(i, border);

        // Header: the resource itself
        ItemStack headerItem = createResourceInfoItem(resource);
        inv.setItem(4, headerItem);

        // Title
        ItemStack titleItem = createItem(Material.ANVIL, "&b&lUsages of: " + resource.getName(),
                Arrays.asList(
                        "&7Recipes that use this item as input",
                        "",
                        "&7Resource ID: &e" + resourceId,
                        "&7Type: &e" + formatType(resource.getType()),
                        "&7Rarity: " + getRarityColor(resource.getRarity()) + capitalize(resource.getRarity())));
        inv.setItem(2, titleItem);

        // Find recipes that use this resource as input
        List<Recipe> recipes = findRecipesUsing(resourceId);

        if (recipes.isEmpty()) {
            ItemStack noUsage = createItem(Material.BARRIER, "&c&lNo Usages Found",
                    Arrays.asList(
                            "&7This resource is not used as",
                            "&7input in any recipe.",
                            "&7It may be a final product."));
            inv.setItem(22, noUsage);
        } else {
            int[] recipeSlots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
            int slotIdx = 0;

            for (Recipe recipe : recipes) {
                if (slotIdx >= recipeSlots.length)
                    break;
                inv.setItem(recipeSlots[slotIdx], createRecipeItem(recipe, null));
                slotIdx++;
            }
        }

        // Back button
        ItemStack backButton = createItem(Material.ARROW, "&e&lBack to Wiki",
                Arrays.asList("&7Return to the wiki list"));
        stampPDC(backButton, "wiki_action", "back_to_wiki");
        inv.setItem(45, backButton);

        // Close
        ItemStack closeButton = createItem(Material.BARRIER, "&c&lClose",
                Arrays.asList("&7Click to close"));
        stampPDC(closeButton, "wiki_action", "close");
        inv.setItem(53, closeButton);

        player.openInventory(inv);
    }

    // =========================================================================
    // Sorting
    // =========================================================================

    public void cycleSortMode() {
        SortMode[] modes = SortMode.values();
        int next = (sortMode.ordinal() + 1) % modes.length;
        sortMode = modes[next];
    }

    public SortMode getSortMode() {
        return sortMode;
    }

    private List<ResourceItem> getSortedResources() {
        List<ResourceItem> resources = new ArrayList<>(plugin.getResourceManager().getAllResources().values());

        switch (sortMode) {
            case NAME:
                resources.sort(Comparator.comparing(r -> ChatColor.stripColor(r.getName()).toLowerCase()));
                break;
            case PRICE:
                resources.sort(Comparator.comparingDouble(ResourceItem::getSellPrice).reversed());
                break;
            case RARITY:
                resources.sort(Comparator.comparingInt(r -> RARITY_ORDER.getOrDefault(r.getRarity().toLowerCase(), 0)));
                break;
            case TYPE:
                resources.sort(Comparator.comparing(r -> r.getType().name()));
                break;
            case SOURCE:
                resources.sort(Comparator.comparing(r -> getSourceFactory(r.getId())));
                break;
        }

        return resources;
    }

    // =========================================================================
    // Recipe lookup helpers
    // =========================================================================

    private List<Recipe> findRecipesProducing(String resourceId) {
        List<Recipe> result = new ArrayList<>();
        for (Recipe recipe : plugin.getRecipeManager().getAllRecipes().values()) {
            if (recipe.getOutputs().containsKey(resourceId)) {
                result.add(recipe);
            }
        }
        return result;
    }

    private List<Recipe> findRecipesUsing(String resourceId) {
        List<Recipe> result = new ArrayList<>();
        for (Recipe recipe : plugin.getRecipeManager().getAllRecipes().values()) {
            if (recipe.getInputs().containsKey(resourceId)) {
                result.add(recipe);
            }
        }
        return result;
    }

    /**
     * Determines which factory type produces a given resource.
     */
    private String getSourceFactory(String resourceId) {
        for (Recipe recipe : plugin.getRecipeManager().getAllRecipes().values()) {
            if (recipe.getOutputs().containsKey(resourceId)) {
                FactoryType ft = FactoryType.fromId(recipe.getFactoryType());
                return ft != null ? ChatColor.stripColor(ft.getDisplayName()) : recipe.getFactoryType();
            }
        }
        return "Unknown";
    }

    // =========================================================================
    // Item builders
    // =========================================================================

    private ItemStack createResourceDisplayItem(ResourceItem resource) {
        Material mat;
        try {
            mat = Material.valueOf(resource.getMaterial());
        } catch (IllegalArgumentException e) {
            mat = Material.STONE;
        }

        String source = getSourceFactory(resource.getId());
        String rarityDisplay = getRarityColor(resource.getRarity()) + capitalize(resource.getRarity());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Rarity: " + rarityDisplay);
        lore.add("&7Type: &e" + formatType(resource.getType()));
        lore.add("&7Price: &6$" + String.format("%.2f", resource.getSellPrice()));
        lore.add("&7Source: &a" + source);

        // Show production time if available
        List<Recipe> producing = findRecipesProducing(resource.getId());
        if (!producing.isEmpty()) {
            Recipe primary = producing.get(0);
            lore.add("&7Production Time: &b" + formatTime(primary.getProductionTime()));
        }

        lore.add("");
        lore.add("&e Left Click &8- &7View crafting recipe");
        lore.add("&e Right Click &8- &7View usages");

        ItemStack item = createItem(mat, resource.getName(), lore);

        // Set custom model data if present
        if (resource.getCustomModelData() > 0) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(resource.getCustomModelData());
                item.setItemMeta(meta);
            }
        }

        // Add glow if enabled
        if (resource.isGlow()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }

        // Stamp resource ID for click handling
        stampPDC(item, "wiki_resource_id", resource.getId());

        return item;
    }

    private ItemStack createResourceInfoItem(ResourceItem resource) {
        Material mat;
        try {
            mat = Material.valueOf(resource.getMaterial());
        } catch (IllegalArgumentException e) {
            mat = Material.STONE;
        }

        String source = getSourceFactory(resource.getId());
        String rarityDisplay = getRarityColor(resource.getRarity()) + capitalize(resource.getRarity());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&8\u2502 &7ID: &f" + resource.getId());
        lore.add("&8\u2502 &7Rarity: " + rarityDisplay);
        lore.add("&8\u2502 &7Type: &e" + formatType(resource.getType()));
        lore.add("&8\u2502 &7Price: &6$" + String.format("%.2f", resource.getSellPrice()));
        lore.add("&8\u2502 &7Source: &a" + source);

        List<Recipe> producing = findRecipesProducing(resource.getId());
        if (!producing.isEmpty()) {
            Recipe primary = producing.get(0);
            lore.add("&8\u2502 &7Production Time: &b" + formatTime(primary.getProductionTime()));
            lore.add("&8\u2502 &7Production Cost: &6$" + String.format("%.2f", primary.getMoneyCost()));
        }

        // Integration info
        if (resource.isMMOItem()) {
            lore.add("");
            lore.add("&8\u2502 &dMMOItems: &f" + resource.getMMOItemsType() + "/" + resource.getMMOItemsId());
        }
        if (resource.isExecutableItem()) {
            lore.add("");
            lore.add("&8\u2502 &dExecutableItems: &f" + resource.getExecutableItemsId());
        }

        ItemStack item = createItem(mat, "&e&l" + ChatColor.stripColor(resource.getName()), lore);

        if (resource.getCustomModelData() > 0) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(resource.getCustomModelData());
                item.setItemMeta(meta);
            }
        }
        if (resource.isGlow()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    private ItemStack createRecipeItem(Recipe recipe, String highlightResourceId) {
        Material iconMat;
        try {
            iconMat = Material.valueOf(recipe.getIcon());
        } catch (IllegalArgumentException e) {
            iconMat = Material.PAPER;
        }

        FactoryType ft = FactoryType.fromId(recipe.getFactoryType());
        String factoryName = ft != null ? ft.getDisplayName() : "&7" + recipe.getFactoryType();

        List<String> lore = new ArrayList<>();
        lore.add("&8Factory: " + factoryName);
        lore.add("&8Production Time: &b" + formatTime(recipe.getProductionTime()));
        lore.add("&8Cost: &6$" + String.format("%.2f", recipe.getMoneyCost()));
        lore.add("");

        // Inputs
        lore.add("&c&lInputs:");
        for (Map.Entry<String, Integer> entry : recipe.getInputs().entrySet()) {
            String inputName = getInputDisplayName(entry.getKey());
            String highlight = (entry.getKey().equals(highlightResourceId)) ? "&e" : "&7";
            lore.add("  " + highlight + "\u2022 " + inputName + " &7x" + entry.getValue());
        }
        lore.add("");

        // Outputs
        lore.add("&a&lOutputs:");
        for (Map.Entry<String, Integer> entry : recipe.getOutputs().entrySet()) {
            ResourceItem outRes = plugin.getResourceManager().getResource(entry.getKey());
            String outName = outRes != null ? outRes.getName() : "&f" + entry.getKey();
            String highlight = (entry.getKey().equals(highlightResourceId)) ? "&e" : "&7";
            lore.add("  " + highlight + "\u2022 " + outName + " &7x" + entry.getValue());
        }

        if (recipe.isDisableItemOutput()) {
            lore.add("");
            lore.add("&8&oItem output disabled (command rewards only)");
        }

        return createItem(iconMat, recipe.getName(), lore);
    }

    // =========================================================================
    // Utility methods
    // =========================================================================

    private String getInputDisplayName(String inputKey) {
        if (ResourceManager.isVanillaInput(inputKey)) {
            return "&f" + ResourceManager.getVanillaDisplayName(inputKey);
        }
        ResourceItem res = plugin.getResourceManager().getResource(inputKey);
        return res != null ? res.getName() : "&f" + inputKey;
    }

    private String formatType(ResourceType type) {
        switch (type) {
            case RAW_RESOURCES:
                return "Raw Resources";
            case ADVANCED_RESOURCES:
                return "Advanced Resources";
            case FINAL_RESOURCES:
                return "Final Resources";
            case FUEL:
                return "Fuel";
            case MACHINE_PARTS:
                return "Machine Parts";
            default:
                return type.name();
        }
    }

    private String formatTime(int seconds) {
        if (seconds < 60)
            return seconds + "s";
        int minutes = seconds / 60;
        int secs = seconds % 60;
        if (secs == 0)
            return minutes + "m";
        return minutes + "m " + secs + "s";
    }

    private String getRarityColor(String rarity) {
        if (rarity == null)
            return "&f";
        switch (rarity.toLowerCase()) {
            case "uncommon":
                return "&a";
            case "rare":
                return "&9";
            case "epic":
                return "&5";
            case "common":
            default:
                return "&f";
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty())
            return "Common";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null)
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore != null) {
                List<String> colored = new ArrayList<>();
                for (String line : lore) {
                    colored.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(colored);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void stampPDC(ItemStack item, String key, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, key),
                    PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }
}
