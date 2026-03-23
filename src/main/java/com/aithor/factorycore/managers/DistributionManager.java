package com.aithor.factorycore.managers;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.ActiveDistributionRequest;
import com.aithor.factorycore.models.Company;
import com.aithor.factorycore.models.DistributionEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DistributionManager - Handles the Distribution Center feature.
 * Manages companies, events, active requests, delivery tracking, and payouts.
 */
public class DistributionManager {

    private final FactoryCore plugin;

    // Configs
    private FileConfiguration companyConfig;
    private FileConfiguration eventConfig;

    // Loaded data
    private final Map<String, Company> companies = new LinkedHashMap<>();
    private final Map<String, DistributionEvent> events = new LinkedHashMap<>();

    // Active requests per player: playerId -> list of active requests
    private final Map<UUID, List<ActiveDistributionRequest>> activeRequests = new HashMap<>();

    // Player distribution tax records: playerId -> total tax paid through distribution
    private final Map<UUID, Double> distributionTaxRecords = new HashMap<>();

    // Data persistence
    private final File dataFile;

    public DistributionManager(FactoryCore plugin) {
        this.plugin = plugin;

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.dataFile = new File(dataFolder, "distribution.yml");

        loadCompanyConfig();
        loadEventConfig();
        loadCompanies();
        loadEvents();
        loadPlayerData();
    }

    // ==================== CONFIG LOADING ====================

    private void loadCompanyConfig() {
        File configFile = new File(plugin.getDataFolder(), "company.yml");
        if (!configFile.exists()) {
            plugin.saveResource("company.yml", false);
        }
        companyConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    private void loadEventConfig() {
        File configFile = new File(plugin.getDataFolder(), "event.yml");
        if (!configFile.exists()) {
            plugin.saveResource("event.yml", false);
        }
        eventConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        loadCompanyConfig();
        loadEventConfig();
        companies.clear();
        events.clear();
        loadCompanies();
        loadEvents();
    }

    // ==================== DATA LOADING ====================

    private void loadCompanies() {
        ConfigurationSection section = companyConfig.getConfigurationSection("companies");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection cs = section.getConfigurationSection(id);
            if (cs == null) continue;

            String name = ChatColor.translateAlternateColorCodes('&', cs.getString("name", id));
            String description = cs.getString("description", "");
            String icon = cs.getString("icon", "CHEST");
            String industry = cs.getString("industry", "Unknown");
            String ceo = cs.getString("ceo", "Unknown");
            String headquarters = cs.getString("headquarters", "Unknown");
            int reputation = cs.getInt("reputation", 3);
            List<String> lore = cs.getStringList("lore").stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList());

            companies.put(id, new Company(id, name, description, icon, industry, ceo, headquarters, reputation, lore));
        }

