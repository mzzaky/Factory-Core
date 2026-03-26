package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import com.aithor.factorycore.managers.TaxManager;

/**
 * Main Hub GUI - Central menu for all FactoryCore features.
 * <p>
 * All visual properties (title, materials, names, lore, slots, sounds) are
 * driven by {@code custom_gui/main_menu.yml}. Hard-coded defaults kick in
 * automatically whenever a config key is absent, so the menu always works
 * even if the admin has not touched the config file.
 */
public class HubGUI {

    private final FactoryCore plugin;
    private final Player player;

    public HubGUI(FactoryCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    public void openHubMenu() {
        FileConfiguration cfg = plugin.getMainMenuConfig();

        // ── GUI title & size ──────────────────────────────────────────────────
        String title = color(cfg.getString("gui.title", "&6&lFactoryCore &8- &eMain Hub"));
        int size = cfg.getInt("gui.size", 54);
        if (size % 9 != 0 || size < 9 || size > 54)
            size = 54;

        Inventory inv = Bukkit.createInventory(null, size, title);

        // ── Border ────────────────────────────────────────────────────────────
        if (cfg.getBoolean("gui.border.enabled", true)) {
            String borderMaterialName = cfg.getString("gui.border.material",
                    plugin.getConfig().getString("gui.border-item", "BLACK_STAINED_GLASS_PANE"));
            String borderName = cfg.getString("gui.border.name", " ");
            int borderCmd = cfg.getInt("gui.border.custom_model_data", 0);

            Material borderMat = parseMaterial(borderMaterialName, Material.BLACK_STAINED_GLASS_PANE);
            ItemStack border = createItem(borderMat, color(borderName), null, borderCmd);

            for (int i = 0; i < 9; i++)
                inv.setItem(i, border);
            for (int i = 45; i < size; i++)
                inv.setItem(i, border);
            for (int i = 9; i < 45; i += 9)
                inv.setItem(i, border);
            for (int i = 17; i < 45; i += 9)
                inv.setItem(i, border);
        }

        Map<String, String> globalPh = buildGlobalPlaceholders();

        // ── Player Info Header ────────────────────────────────────────────────
        if (isItemEnabled(cfg, "player_info"))
            setItems(inv, parseSlots(cfg, "items.player_info.slot", 4), createPlayerInfoItem(cfg, globalPh));

        // ── Factory Browse ────────────────────────────────────────────────────
        if (isItemEnabled(cfg, "factory_browse")) {
            Map<String, String> ph = new HashMap<>(globalPh);
            ph.put("{available_count}", globalPh.get("{factory_available_count}"));
            setItems(inv, parseSlots(cfg, "items.factory_browse.slot", 20),
                    buildItem(cfg, "factory_browse", ph,
                            Material.COMPASS, "&a&lFactory Browse",
                            Arrays.asList(
                                    "&7Browse and purchase",
                                    "&7available factories",
                                    "",
                                    "&eAvailable: &a" + ph.get("{available_count}") + " factories",
                                    "",
                                    "&7Click to browse!")));
        }

        // ── My Factories ──────────────────────────────────────────────────────
        if (isItemEnabled(cfg, "my_factories")) {
            Map<String, String> ph = new HashMap<>(globalPh);
            ph.put("{owned_count}", globalPh.get("{factory_owned_count}"));
            ph.put("{running_count}", globalPh.get("{factory_running_count}"));
            ph.put("{stopped_count}", globalPh.get("{factory_stopped_count}"));
            setItems(inv, parseSlots(cfg, "items.my_factories.slot", 21),
                    buildItem(cfg, "my_factories", ph,
                            Material.SMITHING_TABLE, "&6&lMy Factories",
                            Arrays.asList(
                                    "&7Manage your factories",
                                    "",
                                    "&eOwned: &6" + ph.get("{owned_count}") + " factories",
                                    "&eRunning: &a" + ph.get("{running_count}"),
                                    "&eStopped: &c" + ph.get("{stopped_count}"),
                                    "",
                                    "&7Click to manage!")));
        }

        // ── Invoice Center ────────────────────────────────────────────────────
        long overdueInvoices = Long.parseLong(globalPh.getOrDefault("{invoice_overdue_count}", "0"));
        Material invoiceMat;
        boolean forceMaterial = cfg.getBoolean("items.invoice_center.force_material", false);
        if (forceMaterial) {
            invoiceMat = parseMaterial(cfg.getString("items.invoice_center.material", "PAPER"), Material.PAPER);
        } else {
            invoiceMat = overdueInvoices > 0 ? Material.RED_CONCRETE : Material.PAPER;
        }

        if (isItemEnabled(cfg, "invoice_center")) {
            Map<String, String> ph = new HashMap<>(globalPh);
            ph.put("{invoice_count}", globalPh.get("{invoice_count}"));
            ph.put("{overdue_count}", globalPh.get("{invoice_overdue_count}"));
            ph.put("{total_due}", globalPh.get("{invoice_total_due}"));
            ph.put("{overdue_warning}", globalPh.get("{invoice_overdue_warning}"));
            setItems(inv, parseSlots(cfg, "items.invoice_center.slot", 22),
                    buildItemWithMaterial(cfg, "invoice_center", ph,
                            invoiceMat, "&e&lInvoice Center",
                            Arrays.asList(
                                    "&7View and pay all invoices",
                                    "",
                                    "&ePending: &6" + ph.get("{invoice_count}") + " invoices",
                                    "&eOverdue: &c" + ph.get("{overdue_count}"),
                                    "&eTotal Due: &6$" + ph.get("{total_due}"),
                                    "",
                                    ph.get("{overdue_warning}"),
                                    "",
                                    "&7Click to manage!")));
        }

        // ── Distribution Center ───────────────────────────────────────────────
        if (isItemEnabled(cfg, "distribution_center")) {
            Map<String, String> ph = new HashMap<>(globalPh);
            ph.put("{active_count}", globalPh.get("{distribution_active_count}"));
            setItems(inv, parseSlots(cfg, "items.distribution_center.slot", 23),
                    buildItem(cfg, "distribution_center", ph,
                            Material.CHEST_MINECART, "&6&lDistribution Center",
                            Arrays.asList(
                                    "&7Fulfill delivery requests from",
                                    "&7companies for money rewards!",
                                    "",
                                    "&eActive Requests: &6" + ph.get("{active_count}"),
                                    "",
                                    "&7Click to open!")));
        }

        // ── Employees Center ──────────────────────────────────────────────────
        if (isItemEnabled(cfg, "employees_center")) {
            Map<String, String> ph = new HashMap<>(globalPh);
            ph.put("{assigned_count}", globalPh.get("{employee_assigned_count}"));
            ph.put("{owned_count}", globalPh.get("{employee_owned_count}"));
            ph.put("{unassigned_line}", globalPh.get("{employee_unassigned_line}"));
            setItems(inv, parseSlots(cfg, "items.employees_center.slot", 24),
                    buildItem(cfg, "employees_center", ph,
                            Material.VILLAGER_SPAWN_EGG, "&b&lEmployees Center",
                            Arrays.asList(
                                    "&7Manage factory employees",
                                    "",
                                    "&eAssigned Employees: &b" + ph.get("{assigned_count}"),
                                    "&eOwned (total): &b" + ph.get("{owned_count}"),
                                    ph.get("{unassigned_line}"),
                                    "",
                                    "&7Click to manage!")));
        }

        // ── Marketplace ───────────────────────────────────────────────────────
        if (isItemEnabled(cfg, "marketplace")) {
            Map<String, String> ph = new HashMap<>(globalPh);
            ph.put("{listing_count}", globalPh.get("{marketplace_listing_count}"));
            setItems(inv, parseSlots(cfg, "items.marketplace.slot", 29),
                    buildItem(cfg, "marketplace", ph,
                            Material.EMERALD, "&2&lMarketplace",
                            Arrays.asList(
                                    "&7Buy and sell resources",
                                    "",
                                    "&eYour Listings: &2" + ph.get("{listing_count}"),
                                    "",
                                    "&7Click to browse!")));
        }

        // ── Achievements ──────────────────────────────────────────────────────
        if (isItemEnabled(cfg, "achievements")) {
            Map<String, String> ph = new HashMap<>(globalPh);
            ph.put("{unlocked_count}", globalPh.get("{achievement_unlocked_count}"));
            ph.put("{total_count}", globalPh.get("{achievement_total_count}"));
            setItems(inv, parseSlots(cfg, "items.achievements.slot", 30),
                    buildItem(cfg, "achievements", ph,
                            Material.DIAMOND, "&6&lAchievements",
                            Arrays.asList(
                                    "&7View your achievements",
                                    "&7and track progress",
                                    "",
                                    "&eUnlocked: &6" + ph.get("{unlocked_count}") + "&7/&6" + ph.get("{total_count}"),
                                    "",
                                    "&7Click to view!")));
        }

        // ── Research Center ───────────────────────────────────────────────────
        if (isItemEnabled(cfg, "research_center")) {
            Map<String, String> ph = new HashMap<>(globalPh);
            ph.put("{active_count}", globalPh.get("{research_active_count}"));
            ph.put("{completed_count}", globalPh.get("{research_completed_count}"));
            setItems(inv, parseSlots(cfg, "items.research_center.slot", 31),
                    buildItem(cfg, "research_center", ph,
                            Material.ENCHANTING_TABLE, "&5&lResearch Center",
                            Arrays.asList(
                                    "&7Unlock powerful buffs",
                                    "&7through R&D technology",
                                    "",
                                    "&eActive Research: &d" + ph.get("{active_count}"),
                                    "&eCompleted Levels: &a" + ph.get("{completed_count}"),
                                    "",
                                    "&7Click to research!")));
        }

        // ── Daily Quests ──────────────────────────────────────────────────────
        if (isItemEnabled(cfg, "daily_quests")) {
            Map<String, String> ph = new HashMap<>(globalPh);
            ph.put("{completed_count}", globalPh.get("{quest_completed_count}"));
            ph.put("{total_count}", globalPh.get("{quest_total_count}"));
            ph.put("{reset_time}", globalPh.get("{quest_reset_time}"));
            setItems(inv, parseSlots(cfg, "items.daily_quests.slot", 32),
                    buildItem(cfg, "daily_quests", ph,
                            Material.WRITABLE_BOOK, "&a&lDaily Quests",
                            Arrays.asList(
                                    "&7Complete daily missions",
                                    "&7for EXP and money rewards!",
                                    "",
                                    "&eCompleted: &a" + ph.get("{completed_count}") + "&7/&a" + ph.get("{total_count}"),
                                    "&eResets in: &b" + ph.get("{reset_time}"),
                                    "",
                                    "&7Click to view quests!")));
        }

        // ── Help & Info ───────────────────────────────────────────────────────
        if (isItemEnabled(cfg, "help_info"))
            setItems(inv, parseSlots(cfg, "items.help_info.slot", 33),
                    buildItem(cfg, "help_info", globalPh,
                            Material.BOOK, "&d&lHelp & Info",
                            Arrays.asList(
                                    "&7Learn about FactoryCore",
                                    "",
                                    "&7• Plugin guide",
                                    "&7• Commands reference",
                                    "&7• Tips & tricks",
                                    "",
                                    "&7Click to learn more!")));

        // ── Recipes Wiki ─────────────────────────────────────────────────────
        if (isItemEnabled(cfg, "recipes_wiki")) {
            Map<String, String> wikiPh = new HashMap<>(globalPh);
            int resourceCount = plugin.getResourceManager().getAllResources().size();
            int recipeCount = plugin.getRecipeManager().getAllRecipes().size();
            wikiPh.put("{resource_count}", String.valueOf(resourceCount));
            wikiPh.put("{recipe_count}", String.valueOf(recipeCount));
            setItems(inv, parseSlots(cfg, "items.recipes_wiki.slot", 40),
                    buildItem(cfg, "recipes_wiki", wikiPh,
                            Material.KNOWLEDGE_BOOK, "&6&lRecipes Wiki",
                            Arrays.asList(
                                    "&7Browse all resources and",
                                    "&7their crafting recipes",
                                    "",
                                    "&eResources: &6" + wikiPh.get("{resource_count}"),
                                    "&eRecipes: &6" + wikiPh.get("{recipe_count}"),
                                    "",
                                    "&7Click to browse!")));
        }

        // ── Quick Stats ───────────────────────────────────────────────────────
        if (isItemEnabled(cfg, "quick_stats"))
            setItems(inv, parseSlots(cfg, "items.quick_stats.slot", 49), createQuickStatsItem(cfg, globalPh));

        // ── Close Button ──────────────────────────────────────────────────────
        if (isItemEnabled(cfg, "close_button"))
            setItems(inv, parseSlots(cfg, "items.close_button.slot", 53),
                    buildItem(cfg, "close_button", globalPh,
                            Material.BARRIER, "&c&lClose",
                            Arrays.asList("&7Click to close")));

        // ── Custom Items ──────────────────────────────────────────────────────
        if (cfg.contains("items")) {
            List<String> coreKeys = Arrays.asList(
                    "player_info", "factory_browse", "my_factories", "invoice_center",
                    "distribution_center", "employees_center", "marketplace", "achievements",
                    "research_center", "daily_quests", "help_info", "recipes_wiki",
                    "quick_stats", "close_button");
            for (String key : cfg.getConfigurationSection("items").getKeys(false)) {
                if (!coreKeys.contains(key) && isItemEnabled(cfg, key)) {
                    if (cfg.contains("items." + key + ".slot")) {
                        setItems(inv, parseSlots(cfg, "items." + key + ".slot", -1),
                                buildItem(cfg, key, globalPh,
                                        Material.PAPER, "&f" + key,
                                        Collections.emptyList()));
                    }
                }
            }
        }

        player.openInventory(inv);
    }

    // =========================================================================
    // Sound Helpers (called by HubClickListener)
    // =========================================================================

    /**
     * Play the configured click sound for an item key, falling back to the
     * {@code gui.default_click_sound} section when the item has no individual
     * sound configured.
     *
     * @param itemKey config key under {@code items.*} (e.g. "factory_browse")
     */
    public void playClickSound(String itemKey) {
        FileConfiguration cfg = plugin.getMainMenuConfig();

        // Try item-specific sound first
        String base = "items." + itemKey + ".sound";
        if (cfg.contains(base + ".enabled")) {
            if (!cfg.getBoolean(base + ".enabled", false))
                return;
            playSound(
                    cfg.getString(base + ".sound", "UI_BUTTON_CLICK"),
                    (float) cfg.getDouble(base + ".volume", 0.5),
                    (float) cfg.getDouble(base + ".pitch", 1.0));
            return;
        }

        // Fall back to default click sound
        String def = "gui.default_click_sound";
        if (!cfg.getBoolean(def + ".enabled", true))
            return;
        playSound(
                cfg.getString(def + ".sound", "UI_BUTTON_CLICK"),
                (float) cfg.getDouble(def + ".volume", 0.5),
                (float) cfg.getDouble(def + ".pitch", 1.0));
    }

    /** Returns the first configured slot for a given item key. */
    public int getItemSlot(String itemKey) {
        // Fallback slot map mirrors defaults in openHubMenu()
        Map<String, Integer> defaults = new HashMap<>();
        defaults.put("player_info", 4);
        defaults.put("factory_browse", 20);
        defaults.put("my_factories", 21);
        defaults.put("invoice_center", 22);
        defaults.put("distribution_center", 23);
        defaults.put("employees_center", 24);
        defaults.put("marketplace", 29);
        defaults.put("achievements", 30);
        defaults.put("research_center", 31);
        defaults.put("daily_quests", 32);
        defaults.put("help_info", 33);
        defaults.put("recipes_wiki", 40);
        defaults.put("quick_stats", 49);
        defaults.put("close_button", 53);
        int fallback = defaults.getOrDefault(itemKey, -1);
        List<Integer> slots = parseSlots(plugin.getMainMenuConfig(), "items." + itemKey + ".slot", fallback);
        return slots.isEmpty() ? fallback : slots.get(0);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Parse the {@code slot} config value for a given config path.
     * <p>
     * Supports both single-integer and multi-slot formats:
     * 
     * <pre>
     *   slot: 20              # single slot
     *   slot: "20, 21, 22"    # multi-slot (comma-separated string)
     *   slot:                 # falls back to defaultSlot
     *     - 20
     *     - 21
     * </pre>
     *
     * @param cfg         the config to read from
     * @param path        YAML path to the slot value (e.g.
     *                    "items.factory_browse.slot")
     * @param defaultSlot fallback slot when the config key is absent or unparseable
     * @return list of resolved slot indices; never null, never empty (contains at
     *         least {@code defaultSlot})
     */
    private List<Integer> parseSlots(FileConfiguration cfg, String path, int defaultSlot) {
        // Case 1: YAML list (slot:\n - 20\n - 21)
        if (cfg.isList(path)) {
            List<Integer> result = new ArrayList<>();
            for (Object obj : cfg.getList(path, Collections.emptyList())) {
                try {
                    int s = Integer.parseInt(obj.toString().trim());
                    if (s >= 0 && s < 54)
                        result.add(s);
                } catch (NumberFormatException ignored) {
                }
            }
            return result.isEmpty() ? Collections.singletonList(defaultSlot) : result;
        }

        // Case 2: plain integer (slot: 20)
        if (cfg.isInt(path)) {
            return Collections.singletonList(cfg.getInt(path, defaultSlot));
        }

        // Case 3: string — single or comma-separated (slot: "20" or slot: "20, 21, 22")
        String raw = cfg.getString(path);
        if (raw != null && !raw.isBlank()) {
            List<Integer> result = new ArrayList<>();
            for (String part : raw.split(",")) {
                try {
                    int s = Integer.parseInt(part.trim());
                    if (s >= 0 && s < 54)
                        result.add(s);
                } catch (NumberFormatException ignored) {
                }
            }
            return result.isEmpty() ? Collections.singletonList(defaultSlot) : result;
        }

        // Case 4: key not set at all
        return Collections.singletonList(defaultSlot);
    }

    /**
     * Place {@code item} in every slot returned by {@link #parseSlots}.
     *
     * @param inv   target inventory
     * @param slots list of slot indices
     * @param item  the item to place
     */
    private void setItems(Inventory inv, List<Integer> slots, ItemStack item) {
        int size = inv.getSize();
        for (int slot : slots) {
            if (slot >= 0 && slot < size) {
                inv.setItem(slot, item);
            }
        }
    }

    /**
     * Returns {@code true} if the item should be rendered in the GUI.
     * <p>
     * An item is considered <em>disabled</em> when either:
     * <ul>
     * <li>its entire section is absent from {@code custom_gui/main_menu.yml}
     * (i.e. {@code items.<key>} does not exist), OR</li>
     * <li>{@code items.<key>.enabled} is explicitly set to {@code false}.</li>
     * </ul>
     * If the section is present and {@code enabled} is not defined, the item
     * is shown by default.
     *
     * @param cfg the loaded main_menu config
     * @param key the item key under {@code items.*}
     * @return {@code true} when the item should be placed
     */
    private boolean isItemEnabled(FileConfiguration cfg, String key) {
        // Section missing entirely → hide the item
        if (!cfg.contains("items." + key))
            return false;
        // Section present but explicitly disabled → hide the item
        return cfg.getBoolean("items." + key + ".enabled", true);
    }

    private ItemStack buildItem(FileConfiguration cfg, String key,
            Map<String, String> placeholders,
            Material fallbackMaterial, String fallbackName,
            List<String> fallbackLore) {
        Material mat = parseMaterial(cfg.getString("items." + key + ".material"), fallbackMaterial);
        return buildItemWithMaterial(cfg, key, placeholders, mat, fallbackName, fallbackLore);
    }

    /**
     * Build an item using an explicit (already-resolved) material.
     * The name and lore are pulled from config; {@code fallback*} values are
     * used when the config does not define them.
     * Placeholders in name and lore are substituted using the provided map.
     * <p>
     * Every item produced here receives a stable {@code hub_gui_id} PDC tag equal
     * to {@code key} so that the click listener can route by ID, not display name.
     */
    private ItemStack buildItemWithMaterial(FileConfiguration cfg, String key,
            Map<String, String> placeholders,
            Material material, String fallbackName,
            List<String> fallbackLore) {
        String cfgName = cfg.getString("items." + key + ".name");
        String displayName = cfgName != null
                ? color(applyPlaceholders(cfgName, placeholders))
                : color(applyPlaceholders(fallbackName, placeholders));

        List<String> loreCfg = cfg.getStringList("items." + key + ".lore");
        List<String> lore;
        if (loreCfg != null && !loreCfg.isEmpty()) {
            lore = new ArrayList<>();
            for (String line : loreCfg)
                lore.add(color(applyPlaceholders(line, placeholders)));
        } else {
            lore = new ArrayList<>();
            for (String line : fallbackLore)
                lore.add(color(applyPlaceholders(line, placeholders)));
        }

        int cmd = cfg.getInt("items." + key + ".custom_model_data", 0);
        ItemStack item = createItem(material, displayName, lore, cmd);
        stampGuiId(key, item);
        stampOnClick(cfg, key, item);
        return item;
    }

    /**
     * Stamps a stable {@code hub_gui_id} tag on every main-menu item so that
     * {@link com.aithor.factorycore.listeners.HubClickListener} can route clicks
     * by config key rather than by display name.
     *
     * @param key  the item key under {@code items.*} (e.g. "factory_browse")
     * @param item the ItemStack to stamp (modified in-place)
     */
    private void stampGuiId(String key, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "hub_gui_id"),
                PersistentDataType.STRING, key);
        item.setItemMeta(meta);
    }

