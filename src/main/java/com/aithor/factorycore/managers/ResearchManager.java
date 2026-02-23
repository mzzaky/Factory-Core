package com.aithor.factorycore.managers;

import com.aithor.factorycore.FactoryCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * ResearchManager - Handles the Factory Research system.
 * <p>
 * Each player can research technologies that grant permanent buffs.
 * Research requires money and time to complete.
 * </p>
 */
public class ResearchManager {

    private final FactoryCore plugin;
    private FileConfiguration researchConfig;

    // Player research data: playerId -> (researchId -> ResearchData)
    private final Map<UUID, Map<String, ResearchData>> playerResearch;
    private final File dataFile;

    public ResearchManager(FactoryCore plugin) {
        this.plugin = plugin;
        this.playerResearch = new HashMap<>();

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.dataFile = new File(dataFolder, "research.yml");

        loadResearchConfig();
        loadPlayerData();
    }

    // ── Config Loading ───────────────────────────────────────────────────────

    /**
     * Load research definitions from resources/research.yml
     */
    private void loadResearchConfig() {
        File configFile = new File(plugin.getDataFolder(), "research.yml");
        if (!configFile.exists()) {
            plugin.saveResource("research.yml", false);
        }
        researchConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Reload the research config (for /fc admin reload)
     */
    public void reloadConfig() {
        loadResearchConfig();
    }

    /**
     * Get the research config section
     */
    public FileConfiguration getResearchConfig() {
        return researchConfig;
    }

    /**
     * Get all research IDs defined in research.yml
     */
    public Set<String> getResearchIds() {
        ConfigurationSection section = researchConfig.getConfigurationSection("research");
        if (section == null)
            return Collections.emptySet();
        return section.getKeys(false);
    }

    /**
     * Get display name of a research
     */
    public String getResearchName(String researchId) {
        return researchConfig.getString("research." + researchId + ".name", researchId);
    }

    /**
     * Get description lines of a research
     */
    public List<String> getResearchDescription(String researchId) {
        return researchConfig.getStringList("research." + researchId + ".description");
    }

    /**
     * Get the icon material name for a research
     */
    public String getResearchIcon(String researchId) {
        return researchConfig.getString("research." + researchId + ".icon", "PAPER");
    }

    /**
     * Get max level for a research
     */
    public int getMaxLevel(String researchId) {
        return researchConfig.getInt("research." + researchId + ".max-level", 1);
    }

    /**
     * Get unlock cost for a specific level
     */
    public double getResearchCost(String researchId, int level) {
        return researchConfig.getDouble("research." + researchId + ".cost." + level, 0);
    }

    /**
     * Get duration in minutes for a specific level
     */
    public int getResearchDuration(String researchId, int level) {
        return researchConfig.getInt("research." + researchId + ".duration." + level, 1440);
    }

    /**
     * Get buff value per level
     */
    public double getBuffPerLevel(String researchId) {
        return researchConfig.getDouble("research." + researchId + ".buff-per-level", 0);
    }

    /**
     * Get buff type (percentage or flat)
     */
    public String getBuffType(String researchId) {
        return researchConfig.getString("research." + researchId + ".buff-type", "percentage");
    }

    /**
     * Get buff description template
     */
    public String getBuffDescription(String researchId) {
        return researchConfig.getString("research." + researchId + ".buff-description", "");
    }

    // ── Player Data ─────────────────────────────────────────────────────────

    /**
     * Get a player's current completed level for a research (0 = not researched)
     */
    public int getPlayerResearchLevel(UUID playerId, String researchId) {
        Map<String, ResearchData> data = playerResearch.get(playerId);
        if (data == null)
            return 0;
        ResearchData rd = data.get(researchId);
        if (rd == null)
            return 0;
        return rd.completedLevel;
    }

    /**
     * Check if a player is currently researching something
     */
    public boolean isResearching(UUID playerId, String researchId) {
        Map<String, ResearchData> data = playerResearch.get(playerId);
        if (data == null)
            return false;
        ResearchData rd = data.get(researchId);
        if (rd == null)
            return false;
        return rd.researchStartTime > 0;
    }

    /**
     * Get the remaining research time in seconds
     */
    public int getRemainingResearchTime(UUID playerId, String researchId) {
        Map<String, ResearchData> data = playerResearch.get(playerId);
        if (data == null)
            return 0;
        ResearchData rd = data.get(researchId);
        if (rd == null)
            return 0;
        if (rd.researchStartTime <= 0)
            return 0;

        long elapsedMs = System.currentTimeMillis() - rd.researchStartTime;
        long totalMs = (long) rd.researchDurationMinutes * 60 * 1000;
        long remainingMs = totalMs - elapsedMs;
        return (int) Math.max(0, remainingMs / 1000);
    }

    /**
     * Get research progress as a fraction 0.0 - 1.0
     */
    public double getResearchProgress(UUID playerId, String researchId) {
        Map<String, ResearchData> data = playerResearch.get(playerId);
        if (data == null)
            return 0;
        ResearchData rd = data.get(researchId);
        if (rd == null)
            return 0;
        if (rd.researchStartTime <= 0)
            return 0;

        long elapsedMs = System.currentTimeMillis() - rd.researchStartTime;
        long totalMs = (long) rd.researchDurationMinutes * 60 * 1000;
        return Math.min(1.0, (double) elapsedMs / totalMs);
    }

    /**
     * Check if a research timer has completed
     */
    public boolean isResearchComplete(UUID playerId, String researchId) {
        return isResearching(playerId, researchId) && getRemainingResearchTime(playerId, researchId) == 0;
    }

    /**
     * Start a research for a player
     *
     * @return true if research was started successfully
     */
    public boolean startResearch(Player player, String researchId) {
        UUID playerId = player.getUniqueId();
        int currentLevel = getPlayerResearchLevel(playerId, researchId);
        int maxLevel = getMaxLevel(researchId);

        // Already maxed
        if (currentLevel >= maxLevel) {
            player.sendMessage(plugin.getLanguageManager().getMessage("research-max-level"));
            return false;
        }

        // Already researching this one
        if (isResearching(playerId, researchId)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("research-in-progress"));
            return false;
        }

        int nextLevel = currentLevel + 1;
        double cost = getResearchCost(researchId, nextLevel);
        int durationMinutes = getResearchDuration(researchId, nextLevel);

        // Check funds
        if (!plugin.getEconomy().has(player, cost)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("insufficient-funds")
                    .replace("{amount}", String.format("%.2f", cost)));
            return false;
        }

