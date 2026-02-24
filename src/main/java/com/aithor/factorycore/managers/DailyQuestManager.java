package com.aithor.factorycore.managers;

import com.aithor.factorycore.FactoryCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

/**
 * DailyQuestManager - Handles daily quest tracking, progress, rewards and reset.
 * <p>
 * Tracks per-player daily quest progress, automatically resets at the configured hour,
 * awards EXP and money (Vault) rewards, and plays sound/notification on completion.
 * </p>
 */
public class DailyQuestManager {

    private final FactoryCore plugin;
    private FileConfiguration questConfig;

    // Player quest data: playerId -> PlayerQuestData
    private final Map<UUID, PlayerQuestData> playerData;
    private final File dataFile;

    public DailyQuestManager(FactoryCore plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.dataFile = new File(dataFolder, "daily-quests.yml");

        loadQuestConfig();
        loadPlayerData();
    }

    // ── Config Loading ───────────────────────────────────────────────────────

    private void loadQuestConfig() {
        File configFile = new File(plugin.getDataFolder(), "daily-quest.yml");
        if (!configFile.exists()) {
            plugin.saveResource("daily-quest.yml", false);
        }
        questConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig() {
        loadQuestConfig();
    }

    public boolean isEnabled() {
        return questConfig.getBoolean("settings.enabled", true);
    }

    // ── Quest Config Access ──────────────────────────────────────────────────

    /**
     * Get all quest IDs defined in daily-quest.yml
     */
    public Set<String> getQuestIds() {
        ConfigurationSection section = questConfig.getConfigurationSection("quests");
        if (section == null) return Collections.emptySet();
        return section.getKeys(false);
    }

    public String getQuestName(String questId) {
        String raw = questConfig.getString("quests." + questId + ".name", questId);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String getQuestDescription(String questId) {
        String raw = questConfig.getString("quests." + questId + ".description", "");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String getQuestIcon(String questId) {
        return questConfig.getString("quests." + questId + ".icon", "PAPER");
    }

    public String getQuestType(String questId) {
        return questConfig.getString("quests." + questId + ".type", "");
    }

    public int getQuestTarget(String questId) {
        return questConfig.getInt("quests." + questId + ".target", 1);
    }

    public int getQuestRewardExp(String questId) {
        return questConfig.getInt("quests." + questId + ".reward.exp", 0);
    }

    public double getQuestRewardMoney(String questId) {
        return questConfig.getDouble("quests." + questId + ".reward.money", 0.0);
    }

    public int getTotalQuestCount() {
        return getQuestIds().size();
    }

    // Bonus reward config
    public boolean isBonusEnabled() {
        return questConfig.getBoolean("all-complete-bonus.enabled", true);
    }

    public int getBonusExp() {
        return questConfig.getInt("all-complete-bonus.exp", 500);
    }

    public double getBonusMoney() {
        return questConfig.getDouble("all-complete-bonus.money", 25000.0);
    }

    // ── Player Data Access ───────────────────────────────────────────────────

    /**
     * Get the current progress for a quest
     */
    public int getProgress(UUID playerId, String questId) {
        checkAndResetDaily(playerId);
        PlayerQuestData data = playerData.get(playerId);
        if (data == null) return 0;
        return data.questProgress.getOrDefault(questId, 0);
    }

    /**
     * Check if a quest is completed (progress >= target)
     */
    public boolean isQuestCompleted(UUID playerId, String questId) {
        return getProgress(playerId, questId) >= getQuestTarget(questId);
    }

    /**
     * Check if reward has been claimed for a quest
     */
    public boolean isRewardClaimed(UUID playerId, String questId) {
        checkAndResetDaily(playerId);
        PlayerQuestData data = playerData.get(playerId);
        if (data == null) return false;
        return data.claimedRewards.contains(questId);
    }

    /**
     * Check if the all-complete bonus has been claimed
     */
    public boolean isBonusClaimed(UUID playerId) {
        checkAndResetDaily(playerId);
        PlayerQuestData data = playerData.get(playerId);
        if (data == null) return false;
        return data.bonusClaimed;
    }

    /**
     * Get the number of completed quests for a player
     */
    public int getCompletedCount(UUID playerId) {
        checkAndResetDaily(playerId);
        int count = 0;
        for (String questId : getQuestIds()) {
            if (isQuestCompleted(playerId, questId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Check if all quests are completed
     */
    public boolean areAllQuestsCompleted(UUID playerId) {
        return getCompletedCount(playerId) >= getTotalQuestCount();
    }

    // ── Quest Progress Tracking ──────────────────────────────────────────────

    /**
     * Add progress to a quest by type.
     * Automatically awards the quest when the target is reached.
     *
     * @param player    The player
     * @param questType The quest type (e.g., PRODUCTION_COMPLETE, SALARY_PAYMENT)
     * @param amount    The amount to add
     */
    public void addProgressByType(Player player, String questType, int amount) {
        if (!isEnabled()) return;
        UUID playerId = player.getUniqueId();
        checkAndResetDaily(playerId);

        for (String questId : getQuestIds()) {
            if (getQuestType(questId).equals(questType)) {
                addProgress(player, questId, amount);
            }
        }
    }

    /**
     * Add progress to a specific quest.
     */
    public void addProgress(Player player, String questId, int amount) {
        if (!isEnabled()) return;
        UUID playerId = player.getUniqueId();
        checkAndResetDaily(playerId);

        PlayerQuestData data = playerData.computeIfAbsent(playerId, k -> new PlayerQuestData());

        int currentProgress = data.questProgress.getOrDefault(questId, 0);
        int target = getQuestTarget(questId);

        // Already completed, no need to add more
        if (currentProgress >= target) return;

        int newProgress = Math.min(currentProgress + amount, target);
        data.questProgress.put(questId, newProgress);

        // Check if just completed
        if (newProgress >= target && currentProgress < target) {
            showQuestCompleteNotification(player, questId);
        }

        saveAll();
    }

    /**
     * Claim reward for a completed quest
     *
     * @return true if reward was claimed successfully
     */
    public boolean claimReward(Player player, String questId) {
        if (!isEnabled()) return false;
        UUID playerId = player.getUniqueId();
        checkAndResetDaily(playerId);

        if (!isQuestCompleted(playerId, questId)) return false;
        if (isRewardClaimed(playerId, questId)) return false;

        PlayerQuestData data = playerData.computeIfAbsent(playerId, k -> new PlayerQuestData());
        data.claimedRewards.add(questId);

        // Give EXP reward
        int exp = getQuestRewardExp(questId);
        if (exp > 0) {
            player.giveExp(exp);
        }

        // Give money reward
        double money = getQuestRewardMoney(questId);
        if (money > 0) {
            plugin.getEconomy().depositPlayer(player, money);
        }

        saveAll();
        return true;
    }

    /**
     * Claim the all-complete bonus reward
     *
     * @return true if bonus was claimed successfully
     */
    public boolean claimBonus(Player player) {
        if (!isEnabled() || !isBonusEnabled()) return false;
        UUID playerId = player.getUniqueId();
        checkAndResetDaily(playerId);

        if (!areAllQuestsCompleted(playerId)) return false;
        if (isBonusClaimed(playerId)) return false;

        PlayerQuestData data = playerData.computeIfAbsent(playerId, k -> new PlayerQuestData());
        data.bonusClaimed = true;

        // Give bonus EXP
        int exp = getBonusExp();
        if (exp > 0) {
            player.giveExp(exp);
        }

        // Give bonus money
        double money = getBonusMoney();
        if (money > 0) {
            plugin.getEconomy().depositPlayer(player, money);
        }

        // Play all-complete sound
        String soundName = questConfig.getString("sound.all-complete", "UI_TOAST_CHALLENGE_COMPLETE");
        float volume = (float) questConfig.getDouble("sound.all-complete-volume", 1.0);
        float pitch = (float) questConfig.getDouble("sound.all-complete-pitch", 1.0);
        Sound sound = parseSound(soundName);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }

        // Show all-complete notification
        int fadeIn = questConfig.getInt("notifications.toast-fade-in", 10);
        int stay = questConfig.getInt("notifications.toast-stay", 60);
        int fadeOut = questConfig.getInt("notifications.toast-fade-out", 20);

        player.sendTitle(
                "§6§lALL QUESTS COMPLETED!",
                "§aBonus reward claimed! §e+" + exp + " EXP §7& §6$" + String.format("%.0f", money),
                fadeIn, stay, fadeOut);

        player.sendMessage("");
        player.sendMessage("§8§m----------------------------------------");
        player.sendMessage("§6§l  ALL DAILY QUESTS COMPLETED!");
        player.sendMessage("§a  Bonus Reward Claimed:");
        player.sendMessage("§e  +" + exp + " EXP §7& §6$" + String.format("%.2f", money));
        player.sendMessage("§8§m----------------------------------------");
        player.sendMessage("");

        saveAll();
        return true;
    }

    // ── Daily Reset ──────────────────────────────────────────────────────────

    /**
     * Check if the player's data needs to be reset for a new day.
     */
    private void checkAndResetDaily(UUID playerId) {
        PlayerQuestData data = playerData.get(playerId);
        if (data == null) return;

        long resetTimestamp = getResetTimestampForToday();

        if (data.lastResetTimestamp < resetTimestamp) {
            // Reset all quest progress
            data.questProgress.clear();
            data.claimedRewards.clear();
            data.bonusClaimed = false;
            data.lastResetTimestamp = resetTimestamp;
        }
    }

    /**
     * Get the reset timestamp for today based on configured reset hour.
     */
    private long getResetTimestampForToday() {
        int resetHour = questConfig.getInt("settings.reset-hour", 0);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resetToday = LocalDate.now().atTime(LocalTime.of(resetHour, 0));

        // If current time is before today's reset, use yesterday's reset
        if (now.isBefore(resetToday)) {
            resetToday = resetToday.minusDays(1);
        }

        return resetToday.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * Get time remaining until next daily reset in milliseconds.
     */
    public long getTimeUntilReset() {
        int resetHour = questConfig.getInt("settings.reset-hour", 0);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReset = LocalDate.now().atTime(LocalTime.of(resetHour, 0));

        if (now.isAfter(nextReset) || now.isEqual(nextReset)) {
            nextReset = nextReset.plusDays(1);
        }

        long nextResetMs = nextReset.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return nextResetMs - System.currentTimeMillis();
    }

    /**
     * Format remaining time as "Xh Ym"
     */
    public String formatTimeRemaining() {
        long ms = getTimeUntilReset();
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private void showQuestCompleteNotification(Player player, String questId) {
        String name = getQuestName(questId);

        // Title toast
        int fadeIn = questConfig.getInt("notifications.toast-fade-in", 10);
        int stay = questConfig.getInt("notifications.toast-stay", 60);
        int fadeOut = questConfig.getInt("notifications.toast-fade-out", 20);

        player.sendTitle(
                "§a§lQUEST COMPLETED!",
                name + " §8- §7Click to claim reward!",
                fadeIn, stay, fadeOut);

        // Chat message
        player.sendMessage("");
        player.sendMessage("§8§m----------------------------------------");
        player.sendMessage("§a§l  DAILY QUEST COMPLETED!");
        player.sendMessage("§f  " + name);
        player.sendMessage("§7  Open Daily Quests to claim your reward!");
        player.sendMessage("§8§m----------------------------------------");
        player.sendMessage("");

        // Sound effect
        String soundName = questConfig.getString("sound.quest-complete", "ENTITY_PLAYER_LEVELUP");
        float volume = (float) questConfig.getDouble("sound.quest-complete-volume", 1.0);
        float pitch = (float) questConfig.getDouble("sound.quest-complete-pitch", 1.5);
        Sound sound = parseSound(soundName);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private void loadPlayerData() {
        if (!dataFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        if (!config.contains("players")) return;

        for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(uuidStr);
                PlayerQuestData data = new PlayerQuestData();

                String basePath = "players." + uuidStr;

                data.lastResetTimestamp = config.getLong(basePath + ".last-reset", 0);

                // Load quest progress
                if (config.contains(basePath + ".progress")) {
                    ConfigurationSection progSection = config.getConfigurationSection(basePath + ".progress");
                    if (progSection != null) {
                        for (String questId : progSection.getKeys(false)) {
                            data.questProgress.put(questId, progSection.getInt(questId));
                        }
                    }
                }

                // Load claimed rewards
                if (config.contains(basePath + ".claimed")) {
                    List<String> claimed = config.getStringList(basePath + ".claimed");
                    data.claimedRewards.addAll(claimed);
                }

                // Load bonus claimed
                data.bonusClaimed = config.getBoolean(basePath + ".bonus-claimed", false);

                playerData.put(playerId, data);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load daily quest data for: " + uuidStr);
            }
        }

        plugin.getLogger().info("Loaded daily quest data for " + playerData.size() + " players!");
    }

    public void saveAll() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, PlayerQuestData> entry : playerData.entrySet()) {
            String basePath = "players." + entry.getKey().toString();
            PlayerQuestData data = entry.getValue();

            config.set(basePath + ".last-reset", data.lastResetTimestamp);

            // Save quest progress
            for (Map.Entry<String, Integer> prog : data.questProgress.entrySet()) {
                config.set(basePath + ".progress." + prog.getKey(), prog.getValue());
            }

            // Save claimed rewards
            config.set(basePath + ".claimed", new ArrayList<>(data.claimedRewards));

            // Save bonus claimed
            config.set(basePath + ".bonus-claimed", data.bonusClaimed);
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save daily quest data!");
            e.printStackTrace();
        }
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

    // ── Inner Class ──────────────────────────────────────────────────────────

    private static class PlayerQuestData {
        long lastResetTimestamp = 0;
        final Map<String, Integer> questProgress = new HashMap<>();
        final Set<String> claimedRewards = new LinkedHashSet<>();
        boolean bonusClaimed = false;
    }
}