    /**
     * If the config section {@code items.<key>} defines an {@code on_click} field,
     * write its type and {@code value} into the item's PersistentDataContainer so
     * that {@link com.aithor.factorycore.listeners.HubClickListener} can read and
     * execute the action when the player clicks the item.
     *
     * <p>
     * Supported {@code on_click} types:
     * <ul>
     * <li>{@code console_command} – run {@code value} as a console command;
     * {@code %player%} is replaced with the clicker's name at click-time.
     * Multiple commands can be separated by {@code ;} (semicolon).</li>
     * <li>{@code open_gui_factory_browse} – open Factory Browse GUI.</li>
     * <li>{@code open_gui_my_factories} – open My Factories GUI.</li>
     * <li>{@code open_gui_invoice_center} – open Invoice Center GUI.</li>
     * <li>{@code open_gui_tax_center} – open Tax Center GUI.</li>
     * <li>{@code open_gui_employees_center} – open Employees Center GUI.</li>
     * <li>{@code open_gui_marketplace} – open Marketplace GUI.</li>
     * <li>{@code open_gui_achievements} – open Achievements GUI.</li>
     * <li>{@code open_gui_research_center} – open Research Center GUI.</li>
     * <li>{@code open_gui_daily_quests} – open Daily Quests GUI.</li>
     * <li>{@code open_gui_help_info} – open Help &amp; Info GUI.</li>
     * </ul>
     *
     * @param cfg  the main_menu config
     * @param key  the item key under {@code items.*}
     * @param item the ItemStack to stamp (modified in-place)
     */
    private void stampOnClick(FileConfiguration cfg, String key, ItemStack item) {
        String action = cfg.getString("items." + key + ".on_click");
        if (action == null || action.isBlank())
            return;
        String value = cfg.getString("items." + key + ".value", "");

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "hub_on_click"),
                PersistentDataType.STRING, action.toLowerCase());
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "hub_on_click_value"),
                PersistentDataType.STRING, value);
        item.setItemMeta(meta);
    }

    /**
     * Replaces all placeholder keys in {@code text} with the corresponding
     * values from {@code placeholders}. Keys should be in {@code {key}} format.
     */
    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null)
            return "";
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }

    /** Creates the player-head item for the info header slot. */
    private ItemStack createPlayerInfoItem(FileConfiguration cfg, Map<String, String> globalPh) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);

            Map<String, String> localPh = new HashMap<>(globalPh);
            localPh.put("{owned_count}", globalPh.get("{factory_owned_count}"));
            localPh.put("{total_value}", globalPh.get("{factory_total_value}"));

            // Name
            String cfgName = cfg.getString("items.player_info.name");
            if (cfgName != null) {
                meta.setDisplayName(color(applyPlaceholders(cfgName, localPh)));
            } else {
                meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + player.getName());
            }

            // Lore
            List<String> loreCfg = cfg.getStringList("items.player_info.lore");
            List<String> lore = new ArrayList<>();
            if (loreCfg != null && !loreCfg.isEmpty()) {
                for (String line : loreCfg) {
                    lore.add(color(applyPlaceholders(line, localPh)));
                }
            } else {
                lore.add(color("&7Welcome to FactoryCore!"));
                lore.add("");
                lore.add(color("&eFactories Owned: &6" + localPh.get("{owned_count}")));
                lore.add(color("&eTotal Value: &6$" + localPh.get("{total_value}")));
            }
            meta.setLore(lore);
            skull.setItemMeta(meta);
        }
        stampGuiId("player_info", skull);
        stampOnClick(cfg, "player_info", skull);
        return skull;
    }

    /** Creates the Quick Stats item. */
    private ItemStack createQuickStatsItem(FileConfiguration cfg, Map<String, String> globalPh) {
        Map<String, String> localPh = new HashMap<>(globalPh);
        localPh.put("{factory_count}", globalPh.get("{factory_owned_count}"));
        localPh.put("{running_count}", globalPh.get("{factory_running_count}"));
        localPh.put("{total_value}", globalPh.get("{factory_total_value}"));

        // Config overrides for material, name, and custom_model_data
        Material mat = parseMaterial(cfg.getString("items.quick_stats.material"), Material.NETHER_STAR);
        String cfgName = cfg.getString("items.quick_stats.name");
        String displayName = cfgName != null
                ? color(applyPlaceholders(cfgName, localPh))
                : color("&e&lQuick Stats");
        int cmd = cfg.getInt("items.quick_stats.custom_model_data", 0);

        // Lore: prefer config lore (with placeholders), else use dynamic built-in lore
        List<String> loreCfg = cfg.getStringList("items.quick_stats.lore");
        List<String> lore;
        if (loreCfg != null && !loreCfg.isEmpty()) {
            lore = new ArrayList<>();
            for (String line : loreCfg)
                lore.add(color(applyPlaceholders(line, localPh)));
        } else {
            // Dynamic lore
            lore = new ArrayList<>();
            lore.add(color("&7Your quick statistics"));
            lore.add("");
            lore.add(color("&eFactories: &6" + localPh.get("{factory_count}")));
            lore.add(color("&eRunning: &a" + localPh.get("{running_count}")));
            lore.add(color("&eTotal Value: &6$" + localPh.get("{total_value}")));

            // Factory type breakdown
            List<Factory> ownedAdmin = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());
            List<PlayerFactory> ownedPlayer = plugin.getPlayerFactoryManager() != null
                    ? new ArrayList<>(plugin.getPlayerFactoryManager().getAllFactories()).stream()
                            .filter(pf -> pf.getOwner().equals(player.getUniqueId())).toList()
                    : new ArrayList<>();

            if (!ownedAdmin.isEmpty() || !ownedPlayer.isEmpty()) {
                lore.add("");
                lore.add(color("&7Factory Types:"));
                Map<FactoryType, Long> typeCounts = new HashMap<>();
                for (Factory f : ownedAdmin) {
                    typeCounts.merge(f.getType(), 1L, Long::sum);
                }
                for (PlayerFactory pf : ownedPlayer) {
                    typeCounts.merge(pf.getType(), 1L, Long::sum);
                }
                for (Map.Entry<FactoryType, Long> entry : typeCounts.entrySet()) {
                    lore.add(color("&7\u2022 " + entry.getKey().getDisplayName() + "&7: &e" + entry.getValue()));
                }
            }
        }

        ItemStack item = createItem(mat, displayName, lore, cmd);
        stampGuiId("quick_stats", item);
        stampOnClick(cfg, "quick_stats", item);
        return item;
    }

    private Map<String, String> buildGlobalPlaceholders() {
        Map<String, String> ph = new HashMap<>();
        ph.put("{player_name}", player.getName());

        List<Factory> ownedAdmin = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());
        long runningAdmin = ownedAdmin.stream().filter(f -> f.getStatus() == FactoryStatus.RUNNING).count();
        double adminValue = ownedAdmin.stream().mapToDouble(Factory::getPrice).sum();

        List<PlayerFactory> ownedPlayer = plugin.getPlayerFactoryManager() != null
                ? new ArrayList<>(plugin.getPlayerFactoryManager().getAllFactories()).stream()
                        .filter(pf -> pf.getOwner().equals(player.getUniqueId())).toList()
                : new ArrayList<>();
        long runningPlayer = ownedPlayer.stream().filter(pf -> pf.getStatus() == FactoryStatus.RUNNING).count();
        double playerValue = ownedPlayer.stream().mapToDouble(PlayerFactory::getPrice).sum();

        long runningFactories = runningAdmin + runningPlayer;
        long totalOwned = ownedAdmin.size() + ownedPlayer.size();
        long stoppedFactories = totalOwned - runningFactories;
        int availableFactories = (int) plugin.getFactoryManager().getAllFactories().stream()
                .filter(f -> f.getOwner() == null).count();
        double totalValue = adminValue + playerValue;

        ph.put("{factory_available_count}", String.valueOf(availableFactories));
        ph.put("{factory_owned_count}", String.valueOf(totalOwned));
        ph.put("{factory_running_count}", String.valueOf(runningFactories));
        ph.put("{factory_stopped_count}", String.valueOf(stoppedFactories));
        ph.put("{factory_total_value}", String.format("%.2f", totalValue));

        List<Invoice> allInvoices = new ArrayList<>(
                plugin.getInvoiceManager().getInvoicesByOwner(player.getUniqueId()));
        if (plugin.getTaxManager() != null) {
            for (TaxManager.TaxRecord record : plugin.getTaxManager().getPlayerTaxRecords(player.getUniqueId())) {
                if (record.amountDue > 0) {
                    allInvoices.add(new Invoice("TM_" + record.factoryId, player.getUniqueId(), InvoiceType.TAX,
                            record.amountDue, record.dueDate));
                }
            }
        }
        long overdueInvoices = allInvoices.stream().filter(Invoice::isOverdue).count();
        double totalDue = allInvoices.stream().mapToDouble(Invoice::getAmount).sum();
        double totalTaxDue = plugin.getTaxManager() != null
                ? plugin.getTaxManager().getTotalTaxDue(player.getUniqueId())
                : 0.0;

        ph.put("{invoice_count}", String.valueOf(allInvoices.size()));
        ph.put("{invoice_overdue_count}", String.valueOf(overdueInvoices));
        ph.put("{invoice_total_due}", String.format("%.2f", totalDue));
        ph.put("{invoice_overdue_warning}",
                overdueInvoices > 0 ? color("&c&lWARNING: Overdue invoices!") : color("&aAll invoices up to date"));
        ph.put("{tax_due}", String.format("%.2f", totalTaxDue));
        ph.put("{tax_rate}", String.valueOf(plugin.getConfig().getDouble("tax.rate", 5.0)));

        List<FactoryNPC> playerNPCs = getPlayerNPCs();
        List<FactoryNPC> ownedNPCs = plugin.getNPCManager().getOwnedNPCsByOwner(player.getUniqueId());
        int unassigned = plugin.getNPCManager().getUnassignedNPCsByOwner(player.getUniqueId()).size();
        String unassignedLine = unassigned > 0 ? "&eUnassigned: &e" + unassigned : "&aAll employees assigned!";

        ph.put("{employee_assigned_count}", String.valueOf(playerNPCs.size()));
        ph.put("{employee_owned_count}", String.valueOf(ownedNPCs.size()));
        ph.put("{employee_unassigned_line}", color(unassignedLine));

        int activeListings = plugin.getMarketplaceManager() != null
                ? plugin.getMarketplaceManager().getPlayerListings(player.getUniqueId()).size()
                : 0;
        ph.put("{marketplace_listing_count}", String.valueOf(activeListings));

        int unlockedAchievements = plugin.getAchievementManager() != null
                ? plugin.getAchievementManager().getUnlockedCount(player.getUniqueId())
                : 0;
        int totalAchievements = plugin.getAchievementManager() != null
                ? plugin.getAchievementManager().getTotalAchievementCount()
                : 0;
        ph.put("{achievement_unlocked_count}", String.valueOf(unlockedAchievements));
        ph.put("{achievement_total_count}", String.valueOf(totalAchievements));

        long activeResearch = 0;
        int totalCompleted = 0;
        if (plugin.getResearchManager() != null) {
            for (String rId : plugin.getResearchManager().getResearchIds()) {
                if (plugin.getResearchManager().isResearching(player.getUniqueId(), rId))
                    activeResearch++;
                totalCompleted += plugin.getResearchManager().getPlayerResearchLevel(player.getUniqueId(), rId);
            }
        }
        ph.put("{research_active_count}", String.valueOf(activeResearch));
        ph.put("{research_completed_count}", String.valueOf(totalCompleted));

        int completedQuests = plugin.getDailyQuestManager() != null
                ? plugin.getDailyQuestManager().getCompletedCount(player.getUniqueId())
                : 0;
        int totalQuests = plugin.getDailyQuestManager() != null ? plugin.getDailyQuestManager().getTotalQuestCount()
                : 0;
        String resetTime = plugin.getDailyQuestManager() != null ? plugin.getDailyQuestManager().formatTimeRemaining()
                : "N/A";

        ph.put("{quest_completed_count}", String.valueOf(completedQuests));
        ph.put("{quest_total_count}", String.valueOf(totalQuests));
        ph.put("{quest_reset_time}", resetTime);

        int distributionActiveCount = plugin.getDistributionManager() != null
                ? plugin.getDistributionManager().getActiveRequestCount(player.getUniqueId())
                : 0;
        ph.put("{distribution_active_count}", String.valueOf(distributionActiveCount));

        return ph;
    }

    private List<FactoryNPC> getPlayerNPCs() {
        List<FactoryNPC> playerNPCs = new ArrayList<>();
        List<Factory> adminFactories = plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId());

        List<PlayerFactory> playerFactories = plugin.getPlayerFactoryManager() != null
                ? new ArrayList<>(plugin.getPlayerFactoryManager().getAllFactories()).stream()
                        .filter(pf -> pf.getOwner().equals(player.getUniqueId())).toList()
                : new ArrayList<>();

        for (FactoryNPC npc : plugin.getNPCManager().getAllNPCs()) {
            if (npc.getFactoryId() == null)
                continue;

            boolean found = false;
            for (Factory factory : adminFactories) {
                if (npc.getFactoryId().equals(factory.getId())) {
                    playerNPCs.add(npc);
                    found = true;
                    break;
                }
            }
            if (found)
                continue;

            for (PlayerFactory factory : playerFactories) {
                if (npc.getFactoryId().equals(factory.getId())) {
                    playerNPCs.add(npc);
                    break;
                }
            }
        }
        return playerNPCs;
    }

    /** Core item builder that accepts CustomModelData. */
    private ItemStack createItem(Material material, String name, List<String> lore, int customModelData) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null)
                meta.setDisplayName(name);
            if (lore != null)
                meta.setLore(lore);
            if (customModelData > 0)
                meta.setCustomModelData(customModelData);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Convenience overload without CustomModelData (kept for backward compat). */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        return createItem(material, name, lore, 0);
    }

    /** Translate & color codes. */
    private String color(String text) {
        if (text == null)
            return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /** Parse a material name safely, returning the fallback on failure. */
    private Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isBlank())
            return fallback;
        Material mat = Material.matchMaterial(name.toUpperCase());
        return mat != null ? mat : fallback;
    }

    /** Play a sound to the player. */
    private void playSound(String soundName, float volume, float pitch) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {
            // Unknown sound name - silently skip
        }
    }
}