        // Withdraw money
        plugin.getEconomy().withdrawPlayer(player, cost);

        // Create / update research data
        Map<String, ResearchData> data = playerResearch.computeIfAbsent(playerId, k -> new HashMap<>());
        ResearchData rd = data.computeIfAbsent(researchId, k -> new ResearchData());
        rd.researchStartTime = System.currentTimeMillis();
        rd.researchDurationMinutes = durationMinutes;
        rd.targetLevel = nextLevel;

        saveAll();

        player.sendMessage(plugin.getLanguageManager().getMessage("research-started")
                .replace("{research}", getResearchName(researchId))
                .replace("{time}", formatDuration(durationMinutes)));

        return true;
    }

    /**
     * Forcefully upgrade a research for a player (Admin)
     */
    public boolean forceUpgradeResearch(UUID playerId, String researchId) {
        int currentLevel = getPlayerResearchLevel(playerId, researchId);
        int maxLevel = getMaxLevel(researchId);

        if (currentLevel >= maxLevel) {
            return false;
        }

        Map<String, ResearchData> data = playerResearch.computeIfAbsent(playerId, k -> new HashMap<>());
        ResearchData rd = data.computeIfAbsent(researchId, k -> new ResearchData());

        rd.completedLevel = currentLevel + 1;
        rd.researchStartTime = -1;
        rd.researchDurationMinutes = 0;
        rd.targetLevel = 0;

        saveAll();
        return true;
    }

    /**
     * Forcefully set a research level for a player (Admin)
     */
    public boolean forceSetResearchLevel(UUID playerId, String researchId, int level) {
        int maxLevel = getMaxLevel(researchId);

        if (level < 0 || level > maxLevel) {
            return false;
        }

        Map<String, ResearchData> data = playerResearch.computeIfAbsent(playerId, k -> new HashMap<>());
        ResearchData rd = data.computeIfAbsent(researchId, k -> new ResearchData());

        rd.completedLevel = level;
        rd.researchStartTime = -1;
        rd.researchDurationMinutes = 0;
        rd.targetLevel = 0;

        saveAll();
        return true;
    }

    /**
     * Complete a research that has finished its timer
     */
    public void completeResearch(UUID playerId, String researchId) {
        Map<String, ResearchData> data = playerResearch.get(playerId);
        if (data == null)
            return;
        ResearchData rd = data.get(researchId);
        if (rd == null)
            return;

        rd.completedLevel = rd.targetLevel;
        rd.researchStartTime = -1;
        rd.researchDurationMinutes = 0;
        rd.targetLevel = 0;

        saveAll();

        // Notify player if online
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("research-completed")
                    .replace("{research}", getResearchName(researchId))
                    .replace("{level}", String.valueOf(rd.completedLevel)));

            // Play sound
            if (plugin.getConfig().getBoolean("notifications.sound.enabled")) {
                try {
                    player.playSound(player.getLocation(), "ENTITY_PLAYER_LEVELUP", 1.0f, 1.5f);
                } catch (Exception ignored) {
                }
            }

            // Show title
            if (plugin.getConfig().getBoolean("notifications.titles.enabled")) {
                player.sendTitle(
                        "§a§lRESEARCH COMPLETE!",
                        "§e" + getResearchName(researchId) + " §7Level §6" + rd.completedLevel,
                        10, 60, 20);
            }
        }
    }

    /**
     * Called by scheduler to update all ongoing research
     */
    public void updateResearch() {
        for (Map.Entry<UUID, Map<String, ResearchData>> entry : playerResearch.entrySet()) {
            UUID playerId = entry.getKey();
            for (Map.Entry<String, ResearchData> researchEntry : entry.getValue().entrySet()) {
                String researchId = researchEntry.getKey();
                if (isResearchComplete(playerId, researchId)) {
                    completeResearch(playerId, researchId);
                }
            }
        }
    }

    // ── Buff Calculations ───────────────────────────────────────────────────

    /**
     * Get the total buff value for a player on a specific research.
     * For percentage buffs: returns the total reduction percentage (e.g. 15.0 for
     * 15%).
     * For flat buffs: returns the total flat bonus (e.g. 2 for +2).
     */
    public double getBuffValue(UUID playerId, String researchId) {
        int level = getPlayerResearchLevel(playerId, researchId);
        if (level <= 0)
            return 0;
        return level * getBuffPerLevel(researchId);
    }

    /**
     * Get the tax reduction percentage for a player (from fiscal_optimization)
     */
    public double getTaxReduction(UUID playerId) {
        return getBuffValue(playerId, "fiscal_optimization");
    }

    /**
     * Get the salary reduction percentage for a player (from
     * ai_workforce_integration)
     */
    public double getSalaryReduction(UUID playerId) {
        return getBuffValue(playerId, "ai_workforce_integration");
    }

    /**
     * Get the upgrade time reduction percentage (from nano_construction_framework)
     */
    public double getUpgradeTimeReduction(UUID playerId) {
        return getBuffValue(playerId, "nano_construction_framework");
    }

    /**
     * Get the production time reduction percentage (from
     * advanced_machine_technology)
     */
    public double getProductionTimeReduction(UUID playerId) {
        return getBuffValue(playerId, "advanced_machine_technology");
    }

    /**
     * Get the production cost reduction percentage (from
     * operational_cost_efficiency)
     */
    public double getProductionCostReduction(UUID playerId) {
        return getBuffValue(playerId, "operational_cost_efficiency");
    }

    /**
     * Get the additional factory limit (from industrial_mastery)
     */
    public int getAdditionalFactoryLimit(UUID playerId) {
        return (int) getBuffValue(playerId, "industrial_mastery");
    }

    /**
     * Get the additional marketplace listing limit (from business_expansion)
     */
    public int getAdditionalListingLimit(UUID playerId) {
        return (int) getBuffValue(playerId, "business_expansion");
    }

    /**
     * Get the additional marketplace listing expiration hours (from
     * market_stability_regulation)
     */
    public int getAdditionalListingHours(UUID playerId) {
        return (int) getBuffValue(playerId, "market_stability_regulation");
    }

    // ── Persistence ─────────────────────────────────────────────────────────

    private void loadPlayerData() {
        if (!dataFile.exists())
            return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        if (!config.contains("players"))
            return;

        for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
            UUID playerId = UUID.fromString(uuidStr);
            Map<String, ResearchData> data = new HashMap<>();

            ConfigurationSection playerSection = config.getConfigurationSection("players." + uuidStr);
            if (playerSection != null) {
                for (String researchId : playerSection.getKeys(false)) {
                    String path = "players." + uuidStr + "." + researchId;
                    ResearchData rd = new ResearchData();
                    rd.completedLevel = config.getInt(path + ".completed-level", 0);
                    rd.researchStartTime = config.getLong(path + ".research-start-time", -1);
                    rd.researchDurationMinutes = config.getInt(path + ".research-duration-minutes", 0);
                    rd.targetLevel = config.getInt(path + ".target-level", 0);
                    data.put(researchId, rd);
                }
            }

            playerResearch.put(playerId, data);
        }

        plugin.getLogger().info("Loaded research data for " + playerResearch.size() + " players!");
    }

    public void saveAll() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Map<String, ResearchData>> entry : playerResearch.entrySet()) {
            String uuidStr = entry.getKey().toString();
            for (Map.Entry<String, ResearchData> researchEntry : entry.getValue().entrySet()) {
                String path = "players." + uuidStr + "." + researchEntry.getKey();
                ResearchData rd = researchEntry.getValue();
                config.set(path + ".completed-level", rd.completedLevel);
                config.set(path + ".research-start-time", rd.researchStartTime);
                config.set(path + ".research-duration-minutes", rd.researchDurationMinutes);
                config.set(path + ".target-level", rd.targetLevel);
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save research data!");
            e.printStackTrace();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Format duration in minutes to a human-readable string
     */
    public String formatDuration(int totalMinutes) {
        if (totalMinutes >= 1440) {
            int days = totalMinutes / 1440;
            int hours = (totalMinutes % 1440) / 60;
            if (hours > 0) {
                return days + "d " + hours + "h";
            }
            return days + "d";
        }
        if (totalMinutes >= 60) {
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;
            if (minutes > 0) {
                return hours + "h " + minutes + "m";
            }
            return hours + "h";
        }
        return totalMinutes + "m";
    }

    /**
     * Format seconds to a readable string
     */
    public String formatSeconds(int totalSeconds) {
        if (totalSeconds >= 86400) {
            int days = totalSeconds / 86400;
            int hours = (totalSeconds % 86400) / 3600;
            if (hours > 0) {
                return days + "d " + hours + "h";
            }
            return days + "d";
        }
        if (totalSeconds >= 3600) {
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            if (minutes > 0) {
                return hours + "h " + minutes + "m";
            }
            return hours + "h";
        }
        if (totalSeconds >= 60) {
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            if (seconds > 0) {
                return minutes + "m " + seconds + "s";
            }
            return minutes + "m";
        }
        return totalSeconds + "s";
    }

    // ── Inner Class ─────────────────────────────────────────────────────────

    public static class ResearchData {
        public int completedLevel = 0;
        public long researchStartTime = -1; // epoch ms, -1 = not researching
        public int researchDurationMinutes = 0;
        public int targetLevel = 0; // the level being researched
    }
}
