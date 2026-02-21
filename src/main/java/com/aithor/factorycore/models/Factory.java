package com.aithor.factorycore.models;

import org.bukkit.Location;
import java.util.*;

public class Factory {

    private String id;
    private String regionName;
    private FactoryType type;
    private UUID owner;
    private double price;
    private int level;
    private FactoryStatus status;
    private ProductionTask currentProduction;
    private Location fastTravelLocation;
    private Map<String, Integer> outputStorage;

    // ── Upgrade timer ─────────────────────────────────────────────────────────
    private long upgradeStartTime = -1; // epoch ms, -1 = not upgrading
    private int upgradeDurationSeconds = 0;

    public Factory(String id, String regionName, FactoryType type,
            UUID owner, double price, int level) {
        this.id = id;
        this.regionName = regionName;
        this.type = type;
        this.owner = owner;
        this.price = price;
        this.level = level;
        this.status = FactoryStatus.STOPPED;
        this.currentProduction = null;
        this.outputStorage = new HashMap<>();
    }

    // ── Basic getters / setters ───────────────────────────────────────────────
    public String getId() {
        return id;
    }

    public String getRegionName() {
        return regionName;
    }

    public FactoryType getType() {
        return type;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public double getPrice() {
        return price;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public FactoryStatus getStatus() {
        return status;
    }

    public void setStatus(FactoryStatus status) {
        this.status = status;
    }

    public ProductionTask getCurrentProduction() {
        return currentProduction;
    }

    public void setCurrentProduction(ProductionTask task) {
        this.currentProduction = task;
    }

    public Location getFastTravelLocation() {
        return fastTravelLocation;
    }

    public void setFastTravelLocation(Location location) {
        this.fastTravelLocation = location;
    }

    // ── Upgrade timer getters / setters ───────────────────────────────────────
    public long getUpgradeStartTime() {
        return upgradeStartTime;
    }

    public void setUpgradeStartTime(long t) {
        this.upgradeStartTime = t;
    }

    public int getUpgradeDurationSeconds() {
        return upgradeDurationSeconds;
    }

    public void setUpgradeDurationSeconds(int s) {
        this.upgradeDurationSeconds = s;
    }

    /** True if an upgrade is currently in progress. */
    public boolean isUpgrading() {
        return upgradeStartTime > 0;
    }

    /** Remaining upgrade time in seconds, 0 when done. */
    public int getUpgradeRemainingSeconds() {
        if (!isUpgrading())
            return 0;
        long elapsed = (System.currentTimeMillis() - upgradeStartTime) / 1000;
        return (int) Math.max(0, upgradeDurationSeconds - elapsed);
    }

    /** True when the upgrade timer has expired. */
    public boolean isUpgradeComplete() {
        return isUpgrading() && getUpgradeRemainingSeconds() == 0;
    }

    // ── Output storage ────────────────────────────────────────────────────────
    public Map<String, Integer> getOutputStorage() {
        return new HashMap<>(outputStorage);
    }

    public int getOutputStorageAmount(String resourceId) {
        return outputStorage.getOrDefault(resourceId, 0);
    }

    public void addToOutputStorage(String resourceId, int amount) {
        outputStorage.put(resourceId, outputStorage.getOrDefault(resourceId, 0) + amount);
    }

    public void removeFromOutputStorage(String resourceId, int amount) {
        int current = outputStorage.getOrDefault(resourceId, 0);
        if (current <= amount)
            outputStorage.remove(resourceId);
        else
            outputStorage.put(resourceId, current - amount);
    }

    public void clearOutputStorage() {
        outputStorage.clear();
    }

    public int getOutputStorageSize() {
        return outputStorage.size();
    }
}