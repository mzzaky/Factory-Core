package com.aithor.factorycore.managers;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.Factory;
import com.aithor.factorycore.models.FactoryStatus;
import com.aithor.factorycore.models.FactoryType;
import com.aithor.factorycore.models.ProductionTask;
import com.aithor.factorycore.models.Recipe;
import com.aithor.factorycore.utils.WorldGuardUtils;
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

public class FactoryManager {

    private final FactoryCore plugin;
    private final Map<String, Factory> factories;
    private final Map<String, BossBar> productionBossBars;
    private final File dataFile;

    public FactoryManager(FactoryCore plugin) {
        this.plugin = plugin;
        this.factories = new HashMap<>();
        this.productionBossBars = new HashMap<>();
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.dataFile = new File(dataFolder, "factories.yml");

        loadFactories();
    }

    public void loadFactories() {
        if (!dataFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        for (String key : config.getKeys(false)) {
            try {
                String ownerStr = config.getString(key + ".owner");
                UUID ownerUUID = null;
                if (ownerStr != null && !ownerStr.equals("null")) {
                    try {
                        ownerUUID = UUID.fromString(ownerStr);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger()
                                .warning(String.format("Invalid owner UUID for factory %s: %s", key, ownerStr));
                    }
                }

                Factory factory = new Factory(
                        config.getString(key + ".id"),
                        config.getString(key + ".region"),
                        FactoryType.valueOf(config.getString(key + ".type")),
                        ownerUUID,
                        config.getDouble(key + ".price"),
                        config.getInt(key + ".level", 1));

                factory.setStatus(FactoryStatus.valueOf(
                        config.getString(key + ".status", "STOPPED")));

                // Load current production if exists
                if (config.contains(key + ".production")) {
                    String recipeId = config.getString(key + ".production.recipe");
                    long startTime = config.getLong(key + ".production.start-time");
                    int duration = config.getInt(key + ".production.duration");

                    ProductionTask task = new ProductionTask(recipeId, startTime, duration);
                    factory.setCurrentProduction(task);
                }

                // Load fast travel location if exists
                if (config.contains(key + ".fast-travel")) {
                    String worldName = config.getString(key + ".fast-travel.world");
                    double x = config.getDouble(key + ".fast-travel.x");
                    double y = config.getDouble(key + ".fast-travel.y");
                    double z = config.getDouble(key + ".fast-travel.z");
                    float yaw = (float) config.getDouble(key + ".fast-travel.yaw");
                    float pitch = (float) config.getDouble(key + ".fast-travel.pitch");

                    if (worldName != null) {
                        org.bukkit.World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            Location location = new Location(world, x, y, z, yaw, pitch);
                            factory.setFastTravelLocation(location);
                        }
                    }
                }

                factories.put(factory.getId(), factory);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load factory: " + key);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + factories.size() + " factories!");
    }

    public void saveAll() {
        FileConfiguration config = new YamlConfiguration();

        for (Factory factory : factories.values()) {
            String path = factory.getId();
            config.set(path + ".id", factory.getId());
            config.set(path + ".region", factory.getRegionName());
            config.set(path + ".type", factory.getType().name());
            config.set(path + ".owner", factory.getOwner() != null ? factory.getOwner().toString() : null);
            config.set(path + ".price", factory.getPrice());
            config.set(path + ".level", factory.getLevel());
            config.set(path + ".status", factory.getStatus().name());

            // Save current production if exists
            if (factory.getCurrentProduction() != null) {
                ProductionTask task = factory.getCurrentProduction();
                config.set(path + ".production.recipe", task.getRecipeId());
                config.set(path + ".production.start-time", task.getStartTime());
                config.set(path + ".production.duration", task.getDuration());
            }

            // Save fast travel location if exists
            if (factory.getFastTravelLocation() != null) {
                Location loc = factory.getFastTravelLocation();
                if (loc != null && loc.getWorld() != null) {
                    config.set(path + ".fast-travel.world", loc.getWorld().getName());
                }
                config.set(path + ".fast-travel.x", loc.getX());
                config.set(path + ".fast-travel.y", loc.getY());
                config.set(path + ".fast-travel.z", loc.getZ());
                config.set(path + ".fast-travel.yaw", loc.getYaw());
                config.set(path + ".fast-travel.pitch", loc.getPitch());
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save factories!");
            e.printStackTrace();
        }
    }

    public Factory createFactory(String id, String regionName, FactoryType type, double price) {
        return createFactory(id, regionName, type, price, null);
    }

    public Factory createFactory(String id, String regionName, FactoryType type, double price,
            Location fastTravelLocation) {
        if (factories.containsKey(id)) {
            return null;
        }

        ProtectedRegion region = WorldGuardUtils.getRegion(regionName);
        if (region == null) {
            return null;
        }

        Factory factory = new Factory(id, regionName, type, null, price, 1);
        factory.setFastTravelLocation(fastTravelLocation);
        factories.put(id, factory);
        saveAll();

        return factory;
    }

    public boolean removeFactory(String id) {
        Factory factory = factories.remove(id);
        if (factory != null) {
            // Remove NPC if exists
            plugin.getNPCManager().removeNPC(id);

            // Remove bossbar if exists
            if (productionBossBars.containsKey(id)) {
                productionBossBars.get(id).removeAll();
                productionBossBars.remove(id);
            }

            saveAll();
            return true;
        }
        return false;
    }

    public Factory getFactory(String id) {
        return factories.get(id);
    }

    public List<Factory> getAllFactories() {
        return new ArrayList<>(factories.values());
    }

    public List<Factory> getFactoriesByOwner(UUID owner) {
        List<Factory> owned = new ArrayList<>();
        for (Factory factory : factories.values()) {
            if (factory.getOwner() != null && factory.getOwner().equals(owner)) {
                owned.add(factory);
            }
        }
        return owned;
    }

    public boolean buyFactory(Player player, String id) {
        Factory factory = getFactory(id);
        if (factory == null)
            return false;
        if (factory.getOwner() != null)
            return false;

        double price = factory.getPrice();
        if (!plugin.getEconomy().has(player, price)) {
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, price);
        factory.setOwner(player.getUniqueId());
        factory.setStatus(FactoryStatus.STOPPED);
        saveAll();

        return true;
    }

    public boolean sellFactory(Player player, String id) {
        Factory factory = getFactory(id);
        if (factory == null)
            return false;
        if (!player.getUniqueId().equals(factory.getOwner()))
            return false;

        double sellPrice = factory.getPrice() * plugin.getConfig()
                .getDouble("factory.sell-price-multiplier", 0.5);

        plugin.getEconomy().depositPlayer(player, sellPrice);
        factory.setOwner(null);
        factory.setStatus(FactoryStatus.STOPPED);
        factory.setCurrentProduction(null);

        // Clear storage
        plugin.getStorageManager().clearStorage(id);

        saveAll();
        return true;
    }

    public void startProduction(Factory factory, String recipeId) {
        Recipe recipe = plugin.getRecipeManager().getRecipe(recipeId);
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

    public void updateProduction() {
        for (Factory factory : factories.values()) {
            // Only process running factories
            if (factory.getStatus() != FactoryStatus.RUNNING)
                continue;
            if (factory.getCurrentProduction() == null) {
                // If factory is marked as running but has no production task, stop it
                factory.setStatus(FactoryStatus.STOPPED);
                continue;
            }

            ProductionTask task = factory.getCurrentProduction();
            if (task.isComplete()) {
                // Only complete if not already completed (prevent multiple calls)
                if (factory.getStatus() == FactoryStatus.RUNNING) {
                    completeProduction(factory);
                }
            } else {
                updateProductionBossBar(factory, task);
            }
        }
    }

    private void completeProduction(Factory factory) {
        // Double-check that factory is still running and has a production task
        if (factory.getStatus() != FactoryStatus.RUNNING || factory.getCurrentProduction() == null) {
            return;
        }

        ProductionTask task = factory.getCurrentProduction();
        Recipe recipe = plugin.getRecipeManager().getRecipe(task.getRecipeId());

        if (recipe != null) {
            // Add outputs to output storage
            for (Map.Entry<String, Integer> output : recipe.getOutputs().entrySet()) {
                plugin.getStorageManager().addOutputItem(factory.getId(), output.getKey(), output.getValue());
            }

            // Execute console commands
            for (String cmd : recipe.getConsoleCommands()) {
                String command = cmd.replace("{player}",
                        Bukkit.getOfflinePlayer(factory.getOwner()).getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }

        }

        // Clear production task and set status to stopped
        factory.setCurrentProduction(null);
        factory.setStatus(FactoryStatus.STOPPED);

        // Remove bossbar immediately
        String factoryId = factory.getId();
        BossBar bossBar = productionBossBars.remove(factoryId);
        if (bossBar != null) {
            bossBar.removeAll();
        }

        // Send completion message only once (prevent spam)
        Player owner = Bukkit.getPlayer(factory.getOwner());
        if (owner != null) {
            String recipeName = recipe.getName();
            if (recipeName == null) {
                recipeName = "Unknown Recipe";
            }
            String msg = plugin.getLanguageManager().getMessage("production-complete")
                    .replace("{recipe}", recipeName);
            owner.sendMessage(msg);

            // Play sound
            if (plugin.getConfig().getBoolean("notifications.sound.enabled")) {
                try {
                    owner.playSound(owner.getLocation(),
                            plugin.getConfig().getString("notifications.sound.production-complete"),
                            1.0f, 1.0f);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to play production complete sound: " +
                            plugin.getConfig().getString("notifications.sound.production-complete"));
                }
            }

            // Show title
            if (plugin.getConfig().getBoolean("notifications.titles.enabled")) {
                owner.sendTitle(
                        plugin.getLanguageManager().getMessage("titles.production-complete.title"),
                        plugin.getLanguageManager().getMessage("titles.production-complete.subtitle")
                                .replace("{recipe}", recipe.getName()),
                        10, 40, 10);
            }
        }

        saveAll();
    }

    private void createProductionBossBar(Factory factory, Recipe recipe) {
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

    private void updateProductionBossBar(Factory factory, ProductionTask task) {
        String factoryId = factory.getId();
        BossBar bossBar = productionBossBars.get(factoryId);
        if (bossBar == null)
            return;

        Recipe recipe = plugin.getRecipeManager().getRecipe(task.getRecipeId());
        if (recipe == null)
            return;

        double progress = task.getProgress();
        int percent = (int) (progress * 100);

        String title = plugin.getLanguageManager().getMessage("bossbar.production")
                .replace("{recipe}", recipe.getName())
                .replace("{percent}", String.valueOf(percent));

        bossBar.setTitle(title);
        bossBar.setProgress(progress);
    }

    public boolean upgradeFactory(Player player, String id) {
        Factory factory = getFactory(id);
        if (factory == null)
            return false;
        if (!player.getUniqueId().equals(factory.getOwner()))
            return false;

        int maxLevel = plugin.getConfig().getInt("factory.max-level", 5);
        if (factory.getLevel() >= maxLevel)
            return false;

        double upgradeCost = factory.getPrice() * 0.5 * factory.getLevel();
        if (!plugin.getEconomy().has(player, upgradeCost))
            return false;

        plugin.getEconomy().withdrawPlayer(player, upgradeCost);
        factory.setLevel(factory.getLevel() + 1);
        saveAll();

        return true;
    }

    public boolean teleportPlayer(Player player, String factoryId) {
        Factory factory = getFactory(factoryId);
        if (factory == null)
            return false;

        // Check if player is the owner
        if (!player.getUniqueId().equals(factory.getOwner())) {
            return false;
        }

        Location location = factory.getFastTravelLocation();
        if (location == null) {
            return false;
        }

        // Teleport player to factory location
        player.teleport(location);
        return true;
    }
}