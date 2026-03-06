package com.aithor.factorycore.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * Represents a player-created factory with a 3x3 chunk-style region
 * centered on the player's position at creation time.
 * This does NOT require WorldGuard — the region is stored as two corner locations.
 */
public class PlayerFactory {

    private final String id;
    private final UUID owner;
    private final FactoryType type;
    private final double price;
    private int level;
    private FactoryStatus status;
    private ProductionTask currentProduction;

    // Region corners (min and max)
    private final String worldName;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    // Center location (for fast travel)
    private final double centerX, centerY, centerZ;
    private final float centerYaw, centerPitch;

    // Upgrade timer
    private long upgradeStartTime = -1;
    private int upgradeDurationSeconds = 0;

    public PlayerFactory(String id, UUID owner, FactoryType type, double price,
                         String worldName,
                         int minX, int minY, int minZ,
                         int maxX, int maxY, int maxZ,
                         double centerX, double centerY, double centerZ,
                         float centerYaw, float centerPitch) {
        this.id = id;
        this.owner = owner;
        this.type = type;
        this.price = price;
        this.level = 1;
        this.status = FactoryStatus.STOPPED;
        this.worldName = worldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.centerYaw = centerYaw;
        this.centerPitch = centerPitch;
    }

    // ── Basic getters ────────────────────────────────────────────────────────

    public String getId() { return id; }
    public UUID getOwner() { return owner; }
    public FactoryType getType() { return type; }
    public double getPrice() { return price; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public FactoryStatus getStatus() { return status; }
    public void setStatus(FactoryStatus status) { this.status = status; }
    public ProductionTask getCurrentProduction() { return currentProduction; }
    public void setCurrentProduction(ProductionTask task) { this.currentProduction = task; }
    public String getWorldName() { return worldName; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    // ── Upgrade timer ────────────────────────────────────────────────────────

    public long getUpgradeStartTime() { return upgradeStartTime; }
    public void setUpgradeStartTime(long t) { this.upgradeStartTime = t; }
    public int getUpgradeDurationSeconds() { return upgradeDurationSeconds; }
    public void setUpgradeDurationSeconds(int s) { this.upgradeDurationSeconds = s; }

    public boolean isUpgrading() { return upgradeStartTime > 0; }

    public int getUpgradeRemainingSeconds() {
        if (!isUpgrading()) return 0;
        long elapsed = (System.currentTimeMillis() - upgradeStartTime) / 1000;
        return (int) Math.max(0, upgradeDurationSeconds - elapsed);
    }

    public boolean isUpgradeComplete() {
        return isUpgrading() && getUpgradeRemainingSeconds() == 0;
    }

    // ── Location helpers ─────────────────────────────────────────────────────

    /**
     * Returns the center location (where the player stood at creation time).
     */
    public Location getCenterLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, centerX, centerY, centerZ, centerYaw, centerPitch);
    }

    /**
     * Returns true if the given location is within this factory's region.
     */
    public boolean containsLocation(Location loc) {
        if (loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(worldName)) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public double getCenterX() { return centerX; }
    public double getCenterY() { return centerY; }
    public double getCenterZ() { return centerZ; }
    public float getCenterYaw() { return centerYaw; }
    public float getCenterPitch() { return centerPitch; }
}