        plugin.getLogger().info("Loaded " + companies.size() + " distribution companies.");
    }

    private void loadEvents() {
        ConfigurationSection section = eventConfig.getConfigurationSection("events");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection cs = section.getConfigurationSection(id);
            if (cs == null) continue;

            String name = ChatColor.translateAlternateColorCodes('&', cs.getString("name", id));
            List<String> lore = cs.getStringList("lore").stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList());
            double chance = cs.getDouble("chance", 10.0);
            int time = cs.getInt("time", 600);
            String companyId = cs.getString("company", "");
            List<String> demand = cs.getStringList("demand");
            double priceOffer = cs.getDouble("price_offer", 100.0);
            double bonus = cs.getDouble("bonus", 1.0);
            int bonusCondition = cs.getInt("bonus_condition", 50);
            List<String> commandOnAppear = cs.getStringList("command-on-appear");
            List<String> commandOnComplete = cs.getStringList("command-on-complete");

            events.put(id, new DistributionEvent(id, name, lore, chance, time, companyId,
                    demand, priceOffer, bonus, bonusCondition, commandOnAppear, commandOnComplete));
        }

        plugin.getLogger().info("Loaded " + events.size() + " distribution events.");
    }

    // ==================== PLAYER DATA PERSISTENCE ====================

    private void loadPlayerData() {
        if (!dataFile.exists()) return;

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        // Load active requests
        ConfigurationSection requestsSection = data.getConfigurationSection("active-requests");
        if (requestsSection != null) {
            for (String uuidStr : requestsSection.getKeys(false)) {
                UUID playerId = UUID.fromString(uuidStr);
                ConfigurationSection playerSection = requestsSection.getConfigurationSection(uuidStr);
                if (playerSection == null) continue;

                List<ActiveDistributionRequest> requests = new ArrayList<>();
                for (String requestId : playerSection.getKeys(false)) {
                    ConfigurationSection reqSection = playerSection.getConfigurationSection(requestId);
                    if (reqSection == null) continue;

                    String eventId = reqSection.getString("event-id", "");
                    long startTime = reqSection.getLong("start-time", 0);
                    long expireTime = reqSection.getLong("expire-time", 0);

                    Map<String, Boolean> delivered = new LinkedHashMap<>();
                    ConfigurationSection deliveredSection = reqSection.getConfigurationSection("delivered");
                    if (deliveredSection != null) {
                        for (String resId : deliveredSection.getKeys(false)) {
                            delivered.put(resId, deliveredSection.getBoolean(resId, false));
                        }
                    }

                    // Skip expired requests
                    if (System.currentTimeMillis() > expireTime) continue;

                    requests.add(new ActiveDistributionRequest(requestId, eventId, startTime, expireTime, delivered));
                }

                if (!requests.isEmpty()) {
                    activeRequests.put(playerId, requests);
                }
            }
        }

        // Load tax records
        ConfigurationSection taxSection = data.getConfigurationSection("tax-records");
        if (taxSection != null) {
            for (String uuidStr : taxSection.getKeys(false)) {
                distributionTaxRecords.put(UUID.fromString(uuidStr), taxSection.getDouble(uuidStr, 0.0));
            }
        }
    }

    public void saveAll() {
        FileConfiguration data = new YamlConfiguration();

        // Save active requests
        for (Map.Entry<UUID, List<ActiveDistributionRequest>> entry : activeRequests.entrySet()) {
            String basePath = "active-requests." + entry.getKey().toString();
            for (ActiveDistributionRequest req : entry.getValue()) {
                if (req.isExpired()) continue;
                String reqPath = basePath + "." + req.getRequestId();
                data.set(reqPath + ".event-id", req.getEventId());
                data.set(reqPath + ".start-time", req.getStartTime());
                data.set(reqPath + ".expire-time", req.getExpireTime());
                for (Map.Entry<String, Boolean> delivered : req.getDeliveredResources().entrySet()) {
                    data.set(reqPath + ".delivered." + delivered.getKey(), delivered.getValue());
                }
            }
        }

        // Save tax records
        for (Map.Entry<UUID, Double> entry : distributionTaxRecords.entrySet()) {
            data.set("tax-records." + entry.getKey().toString(), entry.getValue());
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save distribution data: " + e.getMessage());
        }
    }

    // ==================== EVENT SPAWNING ====================

    /**
     * Attempts to spawn a random distribution request for all online players.
     * Called by the scheduler.
     */
    public void tickDistributionEvents() {
        if (!isEnabled()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            List<ActiveDistributionRequest> playerRequests = activeRequests.getOrDefault(player.getUniqueId(), new ArrayList<>());

            // Remove expired requests
            playerRequests.removeIf(ActiveDistributionRequest::isExpired);
            activeRequests.put(player.getUniqueId(), playerRequests);

            // Check max active requests
            int maxRequests = plugin.getConfig().getInt("distribution.max-active-requests", 3);
            if (playerRequests.size() >= maxRequests) continue;

            // Roll for a new event
            DistributionEvent selectedEvent = rollEvent();
            if (selectedEvent == null) continue;

            spawnRequest(player, selectedEvent);
        }
    }

    /**
     * Selects a random event based on weighted chance.
     */
    private DistributionEvent rollEvent() {
        double totalWeight = events.values().stream().mapToDouble(DistributionEvent::getChance).sum();
        if (totalWeight <= 0) return null;

        double roll = new Random().nextDouble() * totalWeight;
        double cumulative = 0;

        for (DistributionEvent event : events.values()) {
            cumulative += event.getChance();
            if (roll <= cumulative) {
                return event;
            }
        }
        return null;
    }

    /**
     * Spawns a distribution request for a specific player.
     */
    public void spawnRequest(Player player, DistributionEvent event) {
        long now = System.currentTimeMillis();
        long expireTime = now + (event.getTime() * 1000L);
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        ActiveDistributionRequest request = new ActiveDistributionRequest(
                requestId, event.getId(), now, expireTime, event.getDemand());

        activeRequests.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(request);

        // Execute appear commands
        for (String cmd : event.getCommandOnAppear()) {
            String parsed = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }

        // Play notification sound
        try {
            player.playSound(player.getLocation(),
                    org.bukkit.Sound.valueOf(plugin.getConfig().getString(
                            "notifications.sound.distribution-request", "BLOCK_NOTE_BLOCK_BELL")),
                    1.0f, 1.0f);
        } catch (Exception ignored) {}
    }

    // ==================== DELIVERY HANDLING ====================

    /**
     * Attempts to deliver a resource for an active request.
     * Returns true if the resource was accepted.
     */
    public boolean deliverResource(Player player, String requestId, String resourceId) {
        List<ActiveDistributionRequest> requests = activeRequests.get(player.getUniqueId());
        if (requests == null) return false;

        for (ActiveDistributionRequest request : requests) {
            if (!request.getRequestId().equals(requestId)) continue;
            if (request.isExpired()) return false;
            if (request.isResourceDelivered(resourceId)) return false;

            // Check if player has the resource and take it from inventory
            ResourceManager resourceManager = plugin.getResourceManager();
            if (!resourceManager.takeResource(player, resourceId, 1)) return false;

            // Mark as delivered
            request.markDelivered(resourceId);

            // Check if request is now complete
            if (request.isCompleted()) {
                completeRequest(player, request);
            }

            return true;
        }
        return false;
    }

    /**
     * Completes a distribution request - pays the player and runs commands.
     */
    private void completeRequest(Player player, ActiveDistributionRequest request) {
        DistributionEvent event = events.get(request.getEventId());
        if (event == null) return;

        double basePayment = event.getPriceOffer();
        double finalPayment = basePayment;
        boolean bonusApplied = false;

        // Check bonus condition
        double timeRemainingPercent = request.getTimeRemainingPercent();
        if (timeRemainingPercent >= event.getBonusCondition()) {
            finalPayment = basePayment * event.getBonus();
            bonusApplied = true;
        }

        // Apply distribution tax
        double taxRate = plugin.getConfig().getDouble("distribution.tax_distribution_rate", 25.0);
        double taxAmount = finalPayment * (taxRate / 100.0);
        double afterTax = finalPayment - taxAmount;

        // Record tax
        distributionTaxRecords.merge(player.getUniqueId(), taxAmount, Double::sum);

        // Pay the player
        plugin.getEconomy().depositPlayer(player, afterTax);

        // Send completion message
        Company company = companies.get(event.getCompanyId());
        String companyName = company != null ? company.getName() : "Unknown Company";

        player.sendMessage("");
        player.sendMessage("\u00a76\u00a7l=== Distribution Request Complete ===");
        player.sendMessage("\u00a77Company: " + companyName);
        player.sendMessage("\u00a77Request: " + event.getName());
        player.sendMessage("");
        player.sendMessage("\u00a7eBase Payment: \u00a76$" + String.format("%.2f", basePayment));
        if (bonusApplied) {
            player.sendMessage("\u00a7aSpeed Bonus Applied! \u00a7e(x" + event.getBonus() + ") \u00a76$" + String.format("%.2f", finalPayment));
            player.sendMessage("\u00a77Time remaining: \u00a7a" + String.format("%.1f", timeRemainingPercent) + "%");
        }
        player.sendMessage("\u00a7cDistribution Tax (" + String.format("%.0f", taxRate) + "%): \u00a7c-$" + String.format("%.2f", taxAmount));
        player.sendMessage("\u00a7a\u00a7lYou received: \u00a76\u00a7l$" + String.format("%.2f", afterTax));
        player.sendMessage("\u00a76\u00a7l================================");
        player.sendMessage("");

        // Play completion sound
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } catch (Exception ignored) {}

        // Execute completion commands
        for (String cmd : event.getCommandOnComplete()) {
            String parsed = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }

        // Remove the completed request
        List<ActiveDistributionRequest> requests = activeRequests.get(player.getUniqueId());
        if (requests != null) {
            requests.remove(request);
        }
    }

    // ==================== GETTERS & UTILITY ====================

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("distribution.enabled", true);
    }

    public Map<String, Company> getCompanies() {
        return Collections.unmodifiableMap(companies);
    }

    public Company getCompany(String id) {
        return companies.get(id);
    }

    public Map<String, DistributionEvent> getEvents() {
        return Collections.unmodifiableMap(events);
    }

    public DistributionEvent getEvent(String id) {
        return events.get(id);
    }

    public List<ActiveDistributionRequest> getActiveRequests(UUID playerId) {
        List<ActiveDistributionRequest> requests = activeRequests.getOrDefault(playerId, new ArrayList<>());
        // Clean expired
        requests.removeIf(ActiveDistributionRequest::isExpired);
        return requests;
    }

    public int getActiveRequestCount(UUID playerId) {
        return getActiveRequests(playerId).size();
    }

    public double getTotalTaxPaid(UUID playerId) {
        return distributionTaxRecords.getOrDefault(playerId, 0.0);
    }

    public int getTotalCompanyCount() {
        return companies.size();
    }

    public int getTotalEventCount() {
        return events.size();
    }

    /**
     * Formats remaining time in a human-readable way.
     */
    public static String formatTime(long millis) {
        if (millis <= 0) return "Expired";
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
