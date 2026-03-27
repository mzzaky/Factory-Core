package com.aithor.factorycore.managers;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.FactoryStatus;
import com.aithor.factorycore.models.FactoryType;
import com.aithor.factorycore.models.PlayerFactory;
import com.aithor.factorycore.models.ProductionTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages player-created factories with built-in region protection (no
 * WorldGuard needed).
 */
public class PlayerFactoryManager {

    private final FactoryCore plugin;
    private final Map<String, PlayerFactory> playerFactories;
    private final Map<String, BossBar> productionBossBars;
    private final File dataFile;

    public PlayerFactoryManager(FactoryCore plugin) {
        this.plugin = plugin;
        this.playerFactories = new HashMap<>();
        this.productionBossBars = new HashMap<>();
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists())
            dataFolder.mkdirs();
        this.dataFile = new File(dataFolder, "player_factories.yml");
        loadAll();
    }

    // ── Load / Save ──────────────────────────────────────────────────────────

    public void loadAll() {
        if (!dataFile.exists())
            return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : config.getKeys(false)) {
            try {
                String ownerStr = config.getString(key + ".owner");
                UUID ownerUUID = UUID.fromString(ownerStr);
                FactoryType type = FactoryType.valueOf(config.getString(key + ".type"));
                double price = config.getDouble(key + ".price");

                String worldName = config.getString(key + ".world");
                int minX = config.getInt(key + ".region.minX");
                int minY = config.getInt(key + ".region.minY");
                int minZ = config.getInt(key + ".region.minZ");
                int maxX = config.getInt(key + ".region.maxX");
                int maxY = config.getInt(key + ".region.maxY");
                int maxZ = config.getInt(key + ".region.maxZ");

                double cx = config.getDouble(key + ".center.x");
                double cy = config.getDouble(key + ".center.y");
                double cz = config.getDouble(key + ".center.z");
                float cyaw = (float) config.getDouble(key + ".center.yaw");
                float cpitch = (float) config.getDouble(key + ".center.pitch");

                PlayerFactory pf = new PlayerFactory(key, ownerUUID, type, price,
                        worldName, minX, minY, minZ, maxX, maxY, maxZ,
                        cx, cy, cz, cyaw, cpitch);

                pf.setLevel(config.getInt(key + ".level", 1));
                pf.setStatus(FactoryStatus.valueOf(config.getString(key + ".status", "STOPPED")));

                // Load production
                if (config.contains(key + ".production")) {
                    String recipeId = config.getString(key + ".production.recipe");
                    long startTime = config.getLong(key + ".production.start-time");
                    int duration = config.getInt(key + ".production.duration");
                    pf.setCurrentProduction(new ProductionTask(recipeId, startTime, duration));
                }

                // Load upgrade timer
                if (config.contains(key + ".upgrade.start-time")) {
                    pf.setUpgradeStartTime(config.getLong(key + ".upgrade.start-time"));
                    pf.setUpgradeDurationSeconds(config.getInt(key + ".upgrade.duration"));
                }

                playerFactories.put(key, pf);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load player factory: " + key);
                e.printStackTrace();
            }
        }
        plugin.getLogger().info("Loaded " + playerFactories.size() + " player-created factories!");
    }

    public void saveAll() {
        FileConfiguration config = new YamlConfiguration();
        for (PlayerFactory pf : playerFactories.values()) {
            String path = pf.getId();
            config.set(path + ".owner", pf.getOwner().toString());
            config.set(path + ".type", pf.getType().name());
            config.set(path + ".price", pf.getPrice());
            config.set(path + ".level", pf.getLevel());
            config.set(path + ".status", pf.getStatus().name());
            config.set(path + ".world", pf.getWorldName());

            config.set(path + ".region.minX", pf.getMinX());
            config.set(path + ".region.minY", pf.getMinY());
            config.set(path + ".region.minZ", pf.getMinZ());
            config.set(path + ".region.maxX", pf.getMaxX());
            config.set(path + ".region.maxY", pf.getMaxY());
            config.set(path + ".region.maxZ", pf.getMaxZ());

            config.set(path + ".center.x", pf.getCenterX());
            config.set(path + ".center.y", pf.getCenterY());
            config.set(path + ".center.z", pf.getCenterZ());
            config.set(path + ".center.yaw", (double) pf.getCenterYaw());
            config.set(path + ".center.pitch", (double) pf.getCenterPitch());

            if (pf.getCurrentProduction() != null) {
                ProductionTask task = pf.getCurrentProduction();
                config.set(path + ".production.recipe", task.getRecipeId());
                config.set(path + ".production.start-time", task.getStartTime());
                config.set(path + ".production.duration", task.getDuration());
            }

            if (pf.isUpgrading()) {
                config.set(path + ".upgrade.start-time", pf.getUpgradeStartTime());
                config.set(path + ".upgrade.duration", pf.getUpgradeDurationSeconds());
            } else {
                config.set(path + ".upgrade", null);
            }
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player factories!");
            e.printStackTrace();
        }
    }

    // ── Factory creation ─────────────────────────────────────────────────────

    /**
     * Creates a new player factory centered on the player's position.
     * The region extends 3 blocks in each horizontal direction and 5 blocks
     * up/down.
     *
     * @return the created PlayerFactory, or null on failure
     */
    public PlayerFactory createFactory(Player player, FactoryType type) {
        Location loc = player.getLocation();
        String worldName = loc.getWorld().getName();

        // Check world whitelist
        List<String> allowedWorlds = plugin.getConfig().getStringList("player-factory.allowed-worlds");
        if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(worldName)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("player-factory-world-not-allowed"));
            return null;
        }

        // Check factory limit
        if (!player.hasPermission("factorycore.bypass.factory-limit")) {
            int limit = plugin.getConfig().getInt("factory.max-factories-per-player", 3);

            // Apply Industrial Mastery research buff
            if (plugin.getResearchManager() != null) {
                limit += plugin.getResearchManager().getAdditionalFactoryLimit(player.getUniqueId());
            }

            int currentCount = getFactoryCountByOwner(player.getUniqueId());
            if (currentCount >= limit) {
                player.sendMessage(plugin.getLanguageManager().getMessage("player-factory-limit-reached")
                        .replace("{limit}", String.valueOf(limit)));
                return null;
            }
        }

        // Get price from config
        String typeId = type.getId();
        double price = plugin.getConfig().getDouble("player-factory.creation-prices." + typeId,
                plugin.getConfig().getDouble("factory-types." + typeId + ".base-price", 100000.0));

        // Check balance
        if (!plugin.getEconomy().has(player, price)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("insufficient-funds")
                    .replace("{amount}", String.format("%.2f", price)));
            return null;
        }

        // Check overlap
        int radius = plugin.getConfig().getInt("player-factory.region-radius", 3);
        int heightUp = plugin.getConfig().getInt("player-factory.region-height-up", 5);
        int heightDown = plugin.getConfig().getInt("player-factory.region-height-down", 5);

        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        int minX = bx - radius, maxX = bx + radius;
        int minY = Math.max(by - heightDown, loc.getWorld().getMinHeight());
        int maxY = Math.min(by + heightUp, loc.getWorld().getMaxHeight());
        int minZ = bz - radius, maxZ = bz + radius;

        for (PlayerFactory existing : playerFactories.values()) {
            if (!existing.getWorldName().equals(worldName))
                continue;
            if (regionsOverlap(minX, minY, minZ, maxX, maxY, maxZ,
                    existing.getMinX(), existing.getMinY(), existing.getMinZ(),
                    existing.getMaxX(), existing.getMaxY(), existing.getMaxZ())) {
                player.sendMessage(plugin.getLanguageManager().getMessage("player-factory-overlap"));
                return null;
            }
        }

        // Withdraw money
        plugin.getEconomy().withdrawPlayer(player, price);

        // Generate unique ID
        String id = "pf_" + player.getName().toLowerCase() + "_" + System.currentTimeMillis();

        PlayerFactory pf = new PlayerFactory(id, player.getUniqueId(), type, price,
                worldName, minX, minY, minZ, maxX, maxY, maxZ,
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        playerFactories.put(id, pf);
        saveAll();

        return pf;
    }

    // ── Sell ──────────────────────────────────────────────────────────────────

    public boolean sellFactory(Player player, String factoryId) {
        PlayerFactory pf = playerFactories.get(factoryId);
        if (pf == null)
            return false;
        if (!pf.getOwner().equals(player.getUniqueId()))
            return false;

        double sellMultiplier = plugin.getConfig().getDouble("factory.sell-price-multiplier", 0.5);
        double sellPrice = pf.getPrice() * sellMultiplier;

        plugin.getEconomy().depositPlayer(player, sellPrice);
        playerFactories.remove(factoryId);

        // Unassign NPC if any is attached to this player factory
        com.aithor.factorycore.models.FactoryNPC npc = plugin.getNPCManager().getAssignedNPCForFactory(factoryId);
        if (npc != null) {
            plugin.getNPCManager().unassignNPC(player, npc.getId());
        }

        // Clear storage
        plugin.getStorageManager().clearStorage(factoryId);

        // Remove bossbar if exists
        if (productionBossBars.containsKey(factoryId)) {
            productionBossBars.get(factoryId).removeAll();
            productionBossBars.remove(factoryId);
        }

        saveAll();
        return true;
    }

    public double getSellPrice(String factoryId) {
        PlayerFactory pf = playerFactories.get(factoryId);
        if (pf == null)
            return 0;
        double sellMultiplier = plugin.getConfig().getDouble("factory.sell-price-multiplier", 0.5);
        return pf.getPrice() * sellMultiplier;
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public PlayerFactory getFactory(String id) {
        return playerFactories.get(id);
    }

    public boolean removeFactory(String id) {
        PlayerFactory pf = playerFactories.remove(id);
        if (pf != null) {
            // Unassign NPC if any is attached
            com.aithor.factorycore.models.FactoryNPC npc = plugin.getNPCManager().getAssignedNPCForFactory(id);
            if (npc != null) {
                plugin.getNPCManager().removeNPC(id);
            }

            // Remove bossbar if exists
            if (productionBossBars.containsKey(id)) {
                productionBossBars.get(id).removeAll();
                productionBossBars.remove(id);
            }

            // Clear storage
            plugin.getStorageManager().clearStorage(id);

            saveAll();
            return true;
        }
        return false;
    }

    public List<PlayerFactory> getAllFactories() {
        return new ArrayList<>(playerFactories.values());
    }

    public List<PlayerFactory> getFactoriesByOwner(UUID owner) {
        return playerFactories.values().stream()
                .filter(pf -> pf.getOwner().equals(owner))
                .collect(Collectors.toList());
    }

    public int getFactoryCountByOwner(UUID owner) {
        return (int) playerFactories.values().stream()
                .filter(pf -> pf.getOwner().equals(owner))
                .count();
    }

    /**
     * Returns the PlayerFactory that contains the given location, or null.
     */
    public PlayerFactory getFactoryAt(Location loc) {
        for (PlayerFactory pf : playerFactories.values()) {
            if (pf.containsLocation(loc))
                return pf;
        }
        return null;
    }

    // ── Region overlap check ─────────────────────────────────────────────────

    private boolean regionsOverlap(int ax1, int ay1, int az1, int ax2, int ay2, int az2,
            int bx1, int by1, int bz1, int bx2, int by2, int bz2) {
        return ax1 <= bx2 && ax2 >= bx1 &&
                ay1 <= by2 && ay2 >= by1 &&
                az1 <= bz2 && az2 >= bz1;
    }

    // ── Upgrade tick (call from scheduler, mirrors FactoryManager.updateUpgrades)
    // ───

    /**
     * Checks all player factories for completed upgrade timers and levels them up.
     * Call this from the same scheduler task that calls
     * FactoryManager.updateUpgrades().
     */
    public void updateUpgrades() {
        for (PlayerFactory pf : playerFactories.values()) {
            if (pf.isUpgradeComplete()) {
                completeUpgrade(pf);
            }
        }
    }

    private void completeUpgrade(PlayerFactory pf) {
        pf.setLevel(pf.getLevel() + 1);
        pf.setUpgradeStartTime(-1);
        pf.setUpgradeDurationSeconds(0);
        saveAll();

        org.bukkit.entity.Player owner = org.bukkit.Bukkit.getPlayer(pf.getOwner());
        if (owner != null) {
            owner.sendMessage(plugin.getLanguageManager().getMessage("level-up")
                    .replace("{level}", String.valueOf(pf.getLevel())));
        }
    }

    // ── Production tick (mirrors FactoryManager.updateProduction) ───────────────

    /**
     * Checks all running player factories for completed productions and adds
     * output items to storage. Call from the same scheduler task.
     */
    public void updateProduction() {
        for (PlayerFactory pf : playerFactories.values()) {
            if (pf.getStatus() != com.aithor.factorycore.models.FactoryStatus.RUNNING)
                continue;
            com.aithor.factorycore.models.ProductionTask task = pf.getCurrentProduction();
            if (task == null) {
                pf.setStatus(com.aithor.factorycore.models.FactoryStatus.STOPPED);
                continue;
            }
            if (task.isComplete()) {
                if (pf.getStatus() == com.aithor.factorycore.models.FactoryStatus.RUNNING) {
                    completeProduction(pf, task);
                }
            } else {
                updateProductionBossBar(pf, task);
            }
        }
    }

    private void completeProduction(PlayerFactory pf, com.aithor.factorycore.models.ProductionTask task) {
        com.aithor.factorycore.models.Recipe recipe = plugin.getRecipeManager().getRecipe(task.getRecipeId());
        if (recipe != null) {
            StorageManager.OutputDestination dest =
                    plugin.getStorageManager().getOutputDestination(pf.getId());

            if (!recipe.isDisableItemOutput()) {
                for (java.util.Map.Entry<String, Integer> output : recipe.getOutputs().entrySet()) {
                    if (dest == StorageManager.OutputDestination.PLAYER_INVENTORY) {
                        org.bukkit.entity.Player online = org.bukkit.Bukkit.getPlayer(pf.getOwner());
                        if (online != null) {
                            org.bukkit.inventory.ItemStack toGive = plugin.getResourceManager()
                                    .createItemStack(output.getKey(), output.getValue());
                            if (toGive == null && output.getKey().startsWith("vanilla:")) {
                                String matName = output.getKey().substring("vanilla:".length()).toUpperCase();
                                try {
                                    toGive = new org.bukkit.inventory.ItemStack(
                                            org.bukkit.Material.valueOf(matName), output.getValue());
                                } catch (IllegalArgumentException ignored) {}
                            }
                            if (toGive != null) {
                                java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftover =
                                        online.getInventory().addItem(toGive);
                                if (!leftover.isEmpty()) {
                                    int notAdded = leftover.values().stream()
                                            .mapToInt(org.bukkit.inventory.ItemStack::getAmount).sum();
                                    plugin.getStorageManager().addOutputItem(
                                            pf.getId(), output.getKey(), notAdded);
                                    online.sendMessage("§c[Factory] Inventory full! §7Some items sent to Output Storage.");
                                }
                            } else {
                                plugin.getStorageManager().addOutputItem(
                                        pf.getId(), output.getKey(), output.getValue());
                            }
                        } else {
                            // Owner is offline — fall back to output storage
                            plugin.getStorageManager().addOutputItem(
                                    pf.getId(), output.getKey(), output.getValue());
                        }
                    } else {
                        plugin.getStorageManager().addOutputItem(pf.getId(), output.getKey(), output.getValue());
                    }
                }
            }
            // Execute console commands
            for (String cmd : recipe.getConsoleCommands()) {
                String command = cmd.replace("{player}",
                        org.bukkit.Bukkit.getOfflinePlayer(pf.getOwner()).getName());
                org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), command);
            }
        }

        pf.setCurrentProduction(null);
        pf.setStatus(com.aithor.factorycore.models.FactoryStatus.STOPPED);

        // Remove bossbar immediately
        String factoryId = pf.getId();
        BossBar bossBar = productionBossBars.remove(factoryId);
        if (bossBar != null) {
            bossBar.removeAll();
        }

        saveAll();

        // ── Achievements ────────────────────────────────────────────────────
        if (plugin.getAchievementManager() != null) {
            org.bukkit.entity.Player online = org.bukkit.Bukkit.getPlayer(pf.getOwner());
            if (online != null) {
                plugin.getAchievementManager().awardAchievement(online, "early_prototype");
                plugin.getAchievementManager().addProgress(online, "mass_producer", 1);
            } else {
                plugin.getAchievementManager().addProgressOffline(pf.getOwner(), "mass_producer", 1);
            }
        }

        // ── Daily Quest ─────────────────────────────────────────────────────
        if (plugin.getDailyQuestManager() != null) {
            org.bukkit.entity.Player online = org.bukkit.Bukkit.getPlayer(pf.getOwner());
            if (online != null) {
                plugin.getDailyQuestManager().addProgressByType(online, "PRODUCTION_COMPLETE", 1);
            }
        }

        // ── Notify owner ────────────────────────────────────────────────────
        org.bukkit.entity.Player owner = org.bukkit.Bukkit.getPlayer(pf.getOwner());
        if (owner != null) {
            String recipeName = (recipe != null && recipe.getName() != null) ? recipe.getName() : "Unknown Recipe";
            owner.sendMessage(plugin.getLanguageManager().getMessage("production-complete")
                    .replace("{recipe}", recipeName));

            // Sound
            if (plugin.getConfig().getBoolean("notifications.sound.enabled")) {
                try {
                    owner.playSound(owner.getLocation(),
                            plugin.getConfig().getString("notifications.sound.production-complete"),
                            1.0f, 1.0f);
                } catch (Exception ignored) {
                }
            }

            // Title
            if (plugin.getConfig().getBoolean("notifications.titles.enabled")) {
                owner.sendTitle(
                        plugin.getLanguageManager().getMessage("titles.production-complete.title"),
                        plugin.getLanguageManager().getMessage("titles.production-complete.subtitle")
                                .replace("{recipe}", recipeName),
                        10, 40, 10);
            }
        }
    }

    public void startProduction(PlayerFactory factory, String recipeId) {
        com.aithor.factorycore.models.Recipe recipe = plugin.getRecipeManager().getRecipe(recipeId);
        if (recipe == null)
            return;

        int duration = recipe.getProductionTime();

        // Apply level bonuses
        int level = factory.getLevel();
        double timeReduction = plugin.getConfig()
                .getDouble("factory.level-bonuses.time-reduction", 10.0);
        duration = (int) (duration * (1 - (timeReduction * (level - 1) / 100.0)));

        // Apply employee production-time-reduction buff
        double employeeReduction = plugin.getNPCManager().getProductionTimeReductionForFactory(factory.getId());
        if (employeeReduction > 0) {
            duration = (int) (duration * (1 - (employeeReduction / 100.0)));
        }

        // Apply Advanced Machine Technology research buff
        if (plugin.getResearchManager() != null && factory.getOwner() != null) {
            double researchReduction = plugin.getResearchManager().getProductionTimeReduction(factory.getOwner());
            if (researchReduction > 0) {
                duration = (int) (duration * (1 - (researchReduction / 100.0)));
            }
        }

        // Ensure minimum duration of 1 second
        duration = Math.max(duration, 1);

        ProductionTask task = new ProductionTask(recipeId, System.currentTimeMillis(), duration);
        factory.setCurrentProduction(task);
        factory.setStatus(FactoryStatus.RUNNING);

        // Create bossbar
        if (plugin.getConfig().getBoolean("production.show-bossbar", true)) {
            createProductionBossBar(factory, recipe);
        }

        saveAll();
    }

    private void createProductionBossBar(PlayerFactory factory, com.aithor.factorycore.models.Recipe recipe) {
        Player owner = Bukkit.getPlayer(factory.getOwner());
        if (owner == null)
            return;

        String title = plugin.getLanguageManager().getMessage("bossbar.production")
                .replace("{recipe}", recipe.getName())
                .replace("{percent}", "0");

        BossBar bossBar = Bukkit.createBossBar(
                title,
                BarColor.valueOf(plugin.getConfig().getString("production.bossbar-color", "BLUE")),
                BarStyle.valueOf(plugin.getConfig().getString("production.bossbar-style", "SOLID")));

        bossBar.addPlayer(owner);
        bossBar.setProgress(0.0);
        productionBossBars.put(factory.getId(), bossBar);
    }

    private void updateProductionBossBar(PlayerFactory factory, ProductionTask task) {
        String factoryId = factory.getId();
        BossBar bossBar = productionBossBars.get(factoryId);
        com.aithor.factorycore.models.Recipe recipe = plugin.getRecipeManager().getRecipe(task.getRecipeId());

        if (recipe == null)
            return;

        Player owner = Bukkit.getPlayer(factory.getOwner());

        if (bossBar == null) {
            if (plugin.getConfig().getBoolean("production.show-bossbar", true)) {
                createProductionBossBar(factory, recipe);
                bossBar = productionBossBars.get(factoryId);
                if (bossBar == null)
                    return;
            } else {
                return;
            }
        } else if (owner != null && !bossBar.getPlayers().contains(owner)) {
            bossBar.addPlayer(owner);
        }

        double progress = task.getProgress();
        int percent = (int) (progress * 100);

        String title = plugin.getLanguageManager().getMessage("bossbar.production")
                .replace("{recipe}", recipe.getName())
                .replace("{percent}", String.valueOf(percent));

        bossBar.setTitle(title);
        bossBar.setProgress(progress);
    }

    /**
     * Start a timed upgrade for a player-created factory: consume money + resources, start the upgrade timer.
     * Returns false (with a message to the player) on any failure.
     */
    public boolean startUpgrade(Player player, String id) {
        PlayerFactory pf = getFactory(id);
        if (pf == null)
            return false;
        if (!pf.getOwner().equals(player.getUniqueId()))
            return false;

        if (pf.isUpgrading()) {
            player.sendMessage("§c⚠ Factory is already being upgraded!");
            return false;
        }

        int maxLevel = plugin.getConfig().getInt("factory.max-level", 5);
        if (pf.getLevel() >= maxLevel) {
            player.sendMessage("§cFactory is already at max level!");
            return false;
        }

        // ── Economy check ─────────────────────────────────────────────────────
        double upgradeCost = pf.getPrice() * 0.5 * pf.getLevel();
        if (!plugin.getEconomy().has(player, upgradeCost)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("insufficient-funds")
                    .replace("{amount}", String.format("%.2f", upgradeCost)));
            return false;
        }

        // ── Resource requirement check ────────────────────────────────────────
        int targetLevel = pf.getLevel() + 1;
        String reqPath = "factory.upgrade-requirements." + targetLevel;

        if (plugin.getConfig().isConfigurationSection(reqPath)) {
            org.bukkit.configuration.ConfigurationSection reqSection = plugin.getConfig()
                    .getConfigurationSection(reqPath);

            for (String resourceId : reqSection.getKeys(false)) {
                int required = reqSection.getInt(resourceId);
                int found = countResourceInInventory(player, resourceId);

                if (found < required) {
                    String resName;
                    if (com.aithor.factorycore.managers.ResourceManager.isVanillaInput(resourceId)) {
                        resName = com.aithor.factorycore.managers.ResourceManager.getVanillaDisplayName(resourceId);
                    } else {
                        com.aithor.factorycore.models.ResourceItem res = plugin.getResourceManager()
                                .getResource(resourceId);
                        resName = (res != null) ? res.getName() : resourceId;
                    }

                    player.sendMessage("§c✗ §7Not enough §f" + resName
                            + "§7! Need §e" + required + "§7, have §c" + found + "§7.");
                    return false;
                }
            }

            // All resources confirmed — consume them
            for (String resourceId : reqSection.getKeys(false)) {
                consumeResourceFromInventory(player, resourceId, reqSection.getInt(resourceId));
            }
        }

        // ── Deduct money ──────────────────────────────────────────────────────
        plugin.getEconomy().withdrawPlayer(player, upgradeCost);

        // ── Start upgrade timer ───────────────────────────────────────────────
        int duration = plugin.getConfig().getInt("factory.upgrade-time." + targetLevel, 60);

        // Apply Nano-Construction Framework research buff
        if (plugin.getResearchManager() != null) {
            double researchReduction = plugin.getResearchManager().getUpgradeTimeReduction(player.getUniqueId());
            if (researchReduction > 0) {
                duration = (int) (duration * (1 - (researchReduction / 100.0)));
            }
        }
        duration = Math.max(duration, 1); // minimum 1 second

        pf.setUpgradeStartTime(System.currentTimeMillis());
        pf.setUpgradeDurationSeconds(duration);
        saveAll();

        return true;
    }

    /** Counts how many of a plugin-registered resource the player holds. */
    private int countResourceInInventory(Player player, String resourceId) {
        if (com.aithor.factorycore.managers.ResourceManager.isVanillaInput(resourceId)) {
            return plugin.getResourceManager().countVanillaInInventory(player, resourceId);
        }
        int total = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == org.bukkit.Material.AIR)
                continue;
            String id = plugin.getResourceManager().getResourceId(item);
            if (resourceId.equals(id))
                total += item.getAmount();
        }
        return total;
    }

    /**
     * Removes the specified amount of a plugin-registered resource from the player's
     * inventory. Assumes availability was already confirmed.
     */
    private void consumeResourceFromInventory(Player player, String resourceId, int amount) {
        if (com.aithor.factorycore.managers.ResourceManager.isVanillaInput(resourceId)) {
            plugin.getResourceManager().consumeVanillaFromInventory(player, resourceId, amount);
            return;
        }
        int remaining = amount;
        org.bukkit.inventory.ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            org.bukkit.inventory.ItemStack item = contents[i];
            if (item == null || item.getType() == org.bukkit.Material.AIR)
                continue;
            String id = plugin.getResourceManager().getResourceId(item);
            if (!resourceId.equals(id))
                continue;

            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
        }
    }
}
