package com.aithor.factorycore.managers;

import com.aithor.factorycore.FactoryCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * AchievementManager - Handles the achievement/badge system.
 * <p>
 * Tracks player progress toward achievements, awards them when thresholds are met,
 * and displays toast notifications with sound effects.
 * </p>
 */
public class AchievementManager {

    private final FactoryCore plugin;
    private FileConfiguration achievementConfig;

    // Player achievement data: playerId -> AchievementData
    private final Map<UUID, PlayerAchievementData> playerData;
    private final File dataFile;

    public AchievementManager(FactoryCore plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.dataFile = new File(dataFolder, "achievements.yml");

        loadAchievementConfig();
        loadPlayerData();
    }

    // ── Config Loading ───────────────────────────────────────────────────────

    private void loadAchievementConfig() {
        File configFile = new File(plugin.getDataFolder(), "achievement.yml");
        if (!configFile.exists()) {
            plugin.saveResource("achievement.yml", false);
        }
        achievementConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig() {
        loadAchievementConfig();
    }

    public boolean isEnabled() {
        return achievementConfig.getBoolean("settings.enabled", true);
    }

    // ── Achievement Config Access ────────────────────────────────────────────

    /**
     * Get all achievement IDs defined in achievement.yml
     */
    public Set<String> getAchievementIds() {
        ConfigurationSection section = achievementConfig.getConfigurationSection("achievements");
        if (section == null) return Collections.emptySet();
        return section.getKeys(false);
    }

    public String getAchievementName(String achievementId) {
        String raw = achievementConfig.getString("achievements." + achievementId + ".name", achievementId);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String getAchievementDescription(String achievementId) {
        String raw = achievementConfig.getString("achievements." + achievementId + ".description", "");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String getAchievementIcon(String achievementId) {
        return achievementConfig.getString("achievements." + achievementId + ".icon", "PAPER");
    }

    public String getAchievementType(String achievementId) {
        return achievementConfig.getString("achievements." + achievementId + ".type", "");
    }

    public double getAchievementThreshold(String achievementId) {
        return achievementConfig.getDouble("achievements." + achievementId + ".threshold", 1);
    }

    // ── Player Data Access ───────────────────────────────────────────────────

    /**
     * Check if a player has unlocked an achievement
     */
    public boolean hasAchievement(UUID playerId, String achievementId) {
        PlayerAchievementData data = playerData.get(playerId);
        if (data == null) return false;
        return data.unlockedAchievements.contains(achievementId);
    }

    /**
     * Get the current progress value for a cumulative achievement
     */
    public double getProgress(UUID playerId, String achievementId) {
        PlayerAchievementData data = playerData.get(playerId);
        if (data == null) return 0;
        return data.progressValues.getOrDefault(achievementId, 0.0);
    }

    /**
     * Get the set of all unlocked achievements for a player
     */
    public Set<String> getUnlockedAchievements(UUID playerId) {
        PlayerAchievementData data = playerData.get(playerId);
        if (data == null) return Collections.emptySet();
        return Collections.unmodifiableSet(data.unlockedAchievements);
    }

    /**
     * Get the total number of achievements
     */
    public int getTotalAchievementCount() {
        return getAchievementIds().size();
    }

    /**
     * Get the number of unlocked achievements for a player
     */
    public int getUnlockedCount(UUID playerId) {
        PlayerAchievementData data = playerData.get(playerId);
        if (data == null) return 0;
        return data.unlockedAchievements.size();
    }

    // ── Achievement Triggers ─────────────────────────────────────────────────

    /**
     * Award a "first-time" achievement (e.g., first factory purchase).
     * Does nothing if already unlocked.
     */
    public void awardAchievement(Player player, String achievementId) {
        if (!isEnabled()) return;
        UUID playerId = player.getUniqueId();

        if (hasAchievement(playerId, achievementId)) return;

        // Unlock
        PlayerAchievementData data = playerData.computeIfAbsent(playerId, k -> new PlayerAchievementData());
        data.unlockedAchievements.add(achievementId);
        data.unlockTimestamps.put(achievementId, System.currentTimeMillis());

        saveAll();
        showAchievementToast(player, achievementId);
    }

    /**
     * Add progress toward a cumulative achievement.
     * Automatically awards the achievement when the threshold is reached.
     *
     * @param player        The player
     * @param achievementId The achievement ID
     * @param amount        The amount to add to progress
     */
    public void addProgress(Player player, String achievementId, double amount) {
        if (!isEnabled()) return;
        UUID playerId = player.getUniqueId();

        if (hasAchievement(playerId, achievementId)) return;

        PlayerAchievementData data = playerData.computeIfAbsent(playerId, k -> new PlayerAchievementData());
        double currentProgress = data.progressValues.getOrDefault(achievementId, 0.0);
        double newProgress = currentProgress + amount;
        data.progressValues.put(achievementId, newProgress);

        double threshold = getAchievementThreshold(achievementId);
        if (newProgress >= threshold) {
            data.unlockedAchievements.add(achievementId);
            data.unlockTimestamps.put(achievementId, System.currentTimeMillis());
            showAchievementToast(player, achievementId);
        }

        saveAll();
    }

    /**
     * Add progress for an offline player (e.g., marketplace sale while offline).
     * Toast will not be shown; the player sees it next login via pending check.
     */
    public void addProgressOffline(UUID playerId, String achievementId, double amount) {
        if (!isEnabled()) return;
        if (hasAchievement(playerId, achievementId)) return;

        PlayerAchievementData data = playerData.computeIfAbsent(playerId, k -> new PlayerAchievementData());
        double currentProgress = data.progressValues.getOrDefault(achievementId, 0.0);
        double newProgress = currentProgress + amount;
        data.progressValues.put(achievementId, newProgress);

        double threshold = getAchievementThreshold(achievementId);
        if (newProgress >= threshold) {
            data.unlockedAchievements.add(achievementId);
            data.unlockTimestamps.put(achievementId, System.currentTimeMillis());
        }

        saveAll();
    }

    // ── Toast Notification ───────────────────────────────────────────────────

    private void showAchievementToast(Player player, String achievementId) {
        String name = getAchievementName(achievementId);
        String description = getAchievementDescription(achievementId);

        // Title toast
        int fadeIn = achievementConfig.getInt("settings.toast-fade-in", 10);
        int stay = achievementConfig.getInt("settings.toast-stay", 70);
        int fadeOut = achievementConfig.getInt("settings.toast-fade-out", 20);

        player.sendTitle(
                "§6§lACHIEVEMENT UNLOCKED!",
                name + " §8- §7" + description,
                fadeIn, stay, fadeOut);

        // Chat message
        player.sendMessage("");
        player.sendMessage("§8§m----------------------------------------");
        player.sendMessage("§6§l  ACHIEVEMENT UNLOCKED!");
        player.sendMessage("§f  " + name);
        player.sendMessage("§7  " + description);
        player.sendMessage("§8§m----------------------------------------");
        player.sendMessage("");

        // Sound effect
        String soundName = achievementConfig.getString("achievements." + achievementId + ".sound",
                "ENTITY_PLAYER_LEVELUP");
        float volume = (float) achievementConfig.getDouble("achievements." + achievementId + ".sound-volume", 1.0);
        float pitch = (float) achievementConfig.getDouble("achievements." + achievementId + ".sound-pitch", 1.2);

        Sound sound = parseSound(soundName);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    // ── Persistence ─────────────────────────────────────────────────────────

    private void loadPlayerData() {
        if (!dataFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        if (!config.contains("players")) return;

        for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(uuidStr);
                PlayerAchievementData data = new PlayerAchievementData();

                String basePath = "players." + uuidStr;

                // Load unlocked achievements
                if (config.contains(basePath + ".unlocked")) {
                    List<String> unlocked = config.getStringList(basePath + ".unlocked");
                    data.unlockedAchievements.addAll(unlocked);
                }

                // Load unlock timestamps
                if (config.contains(basePath + ".timestamps")) {
                    ConfigurationSection tsSection = config.getConfigurationSection(basePath + ".timestamps");
                    if (tsSection != null) {
                        for (String achId : tsSection.getKeys(false)) {
                            data.unlockTimestamps.put(achId, tsSection.getLong(achId));
                        }
                    }
                }

                // Load progress values
                if (config.contains(basePath + ".progress")) {
                    ConfigurationSection progSection = config.getConfigurationSection(basePath + ".progress");
                    if (progSection != null) {
                        for (String achId : progSection.getKeys(false)) {
                            data.progressValues.put(achId, progSection.getDouble(achId));
                        }
                    }
                }

                playerData.put(playerId, data);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load achievement data for: " + uuidStr);
            }
        }

        plugin.getLogger().info("Loaded achievement data for " + playerData.size() + " players!");
    }

    public void saveAll() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, PlayerAchievementData> entry : playerData.entrySet()) {
            String basePath = "players." + entry.getKey().toString();
            PlayerAchievementData data = entry.getValue();

            // Save unlocked achievements as list
            config.set(basePath + ".unlocked", new ArrayList<>(data.unlockedAchievements));

            // Save timestamps
            for (Map.Entry<String, Long> ts : data.unlockTimestamps.entrySet()) {
                config.set(basePath + ".timestamps." + ts.getKey(), ts.getValue());
            }

            // Save progress values
            for (Map.Entry<String, Double> prog : data.progressValues.entrySet()) {
                config.set(basePath + ".progress." + prog.getKey(), prog.getValue());
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save achievement data!");
            e.printStackTrace();
        }
    }

    /**
     * Get the unlock timestamp for an achievement (0 if not unlocked)
     */
    public long getUnlockTimestamp(UUID playerId, String achievementId) {
        PlayerAchievementData data = playerData.get(playerId);
        if (data == null) return 0;
        return data.unlockTimestamps.getOrDefault(achievementId, 0L);
    }

    // ── Sound Utility ────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private Sound parseSound(String name) {
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            Sound s = Registry.SOUNDS.get(key);
            if (s != null) return s;
        } catch (Exception ignored) {
        }
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    // ── Inner Class ─────────────────────────────────────────────────────────

    private static class PlayerAchievementData {
        final Set<String> unlockedAchievements = new LinkedHashSet<>();
        final Map<String, Long> unlockTimestamps = new HashMap<>();
        final Map<String, Double> progressValues = new HashMap<>();
    }
}
