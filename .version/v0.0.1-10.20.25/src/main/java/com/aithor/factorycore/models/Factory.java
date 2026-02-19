package com.aithor.factorycore.models;
import org.bukkit.Location;
import java.util.*;

// ==================== Factory.java ====================
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
    
    public Factory(String id, String regionName, FactoryType type, UUID owner, double price, int level) {
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
    
    // Getters and Setters
    public String getId() { return id; }
    public String getRegionName() { return regionName; }
    public FactoryType getType() { return type; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }
    public double getPrice() { return price; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public FactoryStatus getStatus() { return status; }
    public void setStatus(FactoryStatus status) { this.status = status; }
    public ProductionTask getCurrentProduction() { return currentProduction; }
    public void setCurrentProduction(ProductionTask task) { this.currentProduction = task; }
    public Location getFastTravelLocation() { return fastTravelLocation; }
    public void setFastTravelLocation(Location location) { this.fastTravelLocation = location; }

    // ==================== OUTPUT STORAGE METHODS ====================
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
        if (current <= amount) {
            outputStorage.remove(resourceId);
        } else {
            outputStorage.put(resourceId, current - amount);
        }
    }

    public void clearOutputStorage() {
        outputStorage.clear();
    }

    public int getOutputStorageSize() {
        return outputStorage.size();
    }

    public int getMaxOutputStorageSlots() {
        return level * 9; // 9 slots per level
    }
}